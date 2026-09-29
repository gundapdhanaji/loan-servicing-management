package com.loanservicing.lender;

import com.loanservicing.common.BankDetails;

import java.util.List;
import java.util.Optional;

/**
 * PUBLIC API of the lender module.
 * Future: lender-service. Other services will call it through an HTTP client with this same interface.
 */
public interface LenderApi {

    LenderDto create(CreateLenderRequest request);

    LenderDto get(Long lenderId);

    List<LenderDto> findAll();

    Optional<LenderDto> findByUserId(Long userId);

    /** Where to send the lender's money (used by the payment module for disbursements). */
    BankDetails getBankDetails(Long lenderId);
}
