package com.loanservicing.payment.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DisbursementDto(
        Long id,
        Long paymentId,
        Long loanId,
        Long lenderId,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal totalAmount,
        DisbursementStatus status,
        LocalDate disbursedDate,
        String achReference,
        String note) {

    static DisbursementDto from(LenderDisbursement d) {
        return new DisbursementDto(d.getId(), d.getPaymentId(), d.getLoanId(), d.getLenderId(),
                d.getPrincipalAmount(), d.getInterestAmount(), d.getTotalAmount(), d.getStatus(),
                d.getDisbursedDate(), d.getAchReference(), d.getNote());
    }
}
