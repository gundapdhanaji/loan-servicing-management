package com.loanservicing.auth;

/** Who can do what. Stored on the user; put into the JWT at login. */
public enum Role {
    ADMIN,     // onboards lenders, borrowers and loans; sees everything
    CSR,       // customer service rep; can view everything, cannot onboard
    LENDER,    // sees only loans they funded and their own disbursements
    BORROWER   // sees only their own loans; makes payments
}
