package com.loanservicing.payment.internal;

import com.loanservicing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money owed to one lender from one borrower payment ("Past Payments to Lender" table in React).
 * A negative row is a clawback: the borrower's payment bounced after the lender was already paid.
 */
@Entity
@Table(name = "lender_disbursements", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
public class LenderDisbursement extends BaseEntity {

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long lenderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DisbursementStatus status = DisbursementStatus.PENDING;

    private LocalDate disbursedDate;

    @Column(length = 40)
    private String achReference;

    private String note;
}
