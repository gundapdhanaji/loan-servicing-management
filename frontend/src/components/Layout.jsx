import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { label } from "../utils/format";

const NAV = {
  ADMIN: [
    ["/", "Dashboard"],
    ["/loans", "Loans"],
    ["/loans/new", "Onboard a loan"],
    ["/borrowers", "Borrowers"],
    ["/lenders", "Lenders"],
    ["/disbursements", "Lender payouts"],
    ["/nsf-cases", "Returned payments"],
    ["/dev-tools", "Dev tools"],
  ],
  CSR: [
    ["/", "Dashboard"],
    ["/loans", "Loans"],
    ["/borrowers", "Borrowers"],
    ["/lenders", "Lenders"],
    ["/disbursements", "Lender payouts"],
    ["/nsf-cases", "Returned payments"],
  ],
  LENDER: [
    ["/", "Dashboard"],
    ["/loans", "My portfolio"],
    ["/disbursements", "My payouts"],
  ],
  BORROWER: [
    ["/", "Dashboard"],
    ["/loans", "My loans"],
    ["/pay", "Make a payment"],
    ["/bank-accounts", "Bank accounts"],
  ],
};

export default function Layout() {
  const { role, accountName, session, logout } = useAuth();
  const navigate = useNavigate();
  const links = NAV[role] || [];

  const onLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">
            Loan Servicing
            <br />
            Management
          </span>
        </div>
        <nav aria-label="Main">
          {links.map(([to, text]) => (
            <NavLink key={to} to={to} end={to === "/" || to === "/loans"} className="nav-link">
              {text}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <span className="who">{accountName}</span>
          {accountName !== session?.email && <span className="who-meta">{session?.email}</span>}
          <span className="role-chip">{label(role)}</span>
          <button type="button" className="link-button" onClick={onLogout}>
            Log out
          </button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
