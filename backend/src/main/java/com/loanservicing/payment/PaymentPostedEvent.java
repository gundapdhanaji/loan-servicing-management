package com.loanservicing.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Published after a payment is applied. The notification module listens and sends a receipt.
 * After the microservice split this becomes a Kafka message on topic "payment.posted".
 */
public record PaymentPostedEvent(
        Long paymentId,
        Long loanId,
        Long borrowerId,
        BigDecimal amount,
        LocalDate nextDueDate,
        boolean loanPaidOff) {
}
