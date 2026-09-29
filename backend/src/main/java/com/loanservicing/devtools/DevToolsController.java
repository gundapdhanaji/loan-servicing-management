package com.loanservicing.devtools;

import com.loanservicing.common.AppClock;
import com.loanservicing.loan.LoanJobs;
import com.loanservicing.payment.PaymentJobs;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LOCAL ONLY (@Profile("local")) - this controller does not exist in any other environment.
 *
 * Lets you test time-based logic without waiting:
 *   POST /api/v1/dev/clock/advance?days=40   -> the app now thinks it is 40 days later
 *   POST /api/v1/dev/jobs/run-all            -> run every nightly job right now
 */
@Profile("local")
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevToolsController {

    private final AppClock clock;
    private final LoanJobs loanJobs;
    private final PaymentJobs paymentJobs;

    @GetMapping("/clock")
    public Map<String, Object> clock() {
        return Map.of("today", LocalDate.now(clock), "offsetDays", clock.offsetDays());
    }

    @PostMapping("/clock/advance")
    public Map<String, Object> advance(@RequestParam long days) {
        clock.advanceDays(days);
        return clock();
    }

    @PostMapping("/clock/reset")
    public Map<String, Object> reset() {
        clock.reset();
        return clock();
    }

    @PostMapping("/jobs/late-charges")
    public Map<String, Integer> lateCharges() {
        return Map.of("lateFeesAdded", loanJobs.assessLateCharges());
    }

    @PostMapping("/jobs/defaults")
    public Map<String, Integer> defaults() {
        return Map.of("loansDefaulted", loanJobs.flagDefaults());
    }

    @PostMapping("/jobs/ach-returns")
    public Map<String, Integer> achReturns() {
        return Map.of("paymentsReturned", paymentJobs.processAchReturns());
    }

    @PostMapping("/jobs/disbursements")
    public Map<String, Integer> disbursements() {
        return Map.of("lendersPaid", paymentJobs.runDisbursements());
    }

    /** Same order as the real nightly schedule. */
    @PostMapping("/jobs/run-all")
    public Map<String, Integer> runAll() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("paymentsReturned", paymentJobs.processAchReturns());
        result.put("lateFeesAdded", loanJobs.assessLateCharges());
        result.put("loansDefaulted", loanJobs.flagDefaults());
        result.put("lendersPaid", paymentJobs.runDisbursements());
        return result;
    }
}
