package com.loanservicing.payment.internal;

import com.loanservicing.common.BankDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pretends to be the ACH network. NO REAL MONEY MOVES.
 *
 * Testing tricks:
 *   - routing number that is not 9 digits  -> rejected immediately
 *   - account number ending in "0000"      -> accepted now, but BOUNCES (R01 NSF)
 *                                             when the ACH returns job runs
 *   - anything else                         -> succeeds
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ach.mode", havingValue = "fake", matchIfMissing = true)
public class FakeAchGateway implements AchGateway {

    private final ConcurrentLinkedQueue<AchReturn> pendingReturns = new ConcurrentLinkedQueue<>();

    @Override
    public AchResult debit(BankDetails from, BigDecimal amount, String description) {
        if (from.routingNumber() == null || !from.routingNumber().matches("\\d{9}")) {
            log.info("FAKE ACH debit REJECTED - invalid routing number {}", from.routingNumber());
            return AchResult.fail("Invalid routing number");
        }
        String trace = newTraceNumber();
        log.info("FAKE ACH debit {} from {} ({}) trace={}", amount, from.maskedAccountNumber(), description, trace);

        if (from.accountNumber().endsWith("0000")) {
            log.info("  -> account ends in 0000: this payment will bounce (R01) when ACH returns are processed");
            pendingReturns.add(new AchReturn(trace, "R01", "Insufficient funds"));
        }
        return AchResult.ok(trace);
    }

    @Override
    public AchResult credit(BankDetails to, BigDecimal amount, String description) {
        String trace = newTraceNumber();
        log.info("FAKE ACH credit {} to {} ({}) trace={}", amount, to.maskedAccountNumber(), description, trace);
        return AchResult.ok(trace);
    }

    @Override
    public List<AchReturn> fetchReturns() {
        List<AchReturn> returns = new ArrayList<>();
        AchReturn r;
        while ((r = pendingReturns.poll()) != null) {
            returns.add(r);
        }
        return returns;
    }

    private String newTraceNumber() {
        return "FAKE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
