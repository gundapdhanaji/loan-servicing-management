package com.loanservicing.loan.internal;

import com.loanservicing.loan.LoanJobs;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the loan module's nightly jobs. Times are in application.properties (app.jobs.*). */
@Component
@RequiredArgsConstructor
public class LoanScheduler {

    private final LoanJobs loanJobs;

    @Scheduled(cron = "${app.jobs.late-charges-cron}")
    public void lateCharges() {
        loanJobs.assessLateCharges();
    }

    @Scheduled(cron = "${app.jobs.default-check-cron}")
    public void defaults() {
        loanJobs.flagDefaults();
    }
}
