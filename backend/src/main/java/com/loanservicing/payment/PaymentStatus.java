package com.loanservicing.payment;

public enum PaymentStatus {
    POSTED,    // money received and applied to the loan
    RETURNED   // bounced (NSF) - the application was reversed
}
