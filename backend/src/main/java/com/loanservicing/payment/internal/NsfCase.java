package com.loanservicing.payment.internal;

import com.loanservicing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A bounced payment - the React "NSF Cases" table. */
@Entity
@Table(name = "nsf_cases", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
public class NsfCase extends BaseEntity {

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long borrowerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    private String returnCode;

    private String reason;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal feeCharged;

    @Column(nullable = false)
    private LocalDate caseDate;
}
