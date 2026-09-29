package com.loanservicing.lender.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface LenderRepository extends JpaRepository<Lender, Long> {

    Optional<Lender> findByUserId(Long userId);
}
