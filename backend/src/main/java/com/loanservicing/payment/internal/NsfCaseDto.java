package com.loanservicing.payment.internal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NsfCaseDto(
        Long id,
        Long paymentId,
        Long loanId,
        Long borrowerId,
        BigDecimal amount,
        String returnCode,
        String reason,
        BigDecimal feeCharged,
        LocalDate caseDate) {

    static NsfCaseDto from(NsfCase c) {
        return new NsfCaseDto(c.getId(), c.getPaymentId(), c.getLoanId(), c.getBorrowerId(), c.getAmount(),
                c.getReturnCode(), c.getReason(), c.getFeeCharged(), c.getCaseDate());
    }
}
