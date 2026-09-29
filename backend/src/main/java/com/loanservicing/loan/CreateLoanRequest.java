package com.loanservicing.loan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Loan onboarding in one request - the same tabs as the React screen:
 * Loan Details + Loan Terms + Property + Funding + Insurance.
 */
public record CreateLoanRequest(
        String loanNumber,                       // optional, generated if empty
        @NotNull Long borrowerId,
        @NotNull LoanCategory category,
        @NotNull LoanPurpose purpose,
        @Min(1) int lienPriority,                // 1st, 2nd, 3rd mortgage
        @NotNull @DecimalMin("1000.00") BigDecimal originalBalance,
        @NotNull @DecimalMin("0.0") BigDecimal noteRate,   // annual %, e.g. 12.0
        @Min(1) int termMonths,
        BigDecimal monthlyPiPayment,             // optional, calculated (EMI) if empty
        BigDecimal reservePayment,               // monthly amount kept aside (escrow)
        BigDecimal impoundPayment,               // monthly amount for property tax / insurance
        @Min(0) int graceDays,
        BigDecimal lateChargePercent,            // % of P&I payment
        BigDecimal lateChargeMinimum,
        @NotNull LocalDate closingDate,
        @NotNull LocalDate firstPaymentDate,
        @Valid List<PropertyRequest> properties,
        @NotEmpty @Valid List<FundingRequest> fundings,
        @Valid List<InsuranceRequest> insurances) {

    public record PropertyRequest(
            String street, String city, String state, String zipCode,
            String propertyType, String occupancy, BigDecimal appraisedValue,
            String floodZone, String legalDescription, boolean primary) {
    }

    /** One lender's part of the loan. All fundings must add up to originalBalance. */
    public record FundingRequest(
            @NotNull Long lenderId,
            @NotNull @DecimalMin("0.01") BigDecimal fundedAmount,
            @NotNull @DecimalMin("0.0") BigDecimal lenderRate,   // rate the lender earns, <= noteRate
            LocalDate fundingDate) {
    }

    public record InsuranceRequest(
            String companyName, String policyNumber, BigDecimal coverageAmount,
            LocalDate expirationDate, String agentName, String agentPhone, String agentEmail) {
    }
}
