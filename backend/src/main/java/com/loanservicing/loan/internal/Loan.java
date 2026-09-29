package com.loanservicing.loan.internal;

import com.loanservicing.common.BaseEntity;
import com.loanservicing.common.Money;
import com.loanservicing.loan.ChargeStatus;
import com.loanservicing.loan.LoanCategory;
import com.loanservicing.loan.LoanPurpose;
import com.loanservicing.loan.LoanStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The loan account - the heart of loan servicing.
 *
 * Notice: borrowerId is a plain Long, and LoanFunding has a plain lenderId.
 * The loan module never joins to the borrower or lender tables, so it can move to its own
 * database when it becomes loan-service.
 */
@Entity
@Table(name = "loans", schema = "loan")
@Getter
@Setter
@NoArgsConstructor
public class Loan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String loanNumber;

    @Column(nullable = false)
    private Long borrowerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanPurpose purpose;

    private int lienPriority = 1;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originalBalance;

    /** How much principal is still owed. Goes down with every payment. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalBalance;

    @Embedded
    private LoanTerms terms;

    private LocalDate closingDate;
    private LocalDate firstPaymentDate;
    private LocalDate maturityDate;

    /** The installment the borrower must pay next. Moves forward one month per full payment. */
    private LocalDate nextDueDate;
    private LocalDate lastPaymentDate;
    private LocalDate paidOffDate;

    /** Escrow money collected and held for the borrower (tax, insurance). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reserveBalance = Money.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal impoundBalance = Money.ZERO;

    /** Makes sure the late-fee job charges only once per missed installment. */
    private LocalDate lateChargeAssessedForDueDate;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Property> properties = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanFunding> fundings = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Insurance> insurances = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chargeDate ASC, id ASC")
    private List<LoanCharge> charges = new ArrayList<>();

    // ---------- helpers that keep both sides of the relationship in sync ----------

    public void addProperty(Property p) {
        p.setLoan(this);
        properties.add(p);
    }

    public void addFunding(LoanFunding f) {
        f.setLoan(this);
        fundings.add(f);
    }

    public void addInsurance(Insurance i) {
        i.setLoan(this);
        insurances.add(i);
    }

    public void addCharge(LoanCharge c) {
        c.setLoan(this);
        charges.add(c);
    }

    // ---------- business questions about the loan ----------

    /** Charges still owed, oldest first (they get paid first). */
    public List<LoanCharge> openCharges() {
        return charges.stream().filter(c -> c.getStatus() == ChargeStatus.OPEN).toList();
    }

    public BigDecimal unpaidCharges() {
        return openCharges().stream().map(LoanCharge::getBalance).reduce(Money.ZERO, BigDecimal::add);
    }

    public long daysPastDue(LocalDate today) {
        if (nextDueDate == null || !today.isAfter(nextDueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(nextDueDate, today);
    }

    /** 31 or more days past due = default. */
    public boolean isSeriouslyDelinquent(LocalDate today) {
        return daysPastDue(today) > 30;
    }

    public boolean isFundedBy(Long lenderId) {
        return fundings.stream().anyMatch(f -> f.getLenderId().equals(lenderId));
    }
}
