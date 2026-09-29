package com.loanservicing.loan.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** The "Loan Terms" tab. Stored as columns of the loans table (@Embeddable, not a separate table). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class LoanTerms {

    /** Annual interest rate in %, e.g. 12.0000 */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal noteRate;

    @Column(nullable = false)
    private int termMonths;

    /** Principal & Interest part of the monthly installment (the EMI). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPiPayment;

    /** Monthly amount collected into the reserve (escrow) account. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reservePayment;

    /** Monthly amount collected for property tax / insurance (impound). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal impoundPayment;

    /** Days after the due date before a late fee is charged. */
    @Column(nullable = false)
    private int graceDays;

    /** Late fee = this % of the P&I payment... */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal lateChargePercent;

    /** ...but never less than this. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lateChargeMinimum;
}
