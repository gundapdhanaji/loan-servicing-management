package com.loanservicing.payment.internal;

import com.loanservicing.common.Money;
import com.loanservicing.loan.FundingShare;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits the principal and interest of one payment among the loan's lenders. Pure Java, easy to test.
 *
 * Example: loan note rate 12%, Lender A funded 60% at 10%, Lender B funded 40% at 10%.
 * Payment has principal 1000, interest 500.
 *   A principal = 1000 x 0.60 = 600
 *   A interest  = 500 x 0.60 x (10/12) = 250
 *   B principal = 400, B interest = 500 x 0.40 x (10/12) = 166.67
 *   Servicer keeps 500 - 250 - 166.67 = 83.33  (the 2% spread = servicing fee)
 *
 * Escrow (reserve/impound) and fees are NOT paid to lenders; the servicer holds them.
 */
public final class DistributionCalculator {

    private DistributionCalculator() {
    }

    public record LenderSplit(Long lenderId, BigDecimal principal, BigDecimal interest) {

        public BigDecimal total() {
            return principal.add(interest);
        }
    }

    public static List<LenderSplit> split(BigDecimal principal, BigDecimal interest, List<FundingShare> shares) {
        List<LenderSplit> result = new ArrayList<>();
        BigDecimal principalLeft = Money.of(principal);

        for (int i = 0; i < shares.size(); i++) {
            FundingShare s = shares.get(i);
            boolean last = i == shares.size() - 1;

            // The last lender gets whatever is left, so rounding never loses or creates a paisa/cent
            BigDecimal p = last ? principalLeft : Money.of(principal.multiply(s.share(), MathContext.DECIMAL64));
            principalLeft = principalLeft.subtract(p);

            BigDecimal interestPart = BigDecimal.ZERO;
            if (s.noteRate().signum() > 0) {
                BigDecimal rateRatio = s.lenderRate().divide(s.noteRate(), MathContext.DECIMAL64);
                interestPart = interest.multiply(s.share(), MathContext.DECIMAL64).multiply(rateRatio, MathContext.DECIMAL64);
            }
            result.add(new LenderSplit(s.lenderId(), p, Money.of(interestPart)));
        }
        return result;
    }
}
