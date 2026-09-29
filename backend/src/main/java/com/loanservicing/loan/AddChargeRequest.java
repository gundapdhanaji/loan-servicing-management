package com.loanservicing.loan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddChargeRequest(
        @NotNull ChargeType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String description) {
}
