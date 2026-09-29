package com.loanservicing.payment.internal;

import com.loanservicing.common.BaseEntity;
import com.loanservicing.loan.PaymentApplication;
import com.loanservicing.payment.PaymentStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One payment received from a borrower, with how it was applied (the breakdown).
 * Payments are never deleted or edited; a bounce only changes the status to RETURNED.
 */
@Entity
@Table(name = "payments", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long borrowerId;

    private Long bankAccountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status = PaymentStatus.POSTED;

    /** Same key sent twice (double click, axios retry) = same payment, not a second charge. */
    @Column(unique = true, length = 100)
    private String idempotencyKey;

    /** The ACH trace number from the bank. */
    @Column(unique = true, length = 40)
    private String achReference;

    @Column(nullable = false)
    private LocalDate paymentDate;

    // ----- breakdown (from the loan module's PaymentApplication) -----
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal chargesPaid;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPaid;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalPaid;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal extraPrincipalPaid;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reservePaid;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal impoundPaid;

    private LocalDate dueDateBefore;
    private LocalDate dueDateAfter;
    private boolean loanPaidOff;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "payment_charge_lines", schema = "payment", joinColumns = @JoinColumn(name = "payment_id"))
    private List<ChargeLine> chargeLines = new ArrayList<>();

    // ----- set when the payment bounces -----
    private LocalDate returnedDate;

    @Column(length = 5)
    private String returnCode;

    private String returnReason;

    public void recordApplication(PaymentApplication app) {
        chargeLines.clear();
        app.chargePayments().forEach(cp -> chargeLines.add(new ChargeLine(cp.chargeId(), cp.amount())));
        chargesPaid = app.chargesTotal();
        interestPaid = app.interest();
        principalPaid = app.principal();
        extraPrincipalPaid = app.extraPrincipal();
        reservePaid = app.reserve();
        impoundPaid = app.impound();
        dueDateBefore = app.dueDateBefore();
        dueDateAfter = app.dueDateAfter();
        loanPaidOff = app.loanPaidOff();
    }

    /** Rebuilds what the loan module needs to reverse this payment. */
    public PaymentApplication toApplication() {
        return new PaymentApplication(loanId,
                chargeLines.stream().map(l -> new PaymentApplication.ChargePayment(l.getChargeId(), l.getAmount()))
                        .toList(),
                interestPaid, principalPaid, extraPrincipalPaid, reservePaid, impoundPaid,
                dueDateBefore, dueDateAfter, loanPaidOff);
    }
}
