package com.loanservicing.loan.internal;

import com.loanservicing.common.BaseEntity;
import com.loanservicing.loan.ChargeStatus;
import com.loanservicing.loan.ChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The "Charges" tab: fees the borrower owes on top of the installment (late fee, NSF fee...). */
@Entity
@Table(name = "loan_charges", schema = "loan")
@Getter
@Setter
@NoArgsConstructor
public class LoanCharge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeType type;

    @Column(nullable = false)
    private LocalDate chargeDate;

    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originalAmount;

    /** Still owed. Becomes 0 when fully paid. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChargeStatus status = ChargeStatus.OPEN;
}
