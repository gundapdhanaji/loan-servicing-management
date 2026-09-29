import { useState } from "react";
import { Navigate, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import { Alert } from "../components/ui";

// Sample users created by the backend on first start (DevDataLoader). Password: "password".
const DEMO_USERS = [
  ["admin@loan.local", "Admin"],
  ["csr@loan.local", "Customer service"],
  ["lender1@loan.local", "Lender (Evergreen Capital)"],
  ["john@loan.local", "Borrower, current"],
  ["maria@loan.local", "Borrower, late"],
  ["david@loan.local", "Borrower, in default"],
];

export default function LoginPage() {
  const { session, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (session) return <Navigate to="/" replace />;

  const onSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate(location.state?.from || "/", { replace: true });
    } catch (err) {
      setError(err?.response?.status === 401 ? "Invalid email and/or password." : errorMessage(err));
      setBusy(false);
    }
  };

  const fillDemo = (demoEmail) => {
    setEmail(demoEmail);
    setPassword("password");
    setError(null);
  };

  return (
    <div className="login">
      <section className="login-panel">
        <div className="brand brand-dark">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">Loan Servicing Management</span>
        </div>
        <h1 className="login-title">Sign in</h1>
        <p className="muted">Borrowers, lenders and servicing staff use the same sign-in.</p>

        {params.get("expired") && !error && (
          <Alert tone="info">Your session ended. Please sign in again.</Alert>
        )}
        <Alert>{error}</Alert>

        <form onSubmit={onSubmit} className="stack">
          <label className="field">
            <span className="field-label">Email</span>
            <input type="email" autoComplete="username" required value={email}
                   onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label className="field">
            <span className="field-label">Password</span>
            <input type="password" autoComplete="current-password" required value={password}
                   onChange={(e) => setPassword(e.target.value)} />
          </label>
          <button type="submit" className="button button-primary button-block" disabled={busy}>
            {busy ? "Signing in…" : "Sign in"}
          </button>
        </form>
      </section>

      <aside className="login-demo">
        <h2>Demo accounts</h2>
        <p>Created by the backend on first start. Pick one to fill the form. Every password is "password".</p>
        <p>Maria's bank account ends in 0000, so her payments bounce when bank returns are processed.</p>
        <ul>
          {DEMO_USERS.map(([demoEmail, who]) => (
            <li key={demoEmail}>
              <button type="button" onClick={() => fillDemo(demoEmail)}>
                <span className="demo-who">{who}</span>
                <span className="demo-email">{demoEmail}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}
