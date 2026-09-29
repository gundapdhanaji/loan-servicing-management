package com.loanservicing.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * How one payment was applied to a loan. The payment module stores this with the payment
 * so it can show the breakdown, split money to lenders, and reverse it if the payment bounces.
 */
public record PaymentApplication(
        Long loanId,
        List<ChargePayment> chargePayments,
        BigDecimal interest,
        BigDecimal principal,
        BigDecimal extraPrincipal,
        BigDecimal reserve,
        BigDecimal impound,
        LocalDate dueDateBefore,
        LocalDate dueDateAfter,
        boolean loanPaidOff) {

    public record ChargePayment(Long chargeId, BigDecimal amount) {
    }

    public BigDecimal chargesTotal() {
        return chargePayments.stream().map(ChargePayment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPrincipal() {
        return principal.add(extraPrincipal);
    }
}
