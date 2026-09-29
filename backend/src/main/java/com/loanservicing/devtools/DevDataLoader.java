package com.loanservicing.devtools;

import com.loanservicing.auth.AuthApi;
import com.loanservicing.auth.Role;
import com.loanservicing.borrower.AccountType;
import com.loanservicing.borrower.AddBankAccountRequest;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.borrower.CreateBorrowerRequest;
import com.loanservicing.borrower.TinType;
import com.loanservicing.common.Money;
import com.loanservicing.lender.CreateLenderRequest;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.lender.LenderDto;
import com.loanservicing.loan.CreateLoanRequest;
import com.loanservicing.loan.CreateLoanRequest.FundingRequest;
import com.loanservicing.loan.CreateLoanRequest.InsuranceRequest;
import com.loanservicing.loan.CreateLoanRequest.PropertyRequest;
import com.loanservicing.loan.LoanApi;
import com.loanservicing.loan.LoanCategory;
import com.loanservicing.loan.LoanDto;
import com.loanservicing.loan.LoanPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * LOCAL ONLY: fills an empty database with sample data on startup, using the modules'
 * public APIs (exactly how another service would). All passwords are "password".
 *
 *   admin@loan.local     ADMIN
 *   csr@loan.local       CSR
 *   lender1@loan.local   LENDER  - funds loan 1 (60%) and loan 2 (100%)
 *   lender2@loan.local   LENDER  - funds loan 1 (40%) and loan 3 (100%)
 *   john@loan.local      BORROWER - loan 1, current (next payment due in 10 days)
 *   maria@loan.local     BORROWER - loan 2, 15 days late -> run late-charges job
 *                                   bank account ends in 0000 -> her payments BOUNCE
 *   david@loan.local     BORROWER - loan 3, 40 days late -> run defaults job
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class DevDataLoader implements CommandLineRunner {

    private static final String PASSWORD = "password";

    private final AuthApi authApi;
    private final LenderApi lenderApi;
    private final BorrowerApi borrowerApi;
    private final LoanApi loanApi;
    private final Clock clock;

    @Override
    public void run(String... args) {
        if (authApi.emailExists("admin@loan.local")) {
            log.info("Sample data already present - skipping");
            return;
        }
        LocalDate today = LocalDate.now(clock);

        authApi.createUser("admin@loan.local", PASSWORD, Role.ADMIN);
        authApi.createUser("csr@loan.local", PASSWORD, Role.CSR);

        LenderDto lender1 = lenderApi.create(new CreateLenderRequest("Evergreen Capital LLC",
                "lender1@loan.local", PASSWORD, "555-0101", "123456789", "021000021", "111122223333"));
        LenderDto lender2 = lenderApi.create(new CreateLenderRequest("Summit Private Lending",
                "lender2@loan.local", PASSWORD, "555-0102", "987654321", "026009593", "444455556666"));

        BorrowerDto john = borrower("John", "Smith", "john@loan.local", "123-45-6789", "987654321");
        BorrowerDto maria = borrower("Maria", "Garcia", "maria@loan.local", "234-56-7890", "555550000");
        BorrowerDto david = borrower("David", "Lee", "david@loan.local", "345-67-8901", "112233445");

        // Loan 1: current, funded 60/40 by two lenders
        LoanDto loan1 = loan(john.id(), "300000.00", "12.0", 360, today.minusDays(20), today.plusDays(10),
                LoanCategory.RESIDENTIAL, "12 Oak Street",
                List.of(new FundingRequest(lender1.id(), new BigDecimal("180000.00"), new BigDecimal("10.0"), null),
                        new FundingRequest(lender2.id(), new BigDecimal("120000.00"), new BigDecimal("10.0"), null)));

        // Loan 2: first payment was due 15 days ago (grace is 10 days) -> late fee
        LoanDto loan2 = loan(maria.id(), "150000.00", "11.0", 240, today.minusDays(45), today.minusDays(15),
                LoanCategory.RESIDENTIAL, "48 Pine Avenue",
                List.of(new FundingRequest(lender1.id(), new BigDecimal("150000.00"), new BigDecimal("9.5"), null)));

        // Loan 3: first payment was due 40 days ago -> default
        LoanDto loan3 = loan(david.id(), "80000.00", "13.0", 120, today.minusDays(70), today.minusDays(40),
                LoanCategory.COMMERCIAL, "7 Market Plaza",
                List.of(new FundingRequest(lender2.id(), new BigDecimal("80000.00"), new BigDecimal("11.0"), null)));

        log.info("""

                ================= SAMPLE DATA CREATED (password for everyone: "password") =================
                admin@loan.local (ADMIN)   csr@loan.local (CSR)
                lender1@loan.local, lender2@loan.local (LENDER)
                john@loan.local  -> {} current, EMI {}
                maria@loan.local -> {} 15 days late (her bank account bounces: ends in 0000)
                david@loan.local -> {} 40 days late
                Swagger UI: http://localhost:8080/swagger-ui.html
                ===========================================================================================""",
                loan1.loanNumber(), loan1.monthlyPiPayment(), loan2.loanNumber(), loan3.loanNumber());
    }

    private BorrowerDto borrower(String first, String last, String email, String ssn, String accountNumber) {
        BorrowerDto b = borrowerApi.create(new CreateBorrowerRequest(first, last, email, PASSWORD, "555-0200",
                "100 Main Street", "Austin", "TX", "73301", TinType.SSN, ssn, true, true));
        borrowerApi.addBankAccount(b.id(), new AddBankAccountRequest("First Community Bank",
                first + " " + last, "111000025", accountNumber, AccountType.CHECKING));
        return b;
    }

    private LoanDto loan(Long borrowerId, String amount, String rate, int months, LocalDate closing,
                         LocalDate firstPayment, LoanCategory category, String street, List<FundingRequest> fundings) {
        return loanApi.onboard(new CreateLoanRequest(null, borrowerId, category, LoanPurpose.PURCHASE, 1,
                new BigDecimal(amount), new BigDecimal(rate), months, null,
                new BigDecimal("150.00"), new BigDecimal("250.00"), 10,
                new BigDecimal("5.0"), new BigDecimal("50.00"), closing, firstPayment,
                List.of(new PropertyRequest(street, "Austin", "TX", "73301", "Single Family", "Owner Occupied",
                        Money.of(new BigDecimal(amount).multiply(new BigDecimal("1.4"))), "X - Low Risk", null, true)),
                fundings,
                List.of(new InsuranceRequest("Lone Star Insurance", "POL-" + street.hashCode(),
                        new BigDecimal(amount), LocalDate.now(clock).plusDays(200), "Agent Kim", "555-0300",
                        "agent@insurance.local"))));
    }
}
