# Loan Servicing Management - Front-end

React 18 + Vite front-end for the Spring Boot backend in `../backend`.

## Run it

You need **Node.js 18 or newer** (check with `node -v`). The LTS version (Node 22) is the safest choice. You also need the backend running on port 8080.

```bash
cd frontend
npm install        # first time only
npm run dev
```

Open **http://localhost:5173** and sign in with one of the demo accounts shown on the login page. They are created by the backend on first start, and every password is `password`.

Vite forwards every `/api/...` call to `http://localhost:8080` (see `vite.config.js`), so there's no CORS setup while developing.

For a production build, run `npm run build`. The output goes to `dist/`. If the front-end is hosted on a different address than the API, copy `.env.example` to `.env` and set `VITE_API_BASE_URL`.

## Why `package.json` has "overrides"

Vite normally uses native programs (Rollup's `.node` file and esbuild's `.exe`). On Windows machines with **Smart App Control** or a company **Application Control** policy, these get blocked, and you see:

```
Error: Cannot find module @rollup/rollup-win32-x64-msvc ...
[cause]: Error: An Application Control policy has blocked this file.
```

The `overrides` in `package.json` swap them for their WebAssembly versions (`@rollup/wasm-node` and `esbuild-wasm`). These contain no native binaries, so nothing gets blocked. The only cost is slightly slower builds.

If you changed `package.json`, always reinstall from scratch:

```bash
# Windows PowerShell
Remove-Item -Recurse -Force node_modules, package-lock.json
npm install
```

## What each role sees

| Role | Screens |
|---|---|
| Admin | Dashboard, loans, onboard a loan, borrowers, lenders, lender payouts, returned payments (NSF), dev tools |
| Customer service (CSR) | Same as admin, but can't onboard loans or waive charges |
| Lender | Dashboard with their share of each loan, their portfolio, their payouts |
| Borrower | Dashboard with amount due, their loans, make a payment, bank accounts |

## Sign-in works like LoanLinq

| Step | What happens |
|---|---|
| 1 | `POST /api/v1/auth/get_auth_token` with `{username, password}` returns `token`, `userid` and `axiosdata.is_trusted` |
| 2 | `POST /api/v1/auth/access_account` with `{userid}` returns the roles (`role[].role_name`) and accounts (`account[]`) |
| 3 | Saved in localStorage under the same keys: `AUTH-TOKEN` (with `expiration: Date.now()`), `apex_userid`, `email_for_forgot_password`, `accessed_user` |
| 4 | Every request carries the `jwt` header (token) and the `user` header (`{"email","userId"}`) |
| 5 | After 24 hours, or on any 401, the session is cleared and you go back to the login page |

The code for this is in `src/api/client.js` (axios interceptors), `src/auth/session.js` and `src/auth/AuthContext.jsx`.

## Project structure

```
src/
  api/
    endpoints.js        every backend URL in one place (like urls.js in LoanLinq)
    client.js           axios instance: jwt + user headers, 24h expiry, 401 handling, error messages
  auth/
    session.js          localStorage keys (AUTH-TOKEN, apex_userid...)
    AuthContext.jsx     login (get_auth_token + access_account), logout, current role
    RequireAuth.jsx     protects routes, optionally by role
  hooks/
    useApi.js           GET + loading/error state + reload
    useDirectory.js     borrower/lender names for ids (loans only carry ids)
  components/
    Layout.jsx          sidebar menu per role
    ui.jsx              badges, tabs, modal, form field, stats, empty/error states
  pages/                one file per screen
  utils/format.js       money, dates, labels, idempotency key
  styles.css            all styles (colour tokens at the top)
```

## Things worth showing in a demo

- **Make a payment** (John): the right side shows the payment waterfall (fees, interest, principal, escrow). Every payment sends an `Idempotency-Key` header, so double-clicking "Pay" never charges twice.
- **Loan details**: the statement header shows the balance and how much principal has been repaid. The tabs cover terms, property, funding (each lender's share and the servicer's spread), insurance, charges and payments.
- **Bounced payment** (Maria, whose account ends in 0000): pay, then as admin go to **Dev tools → Process bank returns**. The payment shows as Returned, the NSF fee appears under Charges, and the case is listed under **Returned payments**.
- **Late fees and default**: in Dev tools, move the date forward 20 days and run all nightly jobs. The admin dashboard then shows the late fees and the defaulted loan.
