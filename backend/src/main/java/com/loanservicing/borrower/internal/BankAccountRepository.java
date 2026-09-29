package com.loanservicing.borrower.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByBorrowerIdAndActiveTrue(Long borrowerId);

    Optional<BankAccount> findByIdAndBorrowerId(Long id, Long borrowerId);
}
