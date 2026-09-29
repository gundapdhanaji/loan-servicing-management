package com.loanservicing.lender.internal;

import com.loanservicing.auth.CurrentUser;
import com.loanservicing.common.NotFoundException;
import com.loanservicing.lender.CreateLenderRequest;
import com.loanservicing.lender.LenderDto;
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
@RequestMapping("/api/v1/lenders")
@RequiredArgsConstructor
public class LenderController {

    private final LenderService lenderService;

    /** Lender onboarding screen (admin). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public LenderDto create(@Valid @RequestBody CreateLenderRequest request) {
        return lenderService.create(request);
    }

    /** Lender management list (admin / customer service). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    public List<LenderDto> findAll() {
        return lenderService.findAll();
    }

    /** The logged-in lender's own profile. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('LENDER')")
    public LenderDto me() {
        Long userId = CurrentUser.get().userId();
        return lenderService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No lender profile for this user"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    public LenderDto get(@PathVariable Long id) {
        return lenderService.get(id);
    }
}
