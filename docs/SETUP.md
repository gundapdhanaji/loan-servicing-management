# Setup from scratch, GitHub, and demo

हा guide Windows laptop साठी लिहिला आहे (Mac साठी वेगळं असेल तिथे 🍎 note आहे). सुरुवातीपासून शेवटपर्यंत साधारण 45-60 मिनिटं लागतील, बहुतेक वेळ downloads मध्ये.

---

## Part 1 - Tools install करा (एकदाच)

| # | Tool | Download | Install करताना |
|---|---|---|---|
| 1 | **Git** | https://git-scm.com/downloads | सगळे defaults ठेवा |
| 2 | **JDK 21** (Eclipse Temurin) | https://adoptium.net → Temurin 21 LTS | ✅ "Set JAVA_HOME variable" आणि ✅ "Add to PATH" select करा |
| 3 | **Maven** | https://maven.apache.org/download.cgi → Binary zip | खाली steps पहा |
| 4 | **IntelliJ IDEA Community** | https://www.jetbrains.com/idea/download | defaults |
| 5 | **PostgreSQL 16** | https://www.postgresql.org/download/windows (EDB installer) | password **`root`** ठेवा, port **5432**, pgAdmin ✅ |
| 6 | Postman (optional) | https://www.postman.com/downloads | - |


**Maven setup (Windows):**
1. Zip extract करा, उदा. `C:\tools\apache-maven-3.9.x`
2. Start → "Edit the system environment variables" → Environment Variables → System variables → `Path` → Edit → New → `C:\tools\apache-maven-3.9.x\bin`
3. OK, OK, आणि **नवीन** Command Prompt उघडा

🍎 Mac: `brew install git temurin@21 maven postgresql@16` आणि `brew services start postgresql@16`

**Check करा** (नवीन terminal मध्ये):

```bash
git --version        # git version 2.x
java -version        # openjdk version "21..."
mvn -v               # Apache Maven 3.9.x ... Java version: 21
```

`java -version` 21 नसेल तर JAVA_HOME जुन्या Java कडे point करत आहे. Environment Variables मध्ये `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-21...` करा.

---

## Part 2 - Project उघडा

1. Zip extract करा, उदा. `D:\projects\loan-servicing-management`
   (path मध्ये spaces आणि मराठी अक्षरं टाळा)
2. IntelliJ → **Open** → त्या folder मधली **`pom.xml`** select करा → **Open as Project** → **Trust Project**
3. Maven dependencies download होतील (खाली progress bar, पहिल्यांदा 3-5 मिनिटं)
4. **File → Project Structure → Project → SDK = 21** (नसेल तर "Add SDK → JDK" आणि Temurin 21 folder निवडा)
5. Lombok: IntelliJ मध्ये plugin आधीच असतो. "Enable annotation processing?" popup आला तर **Enable** करा
   (किंवा Settings → Build → Compiler → Annotation Processors → ✅ Enable)

---

## Part 3 - Database

1. **pgAdmin** उघडा → Servers → PostgreSQL 16 → password `root`
2. Databases वर right-click → **Create → Database** → नाव **`loan_servicing`** → Save

(Install करताना वेगळा password ठेवला असेल तर `src/main/resources/application.properties` मध्ये `spring.datasource.password` बदला. सगळी configuration याच एका file मध्ये आहे.)

Tables तुम्ही बनवायचे नाहीत. App पहिल्यांदा start होताना Hibernate 5 schemas (`auth`, `lender`, `borrower`, `loan`, `payment`) आणि सगळे tables बनवतो.

---

## Part 4 - Run

**IntelliJ मधून:** `src/main/java/com/loanservicing/LoanServicingApplication.java` उघडा → `main` शेजारचं ▶ green button → **Run**

**किंवा terminal मधून:**

```bash
mvn spring-boot:run
```

Console मध्ये हे दिसलं की app ready आहे:

```
================= SAMPLE DATA CREATED (password for everyone: "password") =================
admin@loan.local (ADMIN)   csr@loan.local (CSR)
...
Started LoanServicingApplication in 6.2 seconds
```

Browser: **http://localhost:8080/swagger-ui.html**

**Tests:**

```bash
mvn test
```

शेवटी `BUILD SUCCESS` आणि `Tests run: ..., Failures: 0, Errors: 0` दिसलं पाहिजे.

---

## Part 5 - GitHub वर टाका

### 5.1 Repository बनवा

1. https://github.com → login → वर उजवीकडे **+** → **New repository**
2. Name: **`loan-servicing-management`**
3. Description: `Loan Servicing Management - Spring Boot, Hibernate, PostgreSQL, modular monolith`
4. Visibility: **Private** (recommended - खाली "Boss ला access" पहा)
5. ❌ README, .gitignore, license **add करू नका** (project मध्ये आधीच आहेत)
6. **Create repository**

### 5.2 Code push करा

Project folder मध्ये terminal (IntelliJ मध्ये खाली **Terminal** tab):

```bash
git init
git config user.name "Your Name"
git config user.email "your-github-email@example.com"

git add .
git status                      # target/ आणि .idea/ दिसू नयेत (.gitignore मुळे)
git commit -m "Loan Servicing Management: initial backend"

git branch -M main
git remote add origin https://github.com/<your-username>/loan-servicing-management.git
git push -u origin main
```

**Password विचारला तर:** GitHub account password चालत नाही. Browser popup (Git Credential Manager) आला तर त्यातून login करा. Popup नाही आला तर:
GitHub → Settings → Developer settings → **Personal access tokens → Tokens (classic)** → Generate new token → scope ✅ `repo` → token copy करा → password च्या जागी paste करा.

