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

/** The "Insurance" tab: hazard insurance on the property. Expiring policies are flagged. */
@Entity
@Table(name = "loan_insurances", schema = "loan")
@Getter
@Setter
@NoArgsConstructor
public class Insurance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    private String companyName;

    @Column(length = 50)
    private String policyNumber;

    @Column(precision = 15, scale = 2)
    private BigDecimal coverageAmount;

    private LocalDate expirationDate;
    private String agentName;

    @Column(length = 30)
    private String agentPhone;

    private String agentEmail;
}
