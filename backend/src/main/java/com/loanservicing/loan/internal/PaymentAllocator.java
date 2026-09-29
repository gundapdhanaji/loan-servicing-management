package com.loanservicing.loan.internal;

import com.loanservicing.common.BusinessException;
import com.loanservicing.common.Money;
import com.loanservicing.loan.PaymentApplication.ChargePayment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Decides how a payment is split. Pure Java - no database, no Spring - so it is easy to unit test.
 *
 * Order (the "payment waterfall"):
 *   1. unpaid charges (late fees, NSF fees), oldest first
 *   2. interest for the current installment
 *   3. scheduled principal for the current installment
 *   4. reserve + impound (escrow)
 *   5. anything extra -> extra principal (prepayment)
 *
 * Simplification for learning: a payment must cover at least one full installment
 * (charges + interest + principal + escrow). Real systems also accept partial payments
 * and keep them in a "suspense" balance.
 */
public final class PaymentAllocator {

    private PaymentAllocator() {
    }

    /** One installment's breakdown, before any payment. */
    public record Installment(BigDecimal unpaidCharges, BigDecimal interest, BigDecimal principal,
                              BigDecimal reserve, BigDecimal impound) {

        public BigDecimal total() {
            return unpaidCharges.add(interest).add(principal).add(reserve).add(impound);
        }
    }

    public record OpenCharge(Long chargeId, BigDecimal balance) {
    }

    public record Allocation(List<ChargePayment> chargePayments, BigDecimal interest, BigDecimal principal,
                             BigDecimal extraPrincipal, BigDecimal reserve, BigDecimal impound) {
    }

    public static Installment installment(BigDecimal principalBalance, BigDecimal noteRate, BigDecimal monthlyPi,
                                          BigDecimal reserve, BigDecimal impound, List<OpenCharge> openCharges) {
        BigDecimal charges = openCharges.stream().map(OpenCharge::balance).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal interest = EmiCalculator.monthlyInterest(principalBalance, noteRate);
        // Principal part of the EMI, but never more than what is left (last installment)
        BigDecimal principal = Money.min(Money.max(monthlyPi.subtract(interest), Money.ZERO), principalBalance);
        return new Installment(Money.of(charges), interest, Money.of(principal), Money.of(reserve), Money.of(impound));
    }

    public static Allocation allocate(BigDecimal amount, BigDecimal principalBalance, BigDecimal noteRate,
                                      BigDecimal monthlyPi, BigDecimal reserve, BigDecimal impound,
                                      List<OpenCharge> openCharges) {
        amount = Money.of(amount);
        Installment due = installment(principalBalance, noteRate, monthlyPi, reserve, impound, openCharges);

        if (amount.compareTo(due.total()) < 0) {
            throw new BusinessException("Minimum payment is " + due.total() + " (received " + amount + ")");
        }

        BigDecimal remainingPrincipalAfterInstallment = principalBalance.subtract(due.principal());
        BigDecimal extra = amount.subtract(due.total());
        if (extra.compareTo(remainingPrincipalAfterInstallment) > 0) {
            BigDecimal payoff = due.total().add(remainingPrincipalAfterInstallment);
            throw new BusinessException("Payment is more than the payoff amount of " + payoff);
        }

        // 1. charges, oldest first - the amount covers all of them (checked above)
        List<ChargePayment> chargePayments = new ArrayList<>();
        for (OpenCharge charge : openCharges) {
            chargePayments.add(new ChargePayment(charge.chargeId(), Money.of(charge.balance())));
        }

        return new Allocation(chargePayments, due.interest(), due.principal(), Money.of(extra),
                due.reserve(), due.impound());
    }
}