### 5.3 पुढचे बदल push करणं

```bash
git add .
git commit -m "Add draw requests module"
git push
```

### 5.4 Boss ला access

- **Private repo (recommended):** repo → **Settings → Collaborators → Add people** → boss चा GitHub username/email → ते invite accept करतील.
- Public करायचं असेल तर: Settings → Danger Zone → Change visibility.

GitHub वर `docs/ARCHITECTURE.md` उघडलं की सगळे diagrams आपोआप चित्र म्हणून दिसतात - demo साठी उत्तम.

### ⚠️ GitHub वर काय टाकू नये

- **Company चा code** (तुमचा React front-end company project असेल तर तो या repo मध्ये टाकू नका, आणि public तर नक्कीच नाही)
- खरे passwords, API keys, company URLs. या project मधले secrets फक्त local dev values आहेत, ते ठीक आहेत.

---

## Part 6 - Boss साठी 10-minute demo script

Demo आधी: app सुरू करा, Swagger आणि pgAdmin tabs उघडून ठेवा. (Demo पुन्हा fresh हवा असेल तर app बंद करून pgAdmin मध्ये `loan_servicing` database drop करा आणि पुन्हा बनवा - start होताना sample data परत येतो.)

**1. Architecture (2 min)** - GitHub वर `docs/ARCHITECTURE.md`
- System context आणि modules diagram: "एक app, पण business modules वेगळे. प्रत्येक पुढे microservice होऊ शकतो."
- Database: "प्रत्येक module चा स्वतःचा PostgreSQL schema."

**2. Login आणि security (1 min)** - Swagger
- `POST /api/v1/auth/get_auth_token` → `{"username":"admin@loan.local","password":"password"}` → `token` copy → वर **Authorize** → paste ("LoanLinq सारखंच: token `jwt` header मध्ये जातो")
- `POST /api/v1/auth/access_account` → role आणि accounts (LoanLinq login चा दुसरा step)
- `GET /api/v1/loans` → 3 loans
- Borrower (`john@loan.local`) म्हणून login केलं तर फक्त त्याचं 1 loan दिसतं: "role आणि ownership दोन्ही checks"

**3. Payment (2 min)** - john म्हणून
- `GET /api/v1/borrowers/me/bank-accounts` → bank account id
- `GET /api/v1/loans/{id}/amount-due` → `totalDue` (उदा. 3485.84): interest, principal, escrow breakdown दाखवा
- `POST /api/v1/payments` → `{"loanId":1,"bankAccountId":1,"amount":3485.84}`
- Response मधला breakdown दाखवा. Console मध्ये "FAKE ACH debit" आणि receipt "EMAIL" दाखवा.
- `GET /api/v1/loans/1` → principal कमी, next due date एक महिना पुढे

**4. Lenders ना पैसे (1 min)** - `lender1@loan.local` म्हणून
- `GET /api/v1/disbursements` → 60% share, lender rate नुसार interest
- "Loan 12%, lender 10%, 2% servicer fee"

**5. Bounce / NSF (2 min)** - `maria@loan.local` म्हणून
- तिचं amount due पाहून payment करा (तिचा account `...0000` → bounce होईल)
- `POST /api/v1/dev/jobs/ach-returns`
- `GET /api/v1/loans/2` → payment reverse झालं, NSF fee $25 charge. Console मध्ये "payment returned" email.

**6. Late fee आणि default - time travel (1 min)**
- `POST /api/v1/dev/clock/advance?days=20` → `POST /api/v1/dev/jobs/run-all`
- Admin म्हणून `GET /api/v1/loans` → late fees आणि `DEFAULT` status

**7. Quality (1 min)**
- pgAdmin: `SELECT * FROM payment.payments;` आणि `SELECT * FROM loan.loans;`
- `mvn test` → सगळे tests pass, `ModularityTests` module boundaries enforce करतो
- `docs/MICROSERVICES.md` → "पुढचा plan: Kafka, Feign, Saga"

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `mvn` is not recognized | Maven `bin` Path मध्ये नाही, किंवा जुनं terminal. Part 1 Maven setup पुन्हा पहा, नवीन terminal उघडा |
| `release version 21 not supported` | Maven जुनं Java वापरतो. `mvn -v` मध्ये Java version पहा, `JAVA_HOME` JDK 21 करा |
| `password authentication failed for user "postgres"` | `application.properties` मधला password तुमच्या PostgreSQL password शी जुळवा |
| `database "loan_servicing" does not exist` | Part 3 मध्ये database बनवा |
| `Connection refused ... 5432` | PostgreSQL चालू नाही: Windows Services मध्ये "postgresql-x64-16" Start करा |
| `Port 8080 was already in use` | दुसरं app बंद करा, किंवा `application.properties` मध्ये `server.port=8081` |
| IntelliJ मध्ये getters/setters लाल (cannot find symbol `getId`) | Annotation processing enable करा (Part 2 step 5), मग Build → Rebuild Project |
| Sample users दिसत नाहीत | Sample data फक्त `spring.profiles.active=local` असताना आणि database रिकामा असेल तेव्हाच बनतो |
| Data पूर्ण reset करायचा | pgAdmin मध्ये `loan_servicing` database drop करून पुन्हा बनवा (app बंद असताना) |
| `git push` rejected | GitHub repo बनवताना README add केलं असेल: `git pull origin main --allow-unrelated-histories` मग `git push` |
