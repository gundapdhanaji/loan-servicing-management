package com.loanservicing.payment;

import java.math.BigDecimal;

/** Published when an ACH payment bounces (NSF). Future Kafka topic: "payment.returned". */
public record PaymentReturnedEvent(
        Long paymentId,
        Long loanId,
        Long borrowerId,
        BigDecimal amount,
        String returnCode,
        String reason,
        BigDecimal nsfFee) {
}
