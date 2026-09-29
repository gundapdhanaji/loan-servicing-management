package com.loanservicing.payment.internal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** React "Make a Payment": which loan, from which bank account, how much. */
public record MakePaymentRequest(
        @NotNull Long loanId,
        @NotNull Long bankAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
