package com.loanservicing.payment.internal;

import com.loanservicing.common.BankDetails;

import java.math.BigDecimal;
import java.util.List;

/**
 * The "port" to the bank. The business code only knows this interface.
 *
 *   FakeAchGateway  -> used now, on your laptop (no real money)
 *   RealAchGateway  -> would call a real ACH provider in production (not built)
 */
public interface AchGateway {

    /** Pull money from a borrower's account. */
    AchResult debit(BankDetails from, BigDecimal amount, String description);

    /** Push money to a lender's account. */
    AchResult credit(BankDetails to, BigDecimal amount, String description);

    /** Payments that came back from the bank since the last call (bounced / NSF). */
    List<AchReturn> fetchReturns();

    record AchResult(boolean accepted, String traceNumber, String rejectReason) {

        public static AchResult ok(String traceNumber) {
            return new AchResult(true, traceNumber, null);
        }

        public static AchResult fail(String reason) {
            return new AchResult(false, null, reason);
        }
    }

    /** R01 = insufficient funds, R02 = account closed, R03 = no account... (standard ACH return codes) */
    record AchReturn(String traceNumber, String returnCode, String reason) {
    }
}
