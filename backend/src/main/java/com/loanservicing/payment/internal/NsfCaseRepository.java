package com.loanservicing.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface NsfCaseRepository extends JpaRepository<NsfCase, Long> {

    List<NsfCase> findAllByOrderByIdDesc();
}
