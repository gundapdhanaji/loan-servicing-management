# Loan Servicing Management

Spring Boot + Hibernate + PostgreSQL backend for a loan servicing platform (React front-end).

**New here? Start with [docs/SETUP.md]** – laptop setup from scratch, GitHub, and a 10-minute demo script.

The backend of the Loan Servicing Management application is designed for your React front-end. Everything runs **locally**: no real bank transactions are performed (ACH is simulated, and emails are printed to the console).

Complete architecture and flows (including diagrams): **[docs/ARCHITECTURE.md]**

The project follows a **modular monolith** architecture: it is a single application, but it is divided into separate modules. This makes it possible to convert each module into a separate **microservice** in the future.

For more details, see **[docs/MICROSERVICES.md]**.


---

## 1. Requirements

* **JDK 21** (or newer)
* **Maven 3.9+** (built into IntelliJ)
* **PostgreSQL 14+** (with pgAdmin)
* IntelliJ IDEA, Postman (optional)

## 2. How to Run the Application

After installing PostgreSQL, create the database once in pgAdmin:

```sql
CREATE DATABASE loan_servicing;
```

Then run the application:

```bash
mvn spring-boot:run
```

All configuration is available in a single file:

`src/main/resources/application.properties`

The default database username/password is `postgres` / `root`. If your database credentials are different, update:

* `spring.datasource.username`
* `spring.datasource.password`

Hibernate automatically creates **5 schemas**:

* `auth`
* `lender`
* `borrower`
* `loan`
* `payment`

It also automatically creates all the required tables using:

`ddl-auto: update`

Each module's tables are stored in its own schema. This makes it easier to give each service a separate database in the future when converting the modules into microservices.


For viewing data in **pgAdmin / DBeaver**, you can use the following queries:

```sql
SELECT id, loan_number, status, principal_balance, next_due_date FROM loan.loans;

SELECT id, loan_id, amount, status, interest_paid, principal_paid
FROM payment.payments
ORDER BY id DESC;

SELECT lender_id, principal_amount, interest_amount, status
FROM payment.lender_disbursements;

SELECT loan_id, type, balance, status
FROM loan.loan_charges;
```

### After the Application Starts

* **API:** `http://localhost:8080/api/v1/...`
* **Swagger UI:** `http://localhost:8080/swagger-ui.html` – You can try all APIs directly from the browser.

  * First, log in using `POST /api/v1/auth/get_auth_token`.
  * Copy the returned `token`.
  * Click the **Authorize** button at the top of Swagger and paste the token.
  * Swagger sends the token in the `jwt` header, similar to LoanLinq.

Sample users will be displayed in the console.

**Password for all users:** `password`

| Email                | Role     | Description                                                                           |
| -------------------- | -------- | ------------------------------------------------------------------------------------- |
| `admin@loan.local`   | ADMIN    | Can onboard everything and view everything                                            |
| `csr@loan.local`     | CSR      | Customer service; can view everything                                                 |
| `lender1@loan.local` | LENDER   | Funded Loan 1 (60%) and Loan 2                                                        |
| `lender2@loan.local` | LENDER   | Funded Loan 1 (40%) and Loan 3                                                        |
| `john@loan.local`    | BORROWER | Loan 1 – current; payment is due in 10 days                                           |
| `maria@loan.local`   | BORROWER | Loan 2 – 15 days late; her bank account ends in `...0000`, so the payment **bounces** |
| `david@loan.local`   | BORROWER | Loan 3 – 40 days late → default                                                       |

## 3. Tests

Run the tests using:

```bash
mvn test
```

* `PaymentAllocatorTest`, `DistributionCalculatorTest` – Test money calculations such as EMI, payment allocation, and lender share.
* `LoanServicingFlowTest` – Tests the complete flow through REST APIs, including payment, lender split, idempotency, NSF, late fees, default, and access control.
* The tests use their own **in-memory H2 database**. The settings are defined inside `LoanServicingFlowTest`, so they do not affect your PostgreSQL data.
* `ModularityTests` – Checks **module boundaries**, which is one of the most important tests for future microservices. Module diagrams are also generated in `target/spring-modulith-docs`.

