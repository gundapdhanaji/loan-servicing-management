package com.loanservicing.borrower;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Matches the "Borrower Information" tab of the React loan onboarding screen. */
public record CreateBorrowerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        String phone,
        String street,
        String city,
        String state,
        String zipCode,
        @NotNull TinType tinType,
        @NotBlank String tin,
        Boolean sendLateNotices,
        Boolean sendPaymentReceipts) {
}
