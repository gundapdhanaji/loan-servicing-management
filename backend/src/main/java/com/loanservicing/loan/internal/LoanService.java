package com.loanservicing.loan.internal;

import com.loanservicing.auth.AuthenticatedUser;
import com.loanservicing.auth.CurrentUser;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.common.BusinessException;
import com.loanservicing.common.Money;
import com.loanservicing.common.NotFoundException;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.lender.LenderDto;
import com.loanservicing.loan.AmountDueDto;
import com.loanservicing.loan.ChargeDto;
import com.loanservicing.loan.ChargeStatus;
import com.loanservicing.loan.ChargeType;
import com.loanservicing.loan.CreateLoanRequest;
import com.loanservicing.loan.FundingShare;
import com.loanservicing.loan.LoanApi;
import com.loanservicing.loan.LoanDefaultedEvent;
import com.loanservicing.loan.LoanDto;
import com.loanservicing.loan.LoanJobs;
import com.loanservicing.loan.LoanStatus;
import com.loanservicing.loan.PaymentApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService implements LoanApi, LoanJobs {

    private final LoanRepository loanRepository;
    private final BorrowerApi borrowerApi;
    private final LenderApi lenderApi;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    // =====================================================================
    // Onboarding
    // =====================================================================

    @Override
    @Transactional
    public LoanDto onboard(CreateLoanRequest r) {
        borrowerApi.get(r.borrowerId()); // throws 404 if the borrower does not exist
        validateFundings(r);
        if (r.firstPaymentDate().isBefore(r.closingDate())) {
            throw new BusinessException("First payment date cannot be before the closing date");
        }

        Loan loan = new Loan();
        loan.setLoanNumber(resolveLoanNumber(r.loanNumber()));
        loan.setBorrowerId(r.borrowerId());
        loan.setCategory(r.category());
        loan.setPurpose(r.purpose());
        loan.setLienPriority(r.lienPriority());
        loan.setOriginalBalance(Money.of(r.originalBalance()));
        loan.setPrincipalBalance(Money.of(r.originalBalance()));
        loan.setClosingDate(r.closingDate());
        loan.setFirstPaymentDate(r.firstPaymentDate());
        loan.setMaturityDate(r.firstPaymentDate().plusMonths(r.termMonths() - 1L));
        loan.setNextDueDate(r.firstPaymentDate());

        LoanTerms terms = new LoanTerms();
        terms.setNoteRate(r.noteRate());
        terms.setTermMonths(r.termMonths());
        terms.setMonthlyPiPayment(Money.isPositive(r.monthlyPiPayment())
                ? Money.of(r.monthlyPiPayment())
                : EmiCalculator.emi(r.originalBalance(), r.noteRate(), r.termMonths()));
        terms.setReservePayment(Money.of(orZero(r.reservePayment())));
        terms.setImpoundPayment(Money.of(orZero(r.impoundPayment())));
        terms.setGraceDays(r.graceDays());
        terms.setLateChargePercent(r.lateChargePercent() == null ? new BigDecimal("5") : r.lateChargePercent());
        terms.setLateChargeMinimum(Money.of(orZero(r.lateChargeMinimum())));
        loan.setTerms(terms);

        if (r.properties() != null) {
            r.properties().forEach(p -> loan.addProperty(toEntity(p)));
        }
        for (CreateLoanRequest.FundingRequest f : r.fundings()) {
            LoanFunding funding = new LoanFunding();
            funding.setLenderId(f.lenderId());
            funding.setFundedAmount(Money.of(f.fundedAmount()));
            funding.setLenderRate(f.lenderRate());
            funding.setFundingDate(f.fundingDate() == null ? r.closingDate() : f.fundingDate());
            loan.addFunding(funding);
        }
        if (r.insurances() != null) {
            r.insurances().forEach(i -> loan.addInsurance(toEntity(i)));
        }

        Loan saved = loanRepository.save(loan);
        log.info("Onboarded loan {} for borrower {}", saved.getLoanNumber(), saved.getBorrowerId());
        return toDto(saved);
    }

    private void validateFundings(CreateLoanRequest r) {
        BigDecimal total = Money.ZERO;
        for (CreateLoanRequest.FundingRequest f : r.fundings()) {
            lenderApi.get(f.lenderId()); // throws 404 if the lender does not exist
            if (f.lenderRate().compareTo(r.noteRate()) > 0) {
                throw new BusinessException("Lender rate cannot be higher than the note rate");
            }
            total = total.add(Money.of(f.fundedAmount()));
        }
        if (total.compareTo(Money.of(r.originalBalance())) != 0) {
            throw new BusinessException("Fundings add up to " + total + " but the loan amount is "
                    + Money.of(r.originalBalance()));
        }
    }

    private String resolveLoanNumber(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (loanRepository.existsByLoanNumber(requested)) {
                throw new BusinessException("Loan number " + requested + " already exists");
            }
            return requested;
        }
        long next = loanRepository.count() + 1001;
        String generated = "LN-" + next;
        while (loanRepository.existsByLoanNumber(generated)) {
            generated = "LN-" + (++next);
        }
        return generated;
    }

    @Transactional
    public LoanDto addProperty(Long loanId, CreateLoanRequest.PropertyRequest request) {
        Loan loan = find(loanId);
        loan.addProperty(toEntity(request));
        // The loan is already managed by Hibernate: no save() needed. (Calling save() on a managed
        // entity does a merge, which can duplicate the new child.) flush() just assigns the new id now.
        loanRepository.flush();
        return toDto(loan);
    }

    @Transactional
    public LoanDto addInsurance(Long loanId, CreateLoanRequest.InsuranceRequest request) {
        Loan loan = find(loanId);
        loan.addInsurance(toEntity(request));
        loanRepository.flush();
        return toDto(loan);
    }

    // =====================================================================
    // Reading + access control
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public LoanDto get(Long loanId) {
        return toDto(find(loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanDto getForCurrentUser(Long loanId) {
        Loan loan = find(loanId);
        checkCanView(loan);
        return toDto(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanDto> findForCurrentUser() {
        AuthenticatedUser user = CurrentUser.get();
        List<Loan> loans = switch (user.role()) {
            case ADMIN, CSR -> loanRepository.findAll();
            case BORROWER -> currentBorrowerId()
                    .map(loanRepository::findByBorrowerIdOrderByIdAsc).orElse(List.of());
            case LENDER -> currentLenderId()
                    .map(loanRepository::findFundedByLender).orElse(List.of());
        };
        return loans.stream().map(this::toDto).toList();
    }

    /**
     * The ownership check. Role checks (@PreAuthorize) are not enough: every borrower has the
     * BORROWER role, but a borrower must only see THEIR loans.
     */
    private void checkCanView(Loan loan) {
        if (CurrentUser.isStaff()) {
            return;
        }
        boolean allowed = switch (CurrentUser.get().role()) {
            case BORROWER -> currentBorrowerId().map(id -> id.equals(loan.getBorrowerId())).orElse(false);
            case LENDER -> currentLenderId().map(loan::isFundedBy).orElse(false);
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException("Not your loan");
        }
    }

    private Optional<Long> currentBorrowerId() {
        return borrowerApi.findByUserId(CurrentUser.get().userId()).map(BorrowerDto::id);
    }

    private Optional<Long> currentLenderId() {
        return lenderApi.findByUserId(CurrentUser.get().userId()).map(LenderDto::id);
    }

    // =====================================================================
    // Payments
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public AmountDueDto getAmountDue(Long loanId) {
        Loan loan = find(loanId);
        if (loan.getStatus() == LoanStatus.PAID_OFF) {
            return new AmountDueDto(loan.getId(), loan.getLoanNumber(), null, Money.ZERO, Money.ZERO,
                    Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
        }
        PaymentAllocator.Installment due = installment(loan);
        BigDecimal payoff = loan.getPrincipalBalance().add(due.interest()).add(due.unpaidCharges());
        return new AmountDueDto(loan.getId(), loan.getLoanNumber(), loan.getNextDueDate(), due.unpaidCharges(),
                due.interest(), due.principal(), due.reserve(), due.impound(), due.total(), Money.of(payoff));
    }

    @Override
    @Transactional
    public PaymentApplication applyPayment(Long loanId, BigDecimal amount) {
        Loan loan = find(loanId);
        if (loan.getStatus() == LoanStatus.PAID_OFF) {
            throw new BusinessException("Loan " + loan.getLoanNumber() + " is already paid off");
        }
        LoanTerms t = loan.getTerms();
        PaymentAllocator.Allocation a = PaymentAllocator.allocate(amount, loan.getPrincipalBalance(),
                t.getNoteRate(), t.getMonthlyPiPayment(), t.getReservePayment(), t.getImpoundPayment(),
                openCharges(loan));

        LocalDate today = LocalDate.now(clock);

        // 1. charges
        for (PaymentApplication.ChargePayment cp : a.chargePayments()) {
            LoanCharge charge = findCharge(loan, cp.chargeId());
            charge.setBalance(charge.getBalance().subtract(cp.amount()));
            if (charge.getBalance().signum() == 0) {
                charge.setStatus(ChargeStatus.PAID);
            }
        }
        // 2-3-5. interest is income (nothing to reduce); principal reduces the balance
        loan.setPrincipalBalance(loan.getPrincipalBalance().subtract(a.principal()).subtract(a.extraPrincipal()));
        // 4. escrow
        loan.setReserveBalance(loan.getReserveBalance().add(a.reserve()));
        loan.setImpoundBalance(loan.getImpoundBalance().add(a.impound()));

        LocalDate dueBefore = loan.getNextDueDate();
        loan.setLastPaymentDate(today);
        boolean paidOff = loan.getPrincipalBalance().signum() == 0;
        if (paidOff) {
            loan.setStatus(LoanStatus.PAID_OFF);
            loan.setPaidOffDate(today);
            loan.setNextDueDate(null);
        } else {
            loan.setNextDueDate(dueBefore.plusMonths(1));
            if (loan.getStatus() == LoanStatus.DEFAULT && !loan.isSeriouslyDelinquent(today)) {
                loan.setStatus(LoanStatus.ACTIVE); // borrower caught up - out of default
            }
        }

        log.info("Applied {} to loan {}: interest={}, principal={}, extra={}, charges={}",
                amount, loan.getLoanNumber(), a.interest(), a.principal(), a.extraPrincipal(),
                a.chargePayments().size());

        return new PaymentApplication(loan.getId(), a.chargePayments(), a.interest(), a.principal(),
                a.extraPrincipal(), a.reserve(), a.impound(), dueBefore, loan.getNextDueDate(), paidOff);
    }

    @Override
    @Transactional
    public void reversePayment(PaymentApplication app) {
        Loan loan = find(app.loanId());
        LocalDate today = LocalDate.now(clock);

        for (PaymentApplication.ChargePayment cp : app.chargePayments()) {
            LoanCharge charge = findCharge(loan, cp.chargeId());
            charge.setBalance(charge.getBalance().add(cp.amount()));
            if (charge.getStatus() == ChargeStatus.PAID) {
                charge.setStatus(ChargeStatus.OPEN);
            }
        }
        loan.setPrincipalBalance(loan.getPrincipalBalance().add(app.totalPrincipal()));
        loan.setReserveBalance(loan.getReserveBalance().subtract(app.reserve()));
        loan.setImpoundBalance(loan.getImpoundBalance().subtract(app.impound()));

        if (app.loanPaidOff()) {
            loan.setPaidOffDate(null);
            loan.setNextDueDate(app.dueDateBefore());
        } else if (loan.getNextDueDate().equals(app.dueDateAfter())) {
            // No other payment since this one: simply go back to where we were
            loan.setNextDueDate(app.dueDateBefore());
        } else {
            // Other payments came in after this one: move back one installment
            loan.setNextDueDate(loan.getNextDueDate().minusMonths(1));
        }
        loan.setStatus(loan.isSeriouslyDelinquent(today) ? LoanStatus.DEFAULT : LoanStatus.ACTIVE);
        log.info("Reversed a payment on loan {}", loan.getLoanNumber());
    }

    // =====================================================================
    // Charges
    // =====================================================================

    @Override
    @Transactional
    public ChargeDto addCharge(Long loanId, ChargeType type, BigDecimal amount, String description) {
        Loan loan = find(loanId);
        LoanCharge charge = newCharge(type, amount, description);
        loan.addCharge(charge);
        loanRepository.flush(); // cascade-persists the new charge so it gets its id
        return toDto(charge);
    }

    @Transactional
    public ChargeDto waiveCharge(Long loanId, Long chargeId) {
        Loan loan = find(loanId);
        LoanCharge charge = findCharge(loan, chargeId);
        if (charge.getStatus() != ChargeStatus.OPEN) {
            throw new BusinessException("Only open charges can be waived");
        }
        charge.setStatus(ChargeStatus.WAIVED);
        charge.setBalance(Money.ZERO);
        return toDto(charge);
    }

    private LoanCharge newCharge(ChargeType type, BigDecimal amount, String description) {
        LoanCharge charge = new LoanCharge();
        charge.setType(type);
        charge.setChargeDate(LocalDate.now(clock));
        charge.setDescription(description);
        charge.setOriginalAmount(Money.of(amount));
        charge.setBalance(Money.of(amount));
        return charge;
    }

    // =====================================================================
    // Funding shares (used by the payment module to pay lenders)
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<FundingShare> getFundingShares(Long loanId) {
        Loan loan = find(loanId);
        return loan.getFundings().stream()
                .map(f -> new FundingShare(f.getLenderId(), f.getFundedAmount(),
                        f.getFundedAmount().divide(loan.getOriginalBalance(), MathContext.DECIMAL64),
                        f.getLenderRate(), loan.getTerms().getNoteRate()))
                .toList();
    }

    // =====================================================================
    // Nightly jobs
    // =====================================================================

    @Override
    @Transactional
    public int assessLateCharges() {
        LocalDate today = LocalDate.now(clock);
        int count = 0;
        for (Loan loan : loanRepository.findByStatusIn(List.of(LoanStatus.ACTIVE, LoanStatus.DEFAULT))) {
            LocalDate due = loan.getNextDueDate();
            LoanTerms t = loan.getTerms();
            boolean graceOver = due != null && today.isAfter(due.plusDays(t.getGraceDays()));
            boolean alreadyCharged = due != null && due.equals(loan.getLateChargeAssessedForDueDate());
            if (graceOver && !alreadyCharged) {
                BigDecimal fee = Money.max(
                        Money.of(t.getMonthlyPiPayment().multiply(t.getLateChargePercent()).divide(Money.HUNDRED)),
                        t.getLateChargeMinimum());
                loan.addCharge(newCharge(ChargeType.LATE_FEE, fee, "Late fee for installment due " + due));
                loan.setLateChargeAssessedForDueDate(due);
                count++;
            }
        }
        log.info("Late charge job: {} late fees added", count);
        return count;
    }

    @Override
    @Transactional
    public int flagDefaults() {
        LocalDate today = LocalDate.now(clock);
        int count = 0;
        for (Loan loan : loanRepository.findByStatusIn(List.of(LoanStatus.ACTIVE))) {
            if (loan.isSeriouslyDelinquent(today)) {
                loan.setStatus(LoanStatus.DEFAULT);
                events.publishEvent(new LoanDefaultedEvent(loan.getId(), loan.getLoanNumber(),
                        loan.getBorrowerId(), loan.daysPastDue(today)));
                count++;
            }
        }
        log.info("Default check job: {} loans moved to DEFAULT", count);
        return count;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Loan find(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> NotFoundException.of("Loan", id));
    }

    private LoanCharge findCharge(Loan loan, Long chargeId) {
        return loan.getCharges().stream().filter(c -> c.getId().equals(chargeId)).findFirst()
                .orElseThrow(() -> NotFoundException.of("Charge", chargeId));
    }

    private List<PaymentAllocator.OpenCharge> openCharges(Loan loan) {
        return loan.openCharges().stream()
                .map(c -> new PaymentAllocator.OpenCharge(c.getId(), c.getBalance()))
                .toList();
    }

    private PaymentAllocator.Installment installment(Loan loan) {
        LoanTerms t = loan.getTerms();
        return PaymentAllocator.installment(loan.getPrincipalBalance(), t.getNoteRate(), t.getMonthlyPiPayment(),
                t.getReservePayment(), t.getImpoundPayment(), openCharges(loan));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Property toEntity(CreateLoanRequest.PropertyRequest p) {
        Property e = new Property();
        e.setStreet(p.street());
        e.setCity(p.city());
        e.setState(p.state());
        e.setZipCode(p.zipCode());
        e.setPropertyType(p.propertyType());
        e.setOccupancy(p.occupancy());
        e.setAppraisedValue(p.appraisedValue());
        e.setFloodZone(p.floodZone());
        e.setLegalDescription(p.legalDescription());
        e.setPrimaryProperty(p.primary());
        return e;
    }

    private Insurance toEntity(CreateLoanRequest.InsuranceRequest i) {
        Insurance e = new Insurance();
        e.setCompanyName(i.companyName());
        e.setPolicyNumber(i.policyNumber());
        e.setCoverageAmount(i.coverageAmount());
        e.setExpirationDate(i.expirationDate());
        e.setAgentName(i.agentName());
        e.setAgentPhone(i.agentPhone());
        e.setAgentEmail(i.agentEmail());
        return e;
    }

    private LoanDto toDto(Loan l) {
        LocalDate today = LocalDate.now(clock);
        LoanTerms t = l.getTerms();
        return new LoanDto(l.getId(), l.getLoanNumber(), l.getBorrowerId(), l.getStatus(), l.getCategory(),
                l.getPurpose(), l.getLienPriority(), l.getOriginalBalance(), l.getPrincipalBalance(),
                t.getNoteRate(), t.getTermMonths(), t.getMonthlyPiPayment(), t.getReservePayment(),
                t.getImpoundPayment(), t.getGraceDays(), t.getLateChargePercent(), t.getLateChargeMinimum(),
                l.getClosingDate(), l.getFirstPaymentDate(), l.getMaturityDate(), l.getNextDueDate(),
                l.getLastPaymentDate(), l.getPaidOffDate(), l.daysPastDue(today),
                l.getReserveBalance(), l.getImpoundBalance(), l.unpaidCharges(),
                l.getProperties().stream().map(p -> new LoanDto.PropertyDto(p.getId(), p.getStreet(), p.getCity(),
                        p.getState(), p.getZipCode(), p.getPropertyType(), p.getOccupancy(), p.getAppraisedValue(),
                        p.getFloodZone(), p.getLegalDescription(), p.isPrimaryProperty())).toList(),
                l.getFundings().stream().map(f -> new LoanDto.FundingDto(f.getId(), f.getLenderId(),
                        f.getFundedAmount(), f.getLenderRate(), f.getFundingDate())).toList(),
                l.getInsurances().stream().map(i -> new LoanDto.InsuranceDto(i.getId(), i.getCompanyName(),
                        i.getPolicyNumber(), i.getCoverageAmount(), i.getExpirationDate(), i.getAgentName(),
                        i.getAgentPhone(), i.getAgentEmail(),
                        i.getExpirationDate() != null && i.getExpirationDate().isBefore(today.plusDays(30)))).toList(),
                l.getCharges().stream().map(this::toDto).toList());
    }

    private ChargeDto toDto(LoanCharge c) {
        return new ChargeDto(c.getId(), c.getType(), c.getChargeDate(), c.getDescription(),
                c.getOriginalAmount(), c.getBalance(), c.getStatus());
    }
}
