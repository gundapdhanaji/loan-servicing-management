package com.loanservicing.borrower.internal;

import com.loanservicing.auth.CurrentUser;
import com.loanservicing.borrower.AddBankAccountRequest;
import com.loanservicing.borrower.BankAccountDto;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.borrower.CreateBorrowerRequest;
import com.loanservicing.common.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/borrowers")
@RequiredArgsConstructor
public class BorrowerController {

    private final BorrowerService borrowerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public BorrowerDto create(@Valid @RequestBody CreateBorrowerRequest request) {
        return borrowerService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    public List<BorrowerDto> findAll() {
        return borrowerService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    public BorrowerDto get(@PathVariable Long id) {
        return borrowerService.get(id);
    }

    // ---------- Endpoints for the logged-in borrower ("me") ----------

    @GetMapping("/me")
    @PreAuthorize("hasRole('BORROWER')")
    public BorrowerDto me() {
        return currentBorrower();
    }

    /** React: ACH Payments > Add Bank Account */
    @PostMapping("/me/bank-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BORROWER')")
    public BankAccountDto addBankAccount(@Valid @RequestBody AddBankAccountRequest request) {
        return borrowerService.addBankAccount(currentBorrower().id(), request);
    }

    @GetMapping("/me/bank-accounts")
    @PreAuthorize("hasRole('BORROWER')")
    public List<BankAccountDto> myBankAccounts() {
        return borrowerService.getBankAccounts(currentBorrower().id());
    }

    @DeleteMapping("/me/bank-accounts/{bankAccountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BORROWER')")
    public void removeBankAccount(@PathVariable Long bankAccountId) {
        borrowerService.removeBankAccount(currentBorrower().id(), bankAccountId);
    }

    private BorrowerDto currentBorrower() {
        return borrowerService.findByUserId(CurrentUser.get().userId())
                .orElseThrow(() -> new NotFoundException("No borrower profile for this user"));
    }
}
