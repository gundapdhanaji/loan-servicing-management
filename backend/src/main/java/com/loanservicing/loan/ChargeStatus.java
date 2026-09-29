package com.loanservicing.loan;

public enum ChargeStatus {
    OPEN,    // borrower still owes (part of) it
    PAID,
    WAIVED   // staff decided not to collect it
}
