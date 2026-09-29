package com.loanservicing.borrower.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BorrowerRepository extends JpaRepository<Borrower, Long> {

    Optional<Borrower> findByUserId(Long userId);
}