---

## 4. Project Structure

```text
com.loanservicing
 ├── common        shared: BaseEntity, Money, errors, AppClock, BankDetails
 ├── auth          login, JWT, roles                  → future: auth-service
 ├── lender        lenders                            → future: lender-service
 ├── borrower      borrowers + bank accounts          → future: borrower-service
 ├── loan          loan, terms, property, funding,    → future: loan-service
 │                 insurance, charges, late fee/default jobs
 ├── payment       payments, fake ACH, lender split,  → future: payment-service
 │                 NSF, disbursements
 ├── notification  emails (console) from events       → future: notification-service
 ├── profile       access_account (roles + accounts)  → future: API gateway / BFF
 └── devtools      LOCAL ONLY: sample data, time travel, run jobs manually
```

### Structure Inside Each Module

Each module follows a structure like this:

```text
loan/
 ├── LoanApi.java, LoanDto.java, ...   ← PUBLIC API: other modules can use only these
 └── internal/                          ← PRIVATE: entity, repository, service, controller
      ├── Loan.java (@Entity)
      ├── LoanRepository.java (Spring Data JPA)
      ├── LoanService.java (business logic, implements LoanApi)
      └── LoanController.java (RES
```

```

4 Rules for Making the Application Microservice-Ready
1.One module does not use another module's internal classes.
It can use only that module's XxxApi interface. ModularityTests verifies this rule.
2.There are no JPA relationships between modules.
For example, Loan contains Long borrowerId, not Borrower borrower. This allows the Loan and Borrower modules to be separated into different services and databases in the future.
3.Notifications are handled through events, not direct calls.
Examples include PaymentPostedEvent, PaymentReturnedEvent, and LoanDefaultedEvent. In the future, these same events can be published through Kafka.
4.URLs are organized module-wise.
Examples: /api/v1/loans, /api/v1/payments, etc. This makes it easier to route requests through an API Gateway.

---

## 5. ## Payment Flow (Main Business Logic)

1. **Check the borrower's loan and bank account ownership.**

   * Verify that the loan exists and belongs to the borrower.
   * Verify that the bank account exists and belongs to the borrower.

2. **`LoanApi.applyPayment()` — Apply the payment using the waterfall method.**

   The payment is applied in this order:

   **Charges → Interest → Principal → Reserve/Impound (Escrow) → Extra Principal**

3. **`AchGateway.debit()` — Debit the money from the bank account.**

   * In the local application, `FakeAchGateway` is used to simulate the bank transaction.

4. **Save the payment and its breakdown.**

   * The payment details and the amount applied to each component are stored in the database.

5. **`DistributionService` — Calculate each lender's share.**

   * Each lender's portion is calculated according to their funding share.
   * The lender's distribution is stored in the `lender_disbursements` table.

6. **`PaymentPostedEvent` — Send a payment notification.**

   * The payment event is published.
   * The notification module processes the event and sends/displays the receipt in the console.

### Transaction

All these operations are performed within **one database transaction**.

If Step 3 (`AchGateway.debit()`) fails, the changes made in Step 2 (`LoanApi.applyPayment()`) are also **rolled back**.

---

## Lender Share Calculation

Example:

* Loan interest rate = **12%**
* Lender funding rate = **10%**
* Lender funding/ownership share = **60%**

The lender's share is calculated as:

* **Principal:** `Principal × 60%`
* **Interest:** `Interest × 60% × (10 / 12)`
* **Remaining interest (2% spread):** This represents the **servicer's fee/spread**.

---

## NSF (Bounced/Returned Payment) Flow

When a bank returns or bounces a payment:

```text
processAchReturns job
        ↓
NsfService
        ↓
Payment status = RETURNED
        ↓
Reverse the loan payment
        ↓
