package com.loanservicing.payment.internal;

import com.loanservicing.payment.PaymentJobs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * The payment module's nightly jobs.
 * Each returned payment is processed in its own transaction (NsfService.processReturn),
 * so one bad record does not undo all the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJobsService implements PaymentJobs {

    private final AchGateway achGateway;
    private final NsfService nsfService;
    private final DistributionService distributionService;

    @Override
    public int processAchReturns() {
        int processed = 0;
        for (AchGateway.AchReturn r : achGateway.fetchReturns()) {
            try {
                if (nsfService.processReturn(r.traceNumber(), r.returnCode(), r.reason())) {
                    processed++;
                }
            } catch (RuntimeException e) {
                log.error("Could not process ACH return {}", r.traceNumber(), e);
            }
        }
        log.info("ACH returns job: {} payments returned", processed);
        return processed;
    }

    @Override
    public int runDisbursements() {
        return distributionService.runDisbursements();
    }

    @Scheduled(cron = "${app.jobs.ach-returns-cron}")
    public void nightlyAchReturns() {
        processAchReturns();
    }

    @Scheduled(cron = "${app.jobs.disbursements-cron}")
    public void nightlyDisbursements() {
        runDisbursements();
    }
}
