package com.loanservicing.loan;

/**
 * Published when a loan becomes 31+ days past due.
 * Today: a Spring event inside one app. After the split: a Kafka message on topic "loan.defaulted".
 */
public record LoanDefaultedEvent(Long loanId, String loanNumber, Long borrowerId, long daysPastDue) {
}
