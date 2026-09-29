# Loan Servicing Management

Spring Boot + Hibernate + PostgreSQL backend for a loan servicing platform (React front-end).

**New here? Start with [docs/SETUP.md](docs/SETUP.md)** - laptop setup from scratch, GitHub, and a 10-minute demo script.

Loan servicing app चा backend, तुमच्या React front-end साठी. सगळं **local** वर चालतं: कोणताही खरा bank transaction होत नाही (ACH fake आहे, emails console मध्ये print होतात).

Complete architecture आणि flows (diagrams सहित): **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

Project **modular monolith** आहे: एकच app, पण modules मध्ये विभागलेलं, म्हणजे पुढच्या महिन्यात प्रत्येक module वेगळी **microservice** करता येईल. त्यासाठी [docs/MICROSERVICES.md](docs/MICROSERVICES.md) पहा.

---

## 1. Requirements

- **JDK 21** (किंवा नवीन)
- **Maven 3.9+** (IntelliJ मध्ये built-in असतो)
- **PostgreSQL 14+** (pgAdmin सोबत)
- IntelliJ IDEA, Postman (optional)

## 2. Run कसं करायचं

PostgreSQL install झाल्यावर pgAdmin मध्ये एकदाच database तयार करा:

```sql
CREATE DATABASE loan_servicing;
```

मग app चालवा:

```bash
mvn spring-boot:run
```

सगळी configuration एकाच file मध्ये आहे: `src/main/resources/application.properties`. Default DB user/password `postgres` / `root` आहे; तुमचा वेगळा असेल तर `spring.datasource.username` आणि `spring.datasource.password` बदला.

Hibernate आपोआप **5 schemas** (`auth`, `lender`, `borrower`, `loan`, `payment`) आणि सगळे tables तयार करतो (`ddl-auto: update`). प्रत्येक module चे tables त्याच्या स्वतःच्या schema मध्ये आहेत, म्हणजे पुढे प्रत्येक service ला वेगळा database देणं सोपं.

pgAdmin / DBeaver मध्ये data पाहण्यासाठी काही queries:

```sql
SELECT id, loan_number, status, principal_balance, next_due_date FROM loan.loans;
SELECT id, loan_id, amount, status, interest_paid, principal_paid FROM payment.payments ORDER BY id DESC;
SELECT lender_id, principal_amount, interest_amount, status FROM payment.lender_disbursements;
SELECT loan_id, type, balance, status FROM loan.loan_charges;
```

### Start झाल्यावर

- API: `http://localhost:8080/api/v1/...`
- **Swagger UI:** http://localhost:8080/swagger-ui.html - सगळे APIs browser मधून try करा. आधी `POST /api/v1/auth/get_auth_token` ने login करा, `token` copy करा, वर **Authorize** button मध्ये paste करा (Swagger तो `jwt` header मध्ये पाठवतो, LoanLinq सारखं).
- Console मध्ये sample users दिसतील. सगळ्यांचा password: `password`

| Email | Role | काय आहे |
|---|---|---|
| admin@loan.local | ADMIN | सगळं onboard करू शकतो, सगळं पाहू शकतो |
| csr@loan.local | CSR | customer service: सगळं पाहू शकतो |
| lender1@loan.local | LENDER | Loan 1 (60%) आणि Loan 2 fund केले |
| lender2@loan.local | LENDER | Loan 1 (40%) आणि Loan 3 fund केले |
| john@loan.local | BORROWER | Loan 1 - current (हप्ता 10 दिवसांनी) |
| maria@loan.local | BORROWER | Loan 2 - 15 दिवस late; तिचं bank account `...0000` ने संपतं, म्हणून payment **bounce** होतं |
| david@loan.local | BORROWER | Loan 3 - 40 दिवस late → default |

## 3. Tests

```bash
mvn test
```

- `PaymentAllocatorTest`, `DistributionCalculatorTest` - पैशांचं गणित (EMI, हप्त्याची विभागणी, lenders चा हिस्सा)
- `LoanServicingFlowTest` - पूर्ण flow REST API मधून: payment, lender split, idempotency, NSF, late fee, default, access control
- Tests स्वतःचा in-memory H2 database वापरतात (settings `LoanServicingFlowTest` मध्येच आहेत), म्हणजे तुमच्या PostgreSQL data ला हात लावत नाहीत.
- `ModularityTests` - **module boundaries** तपासतो (microservices साठी सगळ्यात महत्त्वाचा test). `target/spring-modulith-docs` मध्ये module diagrams पण तयार होतात.

---

## 4. Project structure

```
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

प्रत्येक module मध्ये:

```
loan/
 ├── LoanApi.java, LoanDto.java, ...   ← PUBLIC API: इतर modules फक्त हेच वापरू शकतात
 └── internal/                          ← PRIVATE: entity, repository, service, controller
      ├── Loan.java (@Entity)
      ├── LoanRepository.java (Spring Data JPA)
      ├── LoanService.java (business logic, implements LoanApi)
      └── LoanController.java (REST)
