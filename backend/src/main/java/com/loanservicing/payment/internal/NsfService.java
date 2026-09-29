package com.loanservicing.payment.internal;

import com.loanservicing.common.Money;
import com.loanservicing.loan.ChargeType;
import com.loanservicing.loan.LoanApi;
import com.loanservicing.payment.PaymentReturnedEvent;
import com.loanservicing.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Handles a bounced (NSF) payment. All steps run in ONE transaction:
 * either everything below happens, or nothing does.
 */
@Slf4j
@Service
public class NsfService {

    private final PaymentRepository paymentRepository;
    private final NsfCaseRepository nsfCaseRepository;
    private final DistributionService distributionService;
    private final LoanApi loanApi;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final BigDecimal nsfFee;

    public NsfService(PaymentRepository paymentRepository, NsfCaseRepository nsfCaseRepository,
                      DistributionService distributionService, LoanApi loanApi,
                      ApplicationEventPublisher events, Clock clock,
                      @Value("${app.fees.nsf-fee}") BigDecimal nsfFee) {
        this.paymentRepository = paymentRepository;
        this.nsfCaseRepository = nsfCaseRepository;
        this.distributionService = distributionService;
        this.loanApi = loanApi;
        this.events = events;
        this.clock = clock;
        this.nsfFee = Money.of(nsfFee);
    }

    @Transactional
    public boolean processReturn(String achReference, String returnCode, String reason) {
        Payment payment = paymentRepository.findByAchReference(achReference).orElse(null);
        if (payment == null || payment.getStatus() == PaymentStatus.RETURNED) {
            log.warn("ACH return {} ignored: unknown payment or already returned", achReference);
            return false;
        }
        LocalDate today = LocalDate.now(clock);

        // 1. mark the payment as returned (never delete it - the history must stay complete)
        payment.setStatus(PaymentStatus.RETURNED);
        payment.setReturnedDate(today);
        payment.setReturnCode(returnCode);
        payment.setReturnReason(reason);

        // 2. undo it on the loan (balance, due date, charges go back to how they were)
        loanApi.reversePayment(payment.toApplication());

        // 3. cancel / claw back the lenders' share
        distributionService.reverse(payment);

        // 4. charge the NSF fee
        loanApi.addCharge(payment.getLoanId(), ChargeType.NSF_FEE, nsfFee,
                "NSF fee - payment #" + payment.getId() + " returned (" + returnCode + ")");

        // 5. open an NSF case for customer service
        NsfCase nsfCase = new NsfCase();
        nsfCase.setPaymentId(payment.getId());
        nsfCase.setLoanId(payment.getLoanId());
        nsfCase.setBorrowerId(payment.getBorrowerId());
        nsfCase.setAmount(payment.getAmount());
        nsfCase.setReturnCode(returnCode);
        nsfCase.setReason(reason);
        nsfCase.setFeeCharged(nsfFee);
        nsfCase.setCaseDate(today);
        nsfCaseRepository.save(nsfCase);

        // 6. tell whoever is interested (notification module sends the borrower a notice)
        events.publishEvent(new PaymentReturnedEvent(payment.getId(), payment.getLoanId(), payment.getBorrowerId(),
                payment.getAmount(), returnCode, reason, nsfFee));

        log.info("Payment #{} returned ({}): reversed, NSF fee {} charged", payment.getId(), returnCode, nsfFee);
        return true;
    }
}
