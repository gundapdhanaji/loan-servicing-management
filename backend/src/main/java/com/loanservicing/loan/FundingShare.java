package com.loanservicing.loan;

import java.math.BigDecimal;

/**
 * One lender's slice of a loan.
 * share = fundedAmount / originalBalance (e.g. 0.60 for 60%).
 * The lender earns lenderRate; the servicer keeps the difference to noteRate as its fee.
 */
public record FundingShare(
        Long lenderId,
        BigDecimal fundedAmount,
        BigDecimal share,
        BigDecimal lenderRate,
        BigDecimal noteRate) {
}
