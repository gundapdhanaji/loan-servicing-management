package com.loanservicing.common;

/**
 * Bank details needed to move money by ACH. Only passed between modules on the server,
 * never sent to the browser in full (the UI gets a masked account number).
 */
public record BankDetails(String accountHolderName, String routingNumber, String accountNumber) {

    public String maskedAccountNumber() {
        return mask(accountNumber);
    }

    public static String mask(String number) {
        if (number == null || number.length() <= 4) {
            return "****";
        }
        return "****" + number.substring(number.length() - 4);
    }
}
