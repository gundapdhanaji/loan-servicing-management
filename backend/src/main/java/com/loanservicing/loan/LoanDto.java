package com.loanservicing.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything the React "Loan Details" screen needs, in one response. */
public record LoanDto(
        Long id,
        String loanNumber,
        Long borrowerId,
        LoanStatus status,
        LoanCategory category,
        LoanPurpose purpose,
        int lienPriority,
        BigDecimal originalBalance,
        BigDecimal principalBalance,
        BigDecimal noteRate,
        int termMonths,
        BigDecimal monthlyPiPayment,
        BigDecimal reservePayment,
        BigDecimal impoundPayment,
        int graceDays,
        BigDecimal lateChargePercent,
        BigDecimal lateChargeMinimum,
        LocalDate closingDate,
        LocalDate firstPaymentDate,
        LocalDate maturityDate,
        LocalDate nextDueDate,
        LocalDate lastPaymentDate,
        LocalDate paidOffDate,
        long daysPastDue,
        BigDecimal reserveBalance,
        BigDecimal impoundBalance,
        BigDecimal unpaidCharges,
        List<PropertyDto> properties,
        List<FundingDto> fundings,
        List<InsuranceDto> insurances,
        List<ChargeDto> charges) {

    public record PropertyDto(Long id, String street, String city, String state, String zipCode,
                              String propertyType, String occupancy, BigDecimal appraisedValue,
                              String floodZone, String legalDescription, boolean primary) {
    }

    public record FundingDto(Long id, Long lenderId, BigDecimal fundedAmount, BigDecimal lenderRate,
                             LocalDate fundingDate) {
    }

    public record InsuranceDto(Long id, String companyName, String policyNumber, BigDecimal coverageAmount,
                               LocalDate expirationDate, String agentName, String agentPhone, String agentEmail,
                               boolean expiringSoon) {
    }
}
