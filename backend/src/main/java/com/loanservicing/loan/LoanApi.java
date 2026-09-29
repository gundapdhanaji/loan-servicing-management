package com.loanservicing.loan;

import java.math.BigDecimal;
import java.util.List;

/**
 * PUBLIC API of the loan module. Future: loan-service.
 *
 * The loan module OWNS the loan balances, so only it may change them.
 * The payment module moves money and then asks the loan module to apply it.
 */
public interface LoanApi {

    LoanDto onboard(CreateLoanRequest request);

    LoanDto get(Long loanId);

    /** Returns the loan only if the logged-in user may see it (staff, its borrower, or one of its lenders). */
    LoanDto getForCurrentUser(Long loanId);

    List<LoanDto> findForCurrentUser();

    AmountDueDto getAmountDue(Long loanId);

    /** Splits a received payment into charges / interest / principal / escrow and updates the loan. */
    PaymentApplication applyPayment(Long loanId, BigDecimal amount);

    /** Undoes applyPayment - used when an ACH payment bounces (NSF). */
    void reversePayment(PaymentApplication application);

    ChargeDto addCharge(Long loanId, ChargeType type, BigDecimal amount, String description);

    /** Who funded the loan and what share each lender gets. */
    List<FundingShare> getFundingShares(Long loanId);
}
