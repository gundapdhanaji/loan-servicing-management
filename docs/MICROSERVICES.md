# Microservices migration guide (पुढच्या महिन्यासाठी)

हा project आत्ता **modular monolith** आहे. खाली दिलंय की प्रत्येक module microservice कसा बनेल आणि code मध्ये नक्की काय बदलायचं.

## 1. Target architecture

```
React (localhost:3000)
   │
   ▼
API Gateway (Spring Cloud Gateway, :8080)  ← JWT check, routing by URL prefix
   │
   ├── /api/v1/auth/**                  → auth-service          (DB: auth_db)
   ├── /api/v1/lenders/**               → lender-service        (DB: lender_db)
   ├── /api/v1/borrowers/**             → borrower-service      (DB: borrower_db)
   ├── /api/v1/loans/**                 → loan-service          (DB: loan_db)
   └── /api/v1/payments/**,
       /api/v1/disbursements/**,
       /api/v1/nsf-cases/**             → payment-service       (DB: payment_db)

notification-service  ← Kafka वरून events ऐकतो (no REST API)

Supporting: Eureka (service discovery), Config Server, Kafka, Zipkin (tracing)
```

## 2. Module → service mapping

| Module (today) | Service | Owns tables | Calls (sync) | Publishes / consumes (async) |
|---|---|---|---|---|
| auth | auth-service | users | - | - |
| lender | lender-service | lenders | auth | - |
| borrower | borrower-service | borrowers, borrower_bank_accounts | auth | - |
| loan | loan-service | loans, loan_properties, loan_fundings, loan_insurances, loan_charges | borrower, lender | publishes `loan.defaulted` |
| payment | payment-service | payments, payment_charge_lines, lender_disbursements, nsf_cases | loan, borrower, lender | publishes `payment.posted`, `payment.returned` |
| notification | notification-service | (none) | borrower | consumes all three topics |
| profile | API gateway / BFF (`access_account`) | (none) | auth, borrower, lender | - |
| common | shared library jar (`loan-common`) | - | - | - |
| devtools | stays local-only / removed | - | - | - |

हे mapping आत्ताच code मध्ये लागू आहे:
- modules मध्ये JPA relationships नाहीत, फक्त IDs
- प्रत्येक module चे tables त्याच्या **स्वतःच्या PostgreSQL schema** मध्ये आहेत (`@Table(schema = "loan")`)

म्हणून split करताना: `pg_dump --schema=loan loan_servicing` ने loan schema काढा, `loan_db` मध्ये restore करा, आणि loan-service चा `datasource.url` बदला. Entities मध्ये काहीच बदल नाही.

## 3. Step by step

### Step 1 - common ला library बनवा
`com.loanservicing.common` (BaseEntity, Money, ApiError, exceptions, BankDetails, AppClock) एका वेगळ्या Maven module / jar मध्ये टाका. प्रत्येक service ते dependency म्हणून घेईल.

प्रत्येक module चे **public API types** (`LoanApi`, `LoanDto`, events...) पण एका `xxx-api` jar मध्ये टाकता येतात, म्हणजे caller आणि service दोघे same DTOs वापरतात.

### Step 2 - Sync calls: `XxxApi` interface → HTTP client
आज: `PaymentService` मध्ये `LoanApi` inject होतो आणि Spring `LoanService` देतो (same JVM).

उद्या payment-service मध्ये त्याच interface चं **OpenFeign client** implementation:

```java
@FeignClient(name = "loan-service", path = "/internal/v1/loans")
public interface LoanClient extends LoanApi {
    // Feign annotations (@GetMapping, @PostMapping) on each method
}
```

loan-service मध्ये त्या internal endpoints साठी एक छोटा controller (`/internal/v1/loans/{id}/apply-payment`...) जो `LoanService` ला call करतो. `PaymentService` चा business code **बदलत नाही**, फक्त inject होणारा bean बदलतो. हाच फायदा आहे interfaces चा.

### Step 3 - Events: Spring events → Kafka
आज: `events.publishEvent(new PaymentPostedEvent(...))` आणि `NotificationListener` मध्ये `@TransactionalEventListener`.

उद्या:
- payment-service: `kafkaTemplate.send("payment.posted", event)` (**Outbox pattern** वापरा, म्हणजे DB commit आणि message एकत्र: Spring Modulith चा `spring-modulith-events-kafka` किंवा Debezium)
- notification-service: `@KafkaListener(topics = "payment.posted")` → same method body

Event records (`PaymentPostedEvent` वगैरे) आधीच plain records आहेत, JSON मध्ये सहज serialize होतात.

### Step 4 - Distributed transaction: Saga
आज `makePayment()` एका DB transaction मध्ये आहे: loan update + ACH + payment save. Services वेगळ्या झाल्या की एक transaction शक्य नाही. त्यासाठी **Saga (orchestration)**:

```
payment-service                         loan-service
 1. Payment row: PENDING
 2. POST apply-payment ───────────────▶ apply, return PaymentApplication
 3. ACH debit
    ├── ok   → Payment POSTED, publish payment.posted
    └── fail → POST reverse-payment ──▶ reverse   (compensating action)
               Payment FAILED
```

`reversePayment()` आधीच आहे (NSF साठी बनवलं), तेच compensation म्हणून वापरता येईल. Retries safe ठेवण्यासाठी loan-service ला पण idempotency key पाठवा (payment id).

### Step 5 - Security
- auth-service JWT देतो (आजच्यासारखं).
- Gateway प्रत्येक request वर JWT validate करतो.
- प्रत्येक service पण `JwtAuthFilter` ठेवते (common library मधून), म्हणजे `CurrentUser.get()` आणि `@PreAuthorize` तसेच चालतात.
- नंतर shared secret ऐवजी RS256 (public/private key) वापरा, म्हणजे services ना फक्त public key लागते.

### Step 6 - Jobs
प्रत्येक service स्वतःचे `@Scheduled` jobs चालवते (loan-service: late fees, defaults; payment-service: ACH returns, disbursements). एकाच service चे 2 instances असतील तर job दोनदा चालू नये म्हणून **ShedLock** वापरा.

### Step 7 - Reports / dashboards
Lender dashboard ला loans + payments दोन्ही लागतात. Services मध्ये join करू नका. पर्याय:
- Gateway / BFF मध्ये दोन calls करून combine करणे (सोपं), किंवा
- `reporting-service` जो Kafka events ऐकून स्वतःचा read-only DB (CQRS) बनवतो.

## 4. Suggested order

1. common library + Config Server + Eureka + Gateway (अजून monolith मागे)
2. **notification-service** सगळ्यात आधी काढा: फक्त events ऐकतो, सगळ्यात सोपं, आणि Kafka शिकायला मिळतं
3. auth-service
4. lender-service, borrower-service
5. loan-service आणि payment-service सगळ्यात शेवटी (Saga लागते)

प्रत्येक step नंतर `LoanServicingFlowTest` सारखे end-to-end tests चालवा.

## 5. Checklist - boundary नियम पाळले जात आहेत का?

`mvn test` मध्ये `ModularityTests` pass झाला तर:
- [x] कोणताही module दुसऱ्याचे `internal` classes वापरत नाही
- [x] Modules मध्ये circular dependency नाही

Manually लक्षात ठेवायचं:
- [ ] नवीन entity मध्ये दुसऱ्या module च्या entity चा `@ManyToOne` नको, फक्त `Long xxxId`
- [ ] दुसऱ्या module च्या table वर `@Query` join नको
- [ ] Cross-module communication फक्त `XxxApi` किंवा events ने
