package com.loanservicing.borrower;

import com.loanservicing.common.BankDetails;

import java.util.List;
import java.util.Optional;

/**
 * PUBLIC API of the borrower module.
 * Future: borrower-service (or a combined "party-service" together with lenders).
 */
public interface BorrowerApi {

    BorrowerDto create(CreateBorrowerRequest request);

    BorrowerDto get(Long borrowerId);

    List<BorrowerDto> findAll();

    Optional<BorrowerDto> findByUserId(Long userId);

    BankAccountDto addBankAccount(Long borrowerId, AddBankAccountRequest request);

    List<BankAccountDto> getBankAccounts(Long borrowerId);

    /** Full bank details for an ACH debit. Fails if the account does not belong to this borrower. */
    BankDetails getBankDetails(Long borrowerId, Long bankAccountId);
}
