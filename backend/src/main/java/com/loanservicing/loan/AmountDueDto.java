package com.loanservicing.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the borrower must pay now (the "Make a Payment" screen), and the payoff amount.
 * totalDue is the minimum payment; anything above it (up to payoff) reduces principal.
 */
public record AmountDueDto(
        Long loanId,
        String loanNumber,
        LocalDate dueDate,
        BigDecimal unpaidCharges,
        BigDecimal interest,
        BigDecimal principal,
        BigDecimal reserve,
        BigDecimal impound,
        BigDecimal totalDue,
        BigDecimal payoffAmount) {
}
