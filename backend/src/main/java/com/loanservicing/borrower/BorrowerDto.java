package com.loanservicing.borrower;

import java.time.Instant;

public record BorrowerDto(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String street,
        String city,
        String state,
        String zipCode,
        TinType tinType,
        String tinMasked,
        boolean sendLateNotices,
        boolean sendPaymentReceipts,
        Instant createdAt) {
}
