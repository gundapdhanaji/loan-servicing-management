package com.loanservicing.loan.internal;

import com.loanservicing.loan.AddChargeRequest;
import com.loanservicing.loan.AmountDueDto;
import com.loanservicing.loan.ChargeDto;
import com.loanservicing.loan.CreateLoanRequest;
import com.loanservicing.loan.LoanDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    /** Loan onboarding (admin) - all tabs in one request. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LoanDto onboard(@Valid @RequestBody CreateLoanRequest request) {
        return loanService.onboard(request);
    }

    /**
     * Admin/CSR: all loans. Lender: loans they funded (portfolio). Borrower: their own loans.
     * The same URL returns different data depending on who is logged in.
     */
    @GetMapping
    public List<LoanDto> myLoans() {
        return loanService.findForCurrentUser();
    }

    @GetMapping("/{id}")
    public LoanDto get(@PathVariable Long id) {
        return loanService.getForCurrentUser(id);
    }

    /** "Make a Payment" screen: minimum payment and payoff amount. */
    @GetMapping("/{id}/amount-due")
    public AmountDueDto amountDue(@PathVariable Long id) {
        loanService.getForCurrentUser(id); // access check
        return loanService.getAmountDue(id);
    }

    @PostMapping("/{id}/properties")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanDto addProperty(@PathVariable Long id, @Valid @RequestBody CreateLoanRequest.PropertyRequest request) {
        return loanService.addProperty(id, request);
    }

    @PostMapping("/{id}/insurances")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanDto addInsurance(@PathVariable Long id, @Valid @RequestBody CreateLoanRequest.InsuranceRequest request) {
        return loanService.addInsurance(id, request);
    }

    @PostMapping("/{id}/charges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    public ChargeDto addCharge(@PathVariable Long id, @Valid @RequestBody AddChargeRequest request) {
        return loanService.addCharge(id, request.type(), request.amount(), request.description());
    }

    @PostMapping("/{id}/charges/{chargeId}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ChargeDto waiveCharge(@PathVariable Long id, @PathVariable Long chargeId) {
        return loanService.waiveCharge(id, chargeId);
    }
}
