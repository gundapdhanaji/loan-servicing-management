package com.loanservicing.loan;

/**
 * Nightly jobs owned by the loan module.
 * Called by the scheduler at night, and by /api/v1/dev/jobs/... in local development.
 */
public interface LoanJobs {

    /** Adds a late fee to every loan whose due date + grace days has passed. Returns how many. */
    int assessLateCharges();

    /** Marks loans 31+ days past due as DEFAULT. Returns how many. */
    int flagDefaults();
}
