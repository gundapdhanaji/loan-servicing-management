package com.loanservicing.payment.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** How much of a payment went to one specific charge (so it can be restored if the payment bounces). */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChargeLine {

    @Column(nullable = false)
    private Long chargeId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
