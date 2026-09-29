package com.loanservicing.borrower;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Local testing tip: an account number ending in 0000 will "bounce" (NSF)
 * when the ACH returns job runs. See FakeAchGateway.
 */
public record AddBankAccountRequest(
        @NotBlank String bankName,
        @NotBlank String accountHolderName,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "must be 9 digits") String routingNumber,
        @NotBlank @Pattern(regexp = "\\d{4,17}", message = "must be 4 to 17 digits") String accountNumber,
        @NotNull AccountType accountType) {
}
