package com.loanservicing.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChargeDto(
        Long id,
        ChargeType type,
        LocalDate chargeDate,
        String description,
        BigDecimal originalAmount,
        BigDecimal balance,
        ChargeStatus status) {
}
