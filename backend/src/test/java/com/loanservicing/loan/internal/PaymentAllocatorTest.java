package com.loanservicing.loan.internal;

import com.loanservicing.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests - no Spring, no database, run in milliseconds. */
class PaymentAllocatorTest {

    private static final BigDecimal BALANCE = new BigDecimal("300000.00");
    private static final BigDecimal RATE = new BigDecimal("12");
    private static final BigDecimal EMI = new BigDecimal("3085.84");
    private static final BigDecimal RESERVE = new BigDecimal("150.00");
    private static final BigDecimal IMPOUND = new BigDecimal("250.00");

    @Test
    void emiForThreeLakhAtTwelvePercentForThirtyYears() {
        assertThat(EmiCalculator.emi(BALANCE, RATE, 360)).isEqualByComparingTo("3085.84");
    }

    @Test
    void emiWithZeroInterestIsSimpleDivision() {
        assertThat(EmiCalculator.emi(new BigDecimal("12000"), BigDecimal.ZERO, 12)).isEqualByComparingTo("1000.00");
    }

    @Test
    void installmentIsInterestPlusPrincipalPlusEscrow() {
        var due = PaymentAllocator.installment(BALANCE, RATE, EMI, RESERVE, IMPOUND, List.of());

        assertThat(due.interest()).isEqualByComparingTo("3000.00");   // 300000 x 1%
        assertThat(due.principal()).isEqualByComparingTo("85.84");    // 3085.84 - 3000
        assertThat(due.total()).isEqualByComparingTo("3485.84");      // + 150 + 250
    }

    @Test
    void chargesArePaidFirstAndExtraGoesToPrincipal() {
        var charges = List.of(new PaymentAllocator.OpenCharge(7L, new BigDecimal("50.00")));

        var a = PaymentAllocator.allocate(new BigDecimal("4535.84"), BALANCE, RATE, EMI, RESERVE, IMPOUND, charges);

        assertThat(a.chargePayments()).hasSize(1);
        assertThat(a.chargePayments().get(0).chargeId()).isEqualTo(7L);
        assertThat(a.chargePayments().get(0).amount()).isEqualByComparingTo("50.00");
        assertThat(a.interest()).isEqualByComparingTo("3000.00");
        assertThat(a.principal()).isEqualByComparingTo("85.84");
        assertThat(a.reserve()).isEqualByComparingTo("150.00");
        assertThat(a.impound()).isEqualByComparingTo("250.00");
        assertThat(a.extraPrincipal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void paymentBelowMinimumIsRejected() {
        assertThatThrownBy(() -> PaymentAllocator.allocate(new BigDecimal("3000"), BALANCE, RATE, EMI,
                RESERVE, IMPOUND, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum payment is 3485.84");
    }

    @Test
    void paymentAbovePayoffIsRejected() {
        assertThatThrownBy(() -> PaymentAllocator.allocate(new BigDecimal("400000"), BALANCE, RATE, EMI,
                RESERVE, IMPOUND, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("payoff");
    }

    @Test
    void lastInstallmentNeverTakesMorePrincipalThanOwed() {
        var due = PaymentAllocator.installment(new BigDecimal("40.00"), RATE, EMI, BigDecimal.ZERO,
                BigDecimal.ZERO, List.of());

        assertThat(due.principal()).isEqualByComparingTo("40.00");
        assertThat(due.interest()).isEqualByComparingTo("0.40");
    }
}
