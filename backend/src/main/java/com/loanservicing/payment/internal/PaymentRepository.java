package com.loanservicing.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByAchReference(String achReference);

    List<Payment> findByLoanIdOrderByIdDesc(Long loanId);
}
