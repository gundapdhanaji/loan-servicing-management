package com.loanservicing.payment;

/** Nightly jobs owned by the payment module (also callable from /api/v1/dev/jobs/... locally). */
public interface PaymentJobs {

    /** Asks the bank (ACH) which payments bounced and processes them as NSF. Returns how many. */
    int processAchReturns();

    /** Sends each lender the money collected for them. Returns how many lenders were paid. */
    int runDisbursements();
}
