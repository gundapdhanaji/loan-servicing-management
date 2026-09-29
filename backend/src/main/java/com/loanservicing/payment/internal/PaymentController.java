package com.loanservicing.payment.internal;

import com.loanservicing.auth.CurrentUser;
import com.loanservicing.common.NotFoundException;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.lender.LenderDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * All payment-module URLs start with /api/v1/payments, /api/v1/disbursements or /api/v1/nsf-cases.
 * Keeping each module's URLs under its own prefix makes the API gateway routing easy later.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LenderDisbursementRepository disbursementRepository;
    private final NsfCaseRepository nsfCaseRepository;
    private final LenderApi lenderApi;

    /**
     * React: axios.post("/api/v1/payments", {loanId, bankAccountId, amount},
     *                   {headers: {"Idempotency-Key": crypto.randomUUID()}})
     */
    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BORROWER')")
    public PaymentDto makePayment(@Valid @RequestBody MakePaymentRequest request,
                                  @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return paymentService.makePayment(request, idempotencyKey);
    }

    /** Payment history of one loan: GET /api/v1/payments?loanId=1 */
    @GetMapping("/payments")
    public List<PaymentDto> byLoan(@RequestParam Long loanId) {
        return paymentService.findByLoan(loanId);
    }

    @GetMapping("/payments/{id}")
    public PaymentDto get(@PathVariable Long id) {
        return paymentService.get(id);
    }

    /** Lender: their own payouts. Admin/CSR: everyone's. */
    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('ADMIN','CSR','LENDER')")
    @Transactional(readOnly = true)
    public List<DisbursementDto> disbursements() {
        if (CurrentUser.isStaff()) {
            return disbursementRepository.findAllByOrderByIdDesc().stream().map(DisbursementDto::from).toList();
        }
        LenderDto lender = lenderApi.findByUserId(CurrentUser.get().userId())
                .orElseThrow(() -> new NotFoundException("No lender profile for this user"));
        return disbursementRepository.findByLenderIdOrderByIdDesc(lender.id()).stream()
                .map(DisbursementDto::from).toList();
    }

    @GetMapping("/nsf-cases")
    @PreAuthorize("hasAnyRole('ADMIN','CSR')")
    @Transactional(readOnly = true)
    public List<NsfCaseDto> nsfCases() {
        return nsfCaseRepository.findAllByOrderByIdDesc().stream().map(NsfCaseDto::from).toList();
    }
}
