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

/** The "Property" tab: the real estate securing the loan (collateral). */
@Entity
@Table(name = "loan_properties", schema = "loan")
@Getter
@Setter
@NoArgsConstructor
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    private String street;
    private String city;

    @Column(length = 30)
    private String state;

    @Column(length = 15)
    private String zipCode;

    @Column(length = 30)
    private String propertyType;   // Single Family, Condo, Commercial...

    @Column(length = 30)
    private String occupancy;      // Owner Occupied, Investment...

    @Column(precision = 15, scale = 2)
    private BigDecimal appraisedValue;

    @Column(length = 30)
    private String floodZone;

    @Column(length = 1000)
    private String legalDescription;

    private boolean primaryProperty;
}
