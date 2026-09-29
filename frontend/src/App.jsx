import { Route, Routes } from "react-router-dom";
import RequireAuth from "./auth/RequireAuth";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import LoansPage from "./pages/LoansPage";
import LoanDetailsPage from "./pages/LoanDetailsPage";
import LoanOnboardingPage from "./pages/LoanOnboardingPage";
import MakePaymentPage from "./pages/MakePaymentPage";
import BankAccountsPage from "./pages/BankAccountsPage";
import BorrowersPage from "./pages/BorrowersPage";
import LendersPage from "./pages/LendersPage";
import DisbursementsPage from "./pages/DisbursementsPage";
import NsfCasesPage from "./pages/NsfCasesPage";
import DevToolsPage from "./pages/DevToolsPage";
import NotFoundPage from "./pages/NotFoundPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="loans" element={<LoansPage />} />
          <Route path="loans/:loanId" element={<LoanDetailsPage />} />

          <Route element={<RequireAuth roles={["ADMIN"]} />}>
            <Route path="loans/new" element={<LoanOnboardingPage />} />
            <Route path="dev-tools" element={<DevToolsPage />} />
          </Route>

          <Route element={<RequireAuth roles={["ADMIN", "CSR"]} />}>
            <Route path="borrowers" element={<BorrowersPage />} />
            <Route path="lenders" element={<LendersPage />} />
            <Route path="nsf-cases" element={<NsfCasesPage />} />
          </Route>

          <Route element={<RequireAuth roles={["ADMIN", "CSR", "LENDER"]} />}>
            <Route path="disbursements" element={<DisbursementsPage />} />
          </Route>

          <Route element={<RequireAuth roles={["BORROWER"]} />}>
            <Route path="pay" element={<MakePaymentPage />} />
            <Route path="bank-accounts" element={<BankAccountsPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