Cancel / Claw back lender's share
        ↓
Add NSF fee ($25)
        ↓
Create NSF case
        ↓
PaymentReturnedEvent
```

### Transaction

All these steps are executed within **one database transaction**.

If **Step 3 fails**, the changes made in **Step 2 are also rolled back**.

### Lender Share

Example:

* Loan interest rate = **12%**
* Lender funding rate = **10%**
* Lender's share = **60%**

Calculation:

* **Principal:** `Principal × 60%`
* **Interest:** `Interest × 60% × (10 / 12)`
* **Remaining interest (2% spread):** This is the **servicer's fee**.

### NSF (Bounced/Returned Payment)

When a payment is returned by the bank:

`processAchReturns` job → `NsfService`

The system performs the following actions:

1. Payment status is changed to **`RETURNED`**.
2. The payment applied to the loan is **reversed**.
3. The lender's share is **cancelled/clawed back**.
4. An **NSF fee of $25** is charged.
5. An **NSF case** is created.
6. A **`PaymentReturnedEvent`** is published.


---

## 6. Local testing: "time travel"

Late fees and loan defaults depend on specific dates. You do not need to wait 30 days in real time.

```bash
# Check today's application date
curl http://localhost:8080/api/v1/dev/clock

# Move the application clock 40 days forward
curl -X POST "http://localhost:8080/api/v1/dev/clock/advance?days=40"

# Run all nightly jobs immediately
curl -X POST http://localhost:8080/api/v1/dev/jobs/run-all

