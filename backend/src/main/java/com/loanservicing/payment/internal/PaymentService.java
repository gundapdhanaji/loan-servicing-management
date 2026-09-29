package com.loanservicing.payment.internal;

import com.loanservicing.auth.CurrentUser;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.common.BankDetails;
import com.loanservicing.common.BusinessException;
import com.loanservicing.common.Money;
import com.loanservicing.common.NotFoundException;
import com.loanservicing.loan.LoanApi;
import com.loanservicing.loan.LoanDto;
import com.loanservicing.loan.PaymentApplication;
import com.loanservicing.payment.PaymentPostedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * The borrower pays. This is the main flow of the whole application:
 *
 *   1. check the borrower owns the loan and the bank account
 *   2. loan module applies the money (waterfall: charges, interest, principal, escrow)
 *   3. ACH debit through the gateway (fake locally)
 *   4. save the payment with its breakdown
 *   5. record what each lender is owed
 *   6. publish PaymentPostedEvent (notification module sends a receipt)
 *
 * Everything is in ONE transaction. If the bank rejects the debit in step 3, the exception
 * rolls back step 2 too - the loan is never left half-updated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DistributionService distributionService;
    private final LoanApi loanApi;
    private final BorrowerApi borrowerApi;
    private final AchGateway achGateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public PaymentDto makePayment(MakePaymentRequest request, String idempotencyKey) {
        // Same request sent twice? Return the first result instead of charging again.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate payment request {} - returning the original payment", idempotencyKey);
                return PaymentDto.from(existing.get());
            }
        }

        // 1. ownership checks
        BorrowerDto borrower = borrowerApi.findByUserId(CurrentUser.get().userId())
                .orElseThrow(() -> new NotFoundException("No borrower profile for this user"));
        LoanDto loan = loanApi.getForCurrentUser(request.loanId());
        BankDetails bank = borrowerApi.getBankDetails(borrower.id(), request.bankAccountId());

        // 2. apply to the loan (validates the amount)
        PaymentApplication application = loanApi.applyPayment(loan.id(), request.amount());

        // 3. move the money
        AchGateway.AchResult ach = achGateway.debit(bank, request.amount(), "Loan " + loan.loanNumber());
        if (!ach.accepted()) {
            throw new BusinessException("Bank rejected the payment: " + ach.rejectReason());
        }

        // 4. save
        Payment payment = new Payment();
        payment.setLoanId(loan.id());
        payment.setBorrowerId(borrower.id());
        payment.setBankAccountId(request.bankAccountId());
        payment.setAmount(Money.of(request.amount()));
        payment.setIdempotencyKey(idempotencyKey);
        payment.setAchReference(ach.traceNumber());
        payment.setPaymentDate(LocalDate.now(clock));
        payment.recordApplication(application);
        paymentRepository.save(payment);

        // 5. lenders' shares
        distributionService.distribute(payment);

        // 6. event (listeners run after the transaction commits)
        events.publishEvent(new PaymentPostedEvent(payment.getId(), loan.id(), borrower.id(),
                payment.getAmount(), application.dueDateAfter(), application.loanPaidOff()));

        return PaymentDto.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> findByLoan(Long loanId) {
        loanApi.getForCurrentUser(loanId); // access check
        return paymentRepository.findByLoanIdOrderByIdDesc(loanId).stream().map(PaymentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto get(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> NotFoundException.of("Payment", paymentId));
        loanApi.getForCurrentUser(payment.getLoanId()); // access check
        return PaymentDto.from(payment);
    }
}
