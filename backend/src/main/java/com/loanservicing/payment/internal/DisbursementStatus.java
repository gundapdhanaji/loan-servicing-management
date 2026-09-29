package com.loanservicing.payment.internal;

public enum DisbursementStatus {
    PENDING,    // collected, waiting for the nightly payout
    PAID,       // sent to the lender's bank account
    CANCELLED   // the borrower's payment bounced before we paid the lender
}
