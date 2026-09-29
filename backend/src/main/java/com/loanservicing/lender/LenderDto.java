package com.loanservicing.lender;

import java.time.Instant;

public record LenderDto(
        Long id,
        Long userId,
        String name,
        String email,
        String phone,
        String taxIdMasked,
        String routingNumber,
        String accountNumberMasked,
        Instant createdAt) {
}
