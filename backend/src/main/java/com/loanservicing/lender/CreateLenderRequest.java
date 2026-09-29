package com.loanservicing.lender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin onboards a lender: creates their login and their lender profile in one step. */
public record CreateLenderRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        String phone,
        @NotBlank String taxId,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "must be 9 digits") String routingNumber,
        @NotBlank @Pattern(regexp = "\\d{4,17}", message = "must be 4 to 17 digits") String accountNumber) {
}