```

### Microservice-ready होण्यासाठी 4 नियम

1. **एक module दुसऱ्याचे `internal` classes वापरत नाही**, फक्त त्याचा `XxxApi` interface. (`ModularityTests` हे check करतो.)
2. **Modules मध्ये JPA relationship नाही.** `Loan` मध्ये `Long borrowerId` आहे, `Borrower borrower` नाही. म्हणून उद्या loan आणि borrower वेगळ्या databases मध्ये असू शकतात.
3. **Notifications events ने होतात**, direct call ने नाही (`PaymentPostedEvent`, `PaymentReturnedEvent`, `LoanDefaultedEvent`). उद्या हेच events Kafka वर जातील.
4. **URLs module-wise आहेत** (`/api/v1/loans`, `/api/v1/payments`...), म्हणजे API Gateway ला route करणं सोपं.

---

## 5. Payment flow (main business logic)

`POST /api/v1/payments` → `PaymentService.makePayment()`:

1. Borrower चा loan आणि bank account आहे का ते check (ownership)
2. `LoanApi.applyPayment()` - पैसे या क्रमाने वापरले जातात (**waterfall**):
   **charges → interest → principal → reserve/impound (escrow) → extra principal**
3. `AchGateway.debit()` - bank कडून पैसे (local मध्ये `FakeAchGateway`)
4. Payment आणि त्याचा breakdown save
5. `DistributionService` - प्रत्येक lender चा हिस्सा (funding share नुसार) `lender_disbursements` मध्ये
6. `PaymentPostedEvent` → notification module receipt "पाठवतो" (console)

हे सगळं **एका transaction** मध्ये: step 3 fail झाली तर step 2 पण rollback होते.

**Lender चा हिस्सा:** Loan 12% ने, lender 10% ने funded, 60% share:
- principal × 60%
- interest × 60% × (10/12)
- उरलेलं interest (2% spread) = servicer ची fee

**NSF (bounce):** `processAchReturns` job → `NsfService`: payment `RETURNED`, loan reverse, lender चा हिस्सा cancel/clawback, NSF fee ($25) charge, NSF case, `PaymentReturnedEvent`.

---

## 6. Local testing: "time travel"

Late fee आणि default तारखांवर अवलंबून आहेत. 30 दिवस थांबायची गरज नाही:

```bash
# आजची app date
curl http://localhost:8080/api/v1/dev/clock

# app ला 40 दिवस पुढे न्या
curl -X POST "http://localhost:8080/api/v1/dev/clock/advance?days=40"

# सगळे nightly jobs आत्ताच चालवा
curl -X POST http://localhost:8080/api/v1/dev/jobs/run-all

# परत आजच्या दिवशी
curl -X POST http://localhost:8080/api/v1/dev/clock/reset
```

Individual jobs: `/api/v1/dev/jobs/late-charges`, `/defaults`, `/ach-returns`, `/disbursements`.
हे `/dev/**` endpoints फक्त `local` profile मध्ये असतात.

### Try it: NSF flow

```bash
# 1. Maria login
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/get_auth_token -H "Content-Type: application/json" \
  -d '{"username":"maria@loan.local","password":"password"}' | jq -r .token)

# 2. तिचे loans, bank account आणि amount due
curl -s localhost:8080/api/v1/loans -H "jwt: $TOKEN" | jq '.[0] | {id, loanNumber, nextDueDate}'
curl -s localhost:8080/api/v1/borrowers/me/bank-accounts -H "jwt: $TOKEN"
curl -s localhost:8080/api/v1/loans/2/amount-due -H "jwt: $TOKEN"

# 3. Pay (amount = totalDue from step 2)
curl -s -X POST localhost:8080/api/v1/payments -H "jwt: $TOKEN" \
  -H "Content-Type: application/json" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"loanId":2,"bankAccountId":2,"amount":<totalDue>}'

# 4. Bank "returns" it → reversed + NSF fee (console मध्ये email पण दिसेल)
curl -X POST localhost:8080/api/v1/dev/jobs/ach-returns
curl -s localhost:8080/api/v1/loans/2 -H "jwt: $TOKEN" | jq '{principalBalance, unpaidCharges, charges}'
```

(IDs तुमच्या data नुसार वेगळे असू शकतात - step 2 मधून घ्या.)

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

Errors नेहमी या format मध्ये येतात: `{ "status": 422, "error": "...", "message": "Minimum payment is 3485.84", "fieldErrors": {...} }`

---

## 8. React front-end जोडणं

1. `src/Utils/urls.js` मधले base URLs `http://localhost:8080/api/v1` ला point करा.
2. `deployment-config` मध्ये `serverType` development ठेवा, म्हणजे axios चं base64 URL encryption बंद राहतं.
3. **JWT - LoanLinq सारखंच, React code बदलायची गरज नाही:**
   - `urls.admin.getAuthToken` → `http://localhost:8080/api/v1/auth/get_auth_token` (body `{username, password}`)
   - Response: `token`, `userid`, `axiosdata.is_trusted` (= true, म्हणून OTP screen skip होतो) → React `AUTH-TOKEN` आणि `apex_userid` मध्ये save करतो
   - `access_account.endpoint` → `http://localhost:8080/api/v1/auth/access_account` (body `{userid}`) → `role[].role_name`, `account[]`
   - axios interceptor जे `jwt` आणि `user` headers पाठवतो ते backend वाचतो
   - Token 24 तास valid, React पण 24 तासांनी session संपवतो
4. CORS: `http://localhost:3000` आणि `5173` allowed आहेत (`application.properties` → `app.cors.allowed-origins`).
5. Payment करताना `Idempotency-Key` header पाठवा (`crypto.randomUUID()`), कारण तुमचं axios 3 वेळा retry करतं.

---

## 9. Learning साठी simplifications (production मध्ये वेगळं)

- Payment किमान एक पूर्ण हप्ता असावा लागतो (partial payments / "suspense" नाहीत)
- Interest monthly (note rate / 12) मोजलं आहे, daily accrual नाही
- Bank account numbers आणि TIN plain text मध्ये (production मध्ये encrypt करा)
- Tables Hibernate `ddl-auto` ने बनतात (production मध्ये Flyway/Liquibase)
- Payoff = principal + current interest + unpaid charges
