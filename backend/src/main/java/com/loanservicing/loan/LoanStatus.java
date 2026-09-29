package com.loanservicing.loan;

public enum LoanStatus {
    ACTIVE,    // being paid normally (may be a few days late)
    DEFAULT,   // 31+ days past due - shows on the Default Management screen
    PAID_OFF   // fully repaid, closed
}
