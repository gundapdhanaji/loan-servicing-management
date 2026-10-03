import { useState } from "react";
import { Link } from "react-router-dom";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Async, Empty, PageHeader, Stat, StatusBadge } from "../components/ui";
import useApi from "../hooks/useApi";
import useDirectory from "../hooks/useDirectory";
import { date, money, sum } from "../utils/format";

export default function DisbursementsPage() {
  const { isStaff } = useAuth();
  const payouts = useApi(endpoints.disbursements, { initial: [] });
  const directory = useDirectory();
  const [status, setStatus] = useState("ALL");

  const all = payouts.data || [];
  const rows = status === "ALL" ? all : all.filter((d) => d.status === status);
  const pending = all.filter((d) => d.status === "PENDING");

  return (
    <>
      <PageHeader
        title={isStaff ? "Lender payouts" : "My payouts"}
        subtitle="Each borrower payment is split between the loan's lenders and paid out in the nightly run."
      />
      <Async loading={payouts.loading} error={payouts.error}>
        <div className="stats">
          <Stat label="Waiting for payout" value={money(sum(pending, (d) => d.totalAmount))} tone="warn" />
          <Stat label="Paid out" value={money(sum(all.filter((d) => d.status === "PAID"), (d) => d.totalAmount))} tone="good" />
          <Stat label="Interest earned" value={money(sum(all.filter((d) => d.status !== "CANCELLED"), (d) => d.interestAmount))} />
        </div>

        <div className="toolbar">
          <div className="segmented" role="group" aria-label="Filter by status">
            {["ALL", "PENDING", "PAID", "CANCELLED"].map((s) => (
              <button key={s} type="button" aria-pressed={status === s}
                      className={status === s ? "segment segment-on" : "segment"} onClick={() => setStatus(s)}>
                {s === "ALL" ? "All" : s.charAt(0) + s.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>

        {rows.length === 0 ? (
          <Empty title="No payouts here yet.">They appear after a borrower pays.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  {isStaff && <th>Lender</th>}
                  <th>Loan</th><th>From payment</th><th className="num">Principal</th><th className="num">Interest</th>
                  <th className="num">Total</th><th>Status</th><th>Paid on</th><th>Note</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((d) => (
                  <tr key={d.id} className={Number(d.totalAmount) < 0 ? "row-clawback" : undefined}>
                    {isStaff && <td>{directory.lenderName(d.lenderId)}</td>}
                    <td><Link to={`/loans/${d.loanId}`} className="link">Loan #{d.loanId}</Link></td>
                    <td>#{d.paymentId}</td>
                    <td className="num">{money(d.principalAmount)}</td>
                    <td className="num">{money(d.interestAmount)}</td>
                    <td className="num">{money(d.totalAmount)}</td>
                    <td><StatusBadge status={d.status} /></td>
                    <td>{date(d.disbursedDate)}</td>
                    <td className="cell-note">{d.note}</td>
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
