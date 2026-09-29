package com.loanservicing.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers. Always use BigDecimal for money, never double:
 * 0.1 + 0.2 in double is 0.30000000000000004.
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal TWELVE = new BigDecimal("12");

    private Money() {
    }

    /** Rounds to 2 decimal places (paise / cents). */
    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
