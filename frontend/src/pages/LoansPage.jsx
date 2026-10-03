import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Async, Empty, PageHeader, StatusBadge } from "../components/ui";
import useApi from "../hooks/useApi";
import useDirectory from "../hooks/useDirectory";
import { date, label, money, percent } from "../utils/format";

const FILTERS = [
  ["ALL", "All"],
  ["ACTIVE", "Active"],
  ["LATE", "Past due"],
  ["DEFAULT", "Default"],
  ["PAID_OFF", "Paid off"],
];

export default function LoansPage() {
  const { role, isAdmin } = useAuth();
  const loans = useApi(endpoints.loans.list, { initial: [] });
  const directory = useDirectory();
  const [filter, setFilter] = useState("ALL");
  const [query, setQuery] = useState("");

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (loans.data || [])
      .filter((l) => {
        if (filter === "LATE") return l.status !== "PAID_OFF" && l.daysPastDue > 0;
        if (filter !== "ALL") return l.status === filter;
        return true;
      })
      .filter((l) => {
        if (!q) return true;
        const address = l.properties?.[0] ? `${l.properties[0].street} ${l.properties[0].city}` : "";
        return [l.loanNumber, directory.borrowerName(l.borrowerId), address].join(" ").toLowerCase().includes(q);
      });
  }, [loans.data, filter, query, directory]);

  const title = role === "BORROWER" ? "My loans" : role === "LENDER" ? "My portfolio" : "Loans";

  return (
    <>
      <PageHeader
        title={title}
        subtitle={`${(loans.data || []).length} loans`}
        actions={isAdmin && <Link to="/loans/new" className="button button-primary">Onboard a loan</Link>}
      />

      <div className="toolbar">
        <div className="segmented" role="group" aria-label="Filter by status">
          {FILTERS.map(([value, text]) => (
            <button key={value} type="button" aria-pressed={filter === value}
                    className={filter === value ? "segment segment-on" : "segment"}
                    onClick={() => setFilter(value)}>
              {text}
            </button>
          ))}
        </div>
        <input type="search" className="search" placeholder="Search loan number, borrower or address"
               value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search loans" />
      </div>

      <Async loading={loans.loading} error={loans.error}>
        {rows.length === 0 ? (
          <Empty title="No loans match.">Try another filter or clear the search.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Loan</th>
                  {role !== "BORROWER" && <th>Borrower</th>}
                  <th>Property</th>
                  <th>Status</th>
                  <th className="num">Rate</th>
                  <th className="num">Principal left</th>
                  <th className="num">Monthly P&amp;I</th>
                  <th>Next due</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((l) => (
                  <tr key={l.id}>
                    <td>
                      <Link to={`/loans/${l.id}`} className="link">{l.loanNumber}</Link>
                      <span className="cell-sub">{label(l.category)}</span>
                    </td>
                    {role !== "BORROWER" && <td>{directory.borrowerName(l.borrowerId)}</td>}
                    <td>{l.properties?.[0] ? `${l.properties[0].street}, ${l.properties[0].city}` : "—"}</td>
                    <td>
                      <StatusBadge status={l.status} />
                      {l.status === "ACTIVE" && l.daysPastDue > 0 && (
                        <span className="cell-sub cell-warn">{l.daysPastDue} days late</span>
                      )}
                    </td>
                    <td className="num">{percent(l.noteRate)}</td>
                    <td className="num">{money(l.principalBalance)}</td>
                    <td className="num">{money(l.monthlyPiPayment)}</td>
                    <td>{l.status === "PAID_OFF" ? "—" : date(l.nextDueDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Async>
    </>
  );
}
