package com.loanservicing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanservicing.auth.AuthApi;
import com.loanservicing.auth.Role;
import com.loanservicing.borrower.AccountType;
import com.loanservicing.borrower.AddBankAccountRequest;
import com.loanservicing.borrower.BankAccountDto;
import com.loanservicing.borrower.BorrowerApi;
import com.loanservicing.borrower.BorrowerDto;
import com.loanservicing.borrower.CreateBorrowerRequest;
import com.loanservicing.borrower.TinType;
import com.loanservicing.lender.CreateLenderRequest;
import com.loanservicing.lender.LenderApi;
import com.loanservicing.lender.LenderDto;
import com.loanservicing.loan.ChargeStatus;
import com.loanservicing.loan.ChargeType;
import com.loanservicing.loan.CreateLoanRequest;
import com.loanservicing.loan.CreateLoanRequest.FundingRequest;
import com.loanservicing.loan.LoanApi;
import com.loanservicing.loan.LoanCategory;
import com.loanservicing.loan.LoanDto;
import com.loanservicing.loan.LoanJobs;
import com.loanservicing.loan.LoanPurpose;
import com.loanservicing.payment.PaymentJobs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the main business flows, through the real REST API, on H2.
 * Each test creates its own users (unique emails), so tests don't disturb each other.
 */
