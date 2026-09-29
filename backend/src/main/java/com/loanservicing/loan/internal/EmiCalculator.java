package com.loanservicing.loan.internal;

import com.loanservicing.common.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Loan maths.
 *
 *   monthly rate r = annualRate% / 100 / 12
 *   EMI = P x r x (1+r)^n / ((1+r)^n - 1)
 *   interest for one month = principalBalance x r
 */
public final class EmiCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;

    private EmiCalculator() {
    }

    public static BigDecimal monthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent.divide(Money.HUNDRED, MC).divide(Money.TWELVE, MC);
    }

    public static BigDecimal emi(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive");
        }
        BigDecimal r = monthlyRate(annualRatePercent);
        if (r.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusRPowN = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal numerator = principal.multiply(r, MC).multiply(onePlusRPowN, MC);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal monthlyInterest(BigDecimal principalBalance, BigDecimal annualRatePercent) {
        return Money.of(principalBalance.multiply(monthlyRate(annualRatePercent), MC));
    }
}
