package com.loanservicing.payment.internal;

import com.loanservicing.common.BankDetails;
import com.loanservicing.common.Money;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.loan.LoanApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Records what each lender is owed from a payment, and pays lenders in the nightly run. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionService {

    private final LenderDisbursementRepository disbursementRepository;
    private final LoanApi loanApi;
    private final LenderApi lenderApi;
    private final AchGateway achGateway;
    private final Clock clock;

    @Transactional
    public void distribute(Payment payment) {
        BigDecimal principal = payment.getPrincipalPaid().add(payment.getExtraPrincipalPaid());
        var splits = DistributionCalculator.split(principal, payment.getInterestPaid(),
                loanApi.getFundingShares(payment.getLoanId()));
        for (var split : splits) {
            LenderDisbursement d = new LenderDisbursement();
            d.setPaymentId(payment.getId());
            d.setLoanId(payment.getLoanId());
            d.setLenderId(split.lenderId());
            d.setPrincipalAmount(split.principal());
            d.setInterestAmount(split.interest());
            d.setTotalAmount(split.total());
            d.setNote("From payment #" + payment.getId());
            disbursementRepository.save(d);
        }
    }

    /** The borrower's payment bounced: cancel what we haven't paid out, claw back what we have. */
    @Transactional
    public void reverse(Payment payment) {
        for (LenderDisbursement d : disbursementRepository.findByPaymentId(payment.getId())) {
            if (d.getStatus() == DisbursementStatus.PENDING) {
                d.setStatus(DisbursementStatus.CANCELLED);
                d.setNote(d.getNote() + " - cancelled, payment returned");
            } else if (d.getStatus() == DisbursementStatus.PAID) {
                LenderDisbursement clawback = new LenderDisbursement();
                clawback.setPaymentId(d.getPaymentId());
                clawback.setLoanId(d.getLoanId());
                clawback.setLenderId(d.getLenderId());
                clawback.setPrincipalAmount(d.getPrincipalAmount().negate());
                clawback.setInterestAmount(d.getInterestAmount().negate());
                clawback.setTotalAmount(d.getTotalAmount().negate());
                clawback.setNote("Clawback: payment #" + payment.getId() + " was returned");
                disbursementRepository.save(clawback);
            }
        }
    }

    /**
     * Pays every lender the NET of their pending rows (positives minus clawbacks).
     * If a lender's net is zero or negative, their rows wait for the next run.
     */
    @Transactional
    public int runDisbursements() {
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<LenderDisbursement>> byLender = disbursementRepository
                .findByStatus(DisbursementStatus.PENDING).stream()
                .collect(Collectors.groupingBy(LenderDisbursement::getLenderId));

        int lendersPaid = 0;
        for (var entry : byLender.entrySet()) {
            BigDecimal net = entry.getValue().stream().map(LenderDisbursement::getTotalAmount)
                    .reduce(Money.ZERO, BigDecimal::add);
            if (net.signum() <= 0) {
                continue;
            }
            BankDetails bank = lenderApi.getBankDetails(entry.getKey());
            AchGateway.AchResult result = achGateway.credit(bank, net, "Lender disbursement " + today);
            if (!result.accepted()) {
                log.warn("Disbursement to lender {} rejected: {}", entry.getKey(), result.rejectReason());
                continue;
            }
            for (LenderDisbursement d : entry.getValue()) {
                d.setStatus(DisbursementStatus.PAID);
                d.setDisbursedDate(today);
                d.setAchReference(result.traceNumber());
            }
            lendersPaid++;
        }
        log.info("Disbursement run: {} lenders paid", lendersPaid);
        return lendersPaid;
    }
}