# Reset the application clock back to today
curl -X POST http://localhost:8080/api/v1/dev/clock/reset
```

### Individual Jobs

You can also run individual jobs:

* `/api/v1/dev/jobs/late-charges`
* `/api/v1/dev/jobs/defaults`
* `/api/v1/dev/jobs/ach-returns`
* `/api/v1/dev/jobs/disbursements`

These `/dev/**` endpoints are available **only when the application is running with the `local` profile**.


### Try it: NSF flow

```bash
# 1. Maria login
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/get_auth_token -H "Content-Type: application/json" \
  -d '{"username":"maria@loan.local","password":"password"}' | jq -r .token)

# 2. Get her loans, bank accounts, and amount due
curl -s localhost:8080/api/v1/loans -H "jwt: $TOKEN" | jq '.[0] | {id, loanNumber, nextDueDate}'
curl -s localhost:8080/api/v1/borrowers/me/bank-accounts -H "jwt: $TOKEN"
curl -s localhost:8080/api/v1/loans/2/amount-due -H "jwt: $TOKEN"

# 3. Make the payment (amount = totalDue from step 2)
curl -s -X POST localhost:8080/api/v1/payments -H "jwt: $TOKEN" \
  -H "Content-Type: application/json" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"loanId":2,"bankAccountId":2,"amount":<totalDue>}'

# 4. The bank "returns" the payment → payment is reversed + NSF fee is charged
#    A notification/receipt will also appear in the console
curl -X POST localhost:8080/api/v1/dev/jobs/ach-returns
curl -s localhost:8080/api/v1/loans/2 -H "jwt: $TOKEN" | jq '{principalBalance, unpaidCharges, charges}'
```

> **Note:** The IDs may be different depending on your local data. Use the actual `loanId` and `bankAccountId` values obtained in Step 2.


---

## 7. API list

| Method | URL | Who | React screen |
|---|---|---|---|
| POST | /api/v1/auth/get_auth_token | anyone | Login (LoanLinq `urls.admin.getAuthToken`) |
| POST | /api/v1/auth/access_account | logged in | roles + accounts after login |
| GET | /api/v1/auth/me | logged in | who am I |
| POST | /api/v1/lenders | ADMIN | Lender onboarding |
| GET | /api/v1/lenders, /{id} | ADMIN, CSR | Lender management |
| GET | /api/v1/lenders/me | LENDER | Profile |
| POST | /api/v1/borrowers | ADMIN | Borrower information |
| GET | /api/v1/borrowers, /{id} | ADMIN, CSR | Borrower directory |
| GET | /api/v1/borrowers/me | BORROWER | Profile |
| GET, POST | /api/v1/borrowers/me/bank-accounts | BORROWER | Add bank account |
| DELETE | /api/v1/borrowers/me/bank-accounts/{id} | BORROWER | Delete account |
| POST | /api/v1/loans | ADMIN | Loan onboarding (all tabs) |
| GET | /api/v1/loans | everyone (filtered by role) | Portfolio / My loans |
| GET | /api/v1/loans/{id} | owner, funding lender, staff | Loan details |
| GET | /api/v1/loans/{id}/amount-due | same | Make a payment, payoff |
| POST | /api/v1/loans/{id}/properties, /insurances | ADMIN | Property / Insurance tabs |
| POST | /api/v1/loans/{id}/charges | ADMIN, CSR | Charges tab |
| POST | /api/v1/loans/{id}/charges/{chargeId}/waive | ADMIN | Waive charge |
| POST | /api/v1/payments | BORROWER | Make a payment (ACH) |
| GET | /api/v1/payments?loanId= | owner, lender, staff | Payment activity / loan history |
| GET | /api/v1/payments/{id} | same | Transaction details |
| GET | /api/v1/disbursements | LENDER (own), staff | Past payments to lender |
| GET | /api/v1/nsf-cases | ADMIN, CSR | NSF cases |

Errors always follow this format:

```json
{
  "status": 422,
  "error": "...",
  "message": "Minimum payment is 3485.84",
  "fieldErrors": {}
}
```


---

## 8. React front-end connection

1. In `src/Utils/urls.js`, configure the base URLs to point to:

   `http://localhost:8080/api/v1`

2. In `deployment-config`, keep `serverType` as **development** so that Axios base64 URL encryption remains disabled.

3. **JWT Authentication — Same as LoanLinq; no React code changes are required:**

   * `urls.admin.getAuthToken` → `http://localhost:8080/api/v1/auth/get_auth_token`

     * Request body: `{username, password}`
   * Response contains:

     * `token`
     * `userid`
     * `axiosdata.is_trusted` = `true`
     * Because `is_trusted` is `true`, the OTP screen is skipped.
   * React saves the authentication information in:

     * `AUTH-TOKEN`
     * `apex_userid`
   * `access_account.endpoint` → `http://localhost:8080/api/v1/auth/access_account`

     * Request body: `{userid}`
     * Response provides:

       * `role[].role_name`
       * `account[]`
   * The Axios interceptor sends the `jwt` and `user` headers with API requests.
   * The backend reads and validates these headers.
   * The JWT token is valid for **24 hours**, and React also expires the session after 24 hours.

4. **CORS Configuration:**

   The following frontend origins are allowed:

   * `http://localhost:3000`
   * `http://localhost:5173`

   This configuration is defined in `application.properties` using:

   `app.cors.allowed-origins`

5. **Payment Idempotency:**

   When making a payment, send an `Idempotency-Key` header using:

   `crypto.randomUUID()`

   This is important because Axios retries the request up to **3 times**. The idempotency key prevents the same payment from being processed multiple times if a retry occurs.


---

## 9. Learning Simplifications (Different in Production)

For learning purposes, this project uses some simplified assumptions. In a real production system, these would be implemented differently.

* **Payment must be at least one full installment.**
  Partial payments and **suspense accounts** are not supported.

* **Interest is calculated monthly.**
  The calculation uses **note rate / 12** instead of daily interest accrual.

* **Bank account numbers and TIN are stored as plain text.**
  In a production system, these sensitive values should be **encrypted**.

* **Database tables are created using Hibernate `ddl-auto`.**
  In production, database migrations should be managed using **Flyway or Liquibase**.

* **Payoff amount** is simplified as:

  `Payoff = Principal + Current Interest + Unpaid Charges`

