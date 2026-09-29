package com.loanservicing.payment.internal;

import com.loanservicing.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the React "Payment Activity" / "Loan History" table, with its breakdown. */
public record PaymentDto(
        Long id,
        Long loanId,
        BigDecimal amount,
        PaymentStatus status,
        LocalDate paymentDate,
        String achReference,
        BigDecimal chargesPaid,
        BigDecimal interestPaid,
        BigDecimal principalPaid,
        BigDecimal extraPrincipalPaid,
        BigDecimal reservePaid,
        BigDecimal impoundPaid,
        LocalDate dueDateBefore,
        LocalDate dueDateAfter,
        LocalDate returnedDate,
        String returnCode,
        String returnReason) {

    static PaymentDto from(Payment p) {
        return new PaymentDto(p.getId(), p.getLoanId(), p.getAmount(), p.getStatus(), p.getPaymentDate(),
                p.getAchReference(), p.getChargesPaid(), p.getInterestPaid(), p.getPrincipalPaid(),
                p.getExtraPrincipalPaid(), p.getReservePaid(), p.getImpoundPaid(), p.getDueDateBefore(),
                p.getDueDateAfter(), p.getReturnedDate(), p.getReturnCode(), p.getReturnReason());
    }
}
