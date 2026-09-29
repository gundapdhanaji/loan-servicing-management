package com.loanservicing.borrower;

/** What the React "bank accounts" list shows. The full account number is never sent to the browser. */
public record BankAccountDto(
        Long id,
        String bankName,
        String accountHolderName,
        String routingNumber,
        String accountNumberMasked,
        AccountType accountType) {
}
