package com.loanservicing.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface LenderDisbursementRepository extends JpaRepository<LenderDisbursement, Long> {

    List<LenderDisbursement> findByPaymentId(Long paymentId);

    List<LenderDisbursement> findByStatus(DisbursementStatus status);

    List<LenderDisbursement> findByLenderIdOrderByIdDesc(Long lenderId);

    List<LenderDisbursement> findAllByOrderByIdDesc();
}