// Tests use an in-memory H2 database (so they never touch your PostgreSQL data)
// and the "test" profile (so no sample data and no dev tools).
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanServicingFlowTest {

    private static final String PASSWORD = "password123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthApi authApi;
    @Autowired LenderApi lenderApi;
    @Autowired BorrowerApi borrowerApi;
    @Autowired LoanApi loanApi;
    @Autowired LoanJobs loanJobs;
    @Autowired PaymentJobs paymentJobs;
    @Autowired Clock clock;

    @Test
    void borrowerPaysAndLendersGetTheirShare() throws Exception {
        LenderDto a = lender();
        LenderDto b = lender();
        Borrower john = borrower("12345678");
        LoanDto loan = loan(john.dto.id(), today().plusDays(10), List.of(
                new FundingRequest(a.id(), new BigDecimal("180000"), new BigDecimal("10"), null),
                new FundingRequest(b.id(), new BigDecimal("120000"), new BigDecimal("10"), null)));

        String token = login(john.dto.email());

        // Amount due: interest 3000 + principal 85.84 + reserve 150 + impound 250
        JsonNode due = getJson("/api/v1/loans/" + loan.id() + "/amount-due", token);
        assertThat(due.get("totalDue").decimalValue()).isEqualByComparingTo("3485.84");

        // Pay it
        String key = UUID.randomUUID().toString();
        JsonNode payment = pay(token, loan.id(), john.bankAccountId, "3485.84", key);
        assertThat(payment.get("status").asText()).isEqualTo("POSTED");
        assertThat(payment.get("interestPaid").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(payment.get("principalPaid").decimalValue()).isEqualByComparingTo("85.84");

        // Loan updated: principal down, next due date one month later, escrow collected
        LoanDto after = loanApi.get(loan.id());
        assertThat(after.principalBalance()).isEqualByComparingTo("299914.16");
        assertThat(after.nextDueDate()).isEqualTo(loan.nextDueDate().plusMonths(1));
        assertThat(after.reserveBalance()).isEqualByComparingTo("150.00");

        // Same Idempotency-Key again = same payment, not a second charge
        JsonNode again = pay(token, loan.id(), john.bankAccountId, "3485.84", key);
        assertThat(again.get("id").asLong()).isEqualTo(payment.get("id").asLong());
        assertThat(loanApi.get(loan.id()).principalBalance()).isEqualByComparingTo("299914.16");

        // Lender A (60%) sees their disbursement: principal 51.50, interest 3000 x 0.6 x 10/12 = 1500
        JsonNode disbursements = getJson("/api/v1/disbursements", login(a.email()));
        JsonNode mine = findByLong(disbursements, "paymentId", payment.get("id").asLong());
        assertThat(mine.get("principalAmount").decimalValue()).isEqualByComparingTo("51.50");
        assertThat(mine.get("interestAmount").decimalValue()).isEqualByComparingTo("1500.00");
    }

    @Test
    void loginWorksLikeLoanLinq() throws Exception {
        Borrower john = borrower("12345678");

        // 1. get_auth_token {username, password} -> token + userid + axiosdata.is_trusted
        String body = json.writeValueAsString(Map.of("username", john.dto.email(), "password", PASSWORD));
        JsonNode auth = json.readTree(mvc.perform(post("/api/v1/auth/get_auth_token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = auth.get("token").asText();
        long userid = auth.get("userid").asLong();
        assertThat(auth.get("axiosdata").get("is_trusted").asBoolean()).isTrue();

        // 2. access_account {userid} with the jwt + user headers the LoanLinq axios interceptor sends
        String userHeader = json.writeValueAsString(Map.of("email", john.dto.email(), "userId", String.valueOf(userid)));
        JsonNode account = json.readTree(mvc.perform(post("/api/v1/auth/access_account")
                        .header("jwt", token).header("user", userHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("userid", userid))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(account.get("role").get(0).get("role_name").asText()).isEqualTo("BORROWER");
        assertThat(account.get("account").get(0).get("recid").asLong()).isEqualTo(john.dto.id());

        // 3. a "user" header that belongs to someone else is rejected
        String wrongUser = json.writeValueAsString(Map.of("email", "x@test.local", "userId", String.valueOf(userid + 999)));
        mvc.perform(get("/api/v1/auth/me").header("jwt", token).header("user", wrongUser))
                .andExpect(status().isUnauthorized());

        // 4. wrong password
        String bad = json.writeValueAsString(Map.of("username", john.dto.email(), "password", "wrong-password"));
        mvc.perform(post("/api/v1/auth/get_auth_token").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void borrowerCannotSeeSomeoneElsesLoan() throws Exception {
        LenderDto a = lender();
        Borrower owner = borrower("12345678");
        Borrower other = borrower("87654321");
        LoanDto loan = loan(owner.dto.id(), today().plusDays(10),
                List.of(new FundingRequest(a.id(), new BigDecimal("300000"), new BigDecimal("10"), null)));

        mvc.perform(get("/api/v1/loans/" + loan.id()).header("jwt", login(other.dto.email())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/loans/" + loan.id()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bouncedPaymentIsReversedAndChargedAnNsfFee() throws Exception {
        LenderDto a = lender();
        Borrower maria = borrower("555550000"); // ends in 0000 -> FakeAchGateway bounces it
        LoanDto loan = loan(maria.dto.id(), today().plusDays(10),
                List.of(new FundingRequest(a.id(), new BigDecimal("300000"), new BigDecimal("10"), null)));

        JsonNode payment = pay(login(maria.dto.email()), loan.id(), maria.bankAccountId, "3485.84", null);
        assertThat(loanApi.get(loan.id()).principalBalance()).isEqualByComparingTo("299914.16");

        assertThat(paymentJobs.processAchReturns()).isGreaterThanOrEqualTo(1);

        LoanDto after = loanApi.get(loan.id());
        assertThat(after.principalBalance()).isEqualByComparingTo("300000.00"); // reversed
        assertThat(after.nextDueDate()).isEqualTo(loan.nextDueDate());         // back to the same installment
        assertThat(after.unpaidCharges()).isEqualByComparingTo("25.00");       // NSF fee
        assertThat(after.charges()).anyMatch(c -> c.type() == ChargeType.NSF_FEE && c.status() == ChargeStatus.OPEN);

        JsonNode reloaded = getJson("/api/v1/payments/" + payment.get("id").asLong(), login(maria.dto.email()));
        assertThat(reloaded.get("status").asText()).isEqualTo("RETURNED");
        assertThat(reloaded.get("returnCode").asText()).isEqualTo("R01");
    }

    @Test
    void lateFeeIsChargedOnceAfterGraceDays() {
        LenderDto a = lender();
        Borrower late = borrower("12345678");
        LoanDto loan = loan(late.dto.id(), today().minusDays(15), // grace is 10 days
                List.of(new FundingRequest(a.id(), new BigDecimal("300000"), new BigDecimal("10"), null)));

        loanJobs.assessLateCharges();
        loanJobs.assessLateCharges(); // second run must not charge again

        LoanDto after = loanApi.get(loan.id());
        assertThat(after.charges()).filteredOn(c -> c.type() == ChargeType.LATE_FEE).hasSize(1);
        // 5% of 3085.84 = 154.29 (more than the 50.00 minimum)
        assertThat(after.unpaidCharges()).isEqualByComparingTo("154.29");
    }

    @Test
    void loanMoreThanThirtyDaysLateGoesToDefault() {
        LenderDto a = lender();
        Borrower david = borrower("12345678");
        LoanDto loan = loan(david.dto.id(), today().minusDays(40),
                List.of(new FundingRequest(a.id(), new BigDecimal("300000"), new BigDecimal("10"), null)));

        loanJobs.flagDefaults();

        assertThat(loanApi.get(loan.id()).status().name()).isEqualTo("DEFAULT");
    }

    // ------------------------------------------------------------------ helpers

    private record Borrower(BorrowerDto dto, Long bankAccountId) {
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    private LenderDto lender() {
        return lenderApi.create(new CreateLenderRequest("Test Lender", unique("lender"), PASSWORD, null,
                "123456789", "021000021", "99887766"));
    }

    private Borrower borrower(String accountNumber) {
        BorrowerDto dto = borrowerApi.create(new CreateBorrowerRequest("Test", "Borrower", unique("borrower"),
                PASSWORD, null, null, null, null, null, TinType.SSN, "123456789", true, true));
        BankAccountDto account = borrowerApi.addBankAccount(dto.id(), new AddBankAccountRequest("Test Bank",
                "Test Borrower", "111000025", accountNumber, AccountType.CHECKING));
        return new Borrower(dto, account.id());
    }

    /** 300,000 at 12% for 360 months -> EMI 3085.84; reserve 150, impound 250; grace 10 days; late fee 5% (min 50). */
    private LoanDto loan(Long borrowerId, LocalDate firstPaymentDate, List<FundingRequest> fundings) {
        return loanApi.onboard(new CreateLoanRequest(null, borrowerId, LoanCategory.RESIDENTIAL,
                LoanPurpose.PURCHASE, 1, new BigDecimal("300000.00"), new BigDecimal("12"), 360, null,
                new BigDecimal("150.00"), new BigDecimal("250.00"), 10, new BigDecimal("5"), new BigDecimal("50.00"),
                firstPaymentDate.minusDays(30), firstPaymentDate, List.of(), fundings, List.of()));
    }

    private String login(String email) throws Exception {
        String body = json.writeValueAsString(Map.of("username", email, "password", PASSWORD));
        String response = mvc.perform(post("/api/v1/auth/get_auth_token").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }

    private JsonNode pay(String token, Long loanId, Long bankAccountId, String amount, String key) throws Exception {
        String body = json.writeValueAsString(Map.of("loanId", loanId, "bankAccountId", bankAccountId,
                "amount", new BigDecimal(amount)));
        var request = post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("jwt", token);
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        String response = mvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String response = mvc.perform(get(url).header("jwt", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private static JsonNode findByLong(JsonNode array, String field, long value) {
        for (JsonNode node : array) {
            if (node.get(field).asLong() == value) {
                return node;
            }
        }
        throw new AssertionError("No element with " + field + "=" + value);
    }
}
