package com.loanservicing.loan.internal;

import com.loanservicing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The "Funding" tab: which lender put in how much money.
 * This is the Loan <-> Lender many-to-many, stored with extra data (amount, rate).
 */
@Entity
@Table(name = "loan_fundings", schema = "loan")
@Getter
@Setter
@NoArgsConstructor
public class LoanFunding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    /** Lender lives in another module - reference by ID only. */
    @Column(nullable = false)
    private Long lenderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fundedAmount;

    /** The annual rate this lender earns (the servicer keeps noteRate - lenderRate). */
    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal lenderRate;

    private LocalDate fundingDate;
}
