import { Link } from "react-router-dom";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Async, Empty, PageHeader, Stat, StatusBadge } from "../components/ui";
import useApi from "../hooks/useApi";
import useDirectory from "../hooks/useDirectory";
import { date, money, sum } from "../utils/format";

export default function DashboardPage() {
  const { role } = useAuth();
  if (role === "BORROWER") return <BorrowerDashboard />;
  if (role === "LENDER") return <LenderDashboard />;
  return <StaffDashboard />;
}

// ---------------------------------------------------------------- staff

function StaffDashboard() {
  const loans = useApi(endpoints.loans.list, { initial: [] });
  const nsf = useApi(endpoints.nsfCases, { initial: [] });
  const directory = useDirectory();
  const all = loans.data || [];
  const open = all.filter((l) => l.status !== "PAID_OFF");
  const late = open.filter((l) => l.daysPastDue > 0).sort((a, b) => b.daysPastDue - a.daysPastDue);

  return (
    <>
      <PageHeader title="Servicing overview" subtitle="Every loan the servicer manages, as of today." />
      <Async loading={loans.loading} error={loans.error}>
        <div className="stats">
          <Stat label="Principal outstanding" value={money(sum(open, (l) => l.principalBalance))}
                note={`${open.length} open loans`} />
          <Stat label="Current" value={all.filter((l) => l.status === "ACTIVE" && l.daysPastDue === 0).length}
                note="loans paid up" tone="good" />
          <Stat label="Past due" value={late.filter((l) => l.status === "ACTIVE").length}
                note="within 30 days" tone={late.some((l) => l.status === "ACTIVE") ? "warn" : undefined} />
          <Stat label="In default" value={all.filter((l) => l.status === "DEFAULT").length}
                note="31+ days past due" tone={all.some((l) => l.status === "DEFAULT") ? "bad" : undefined} />
          <Stat label="Unpaid fees" value={money(sum(open, (l) => l.unpaidCharges))} note="late and NSF fees" />
        </div>

        <section className="panel">
          <div className="panel-head">
            <h2>Needs attention</h2>
            <Link to="/loans" className="link">All loans</Link>
          </div>
          {late.length === 0 ? (
            <Empty title="No loans are past due." />
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Loan</th><th>Borrower</th><th>Status</th><th>Due date</th>
                  <th className="num">Days late</th><th className="num">Unpaid fees</th>
                </tr>
              </thead>
              <tbody>
                {late.slice(0, 8).map((l) => (
                  <tr key={l.id}>
                    <td><Link to={`/loans/${l.id}`} className="link">{l.loanNumber}</Link></td>
                    <td>{directory.borrowerName(l.borrowerId)}</td>
                    <td><StatusBadge status={l.status} /></td>
                    <td>{date(l.nextDueDate)}</td>
                    <td className="num">{l.daysPastDue}</td>
                    <td className="num">{money(l.unpaidCharges)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="panel">
          <div className="panel-head">
            <h2>Recently returned payments</h2>
            <Link to="/nsf-cases" className="link">All returned payments</Link>
          </div>
          <Async loading={nsf.loading} error={nsf.error}>
            {(nsf.data || []).length === 0 ? (
              <Empty title="No payments have bounced." />
            ) : (
              <table className="table">
                <thead>
                  <tr><th>Date</th><th>Loan</th><th>Reason</th><th className="num">Amount</th><th className="num">Fee</th></tr>
                </thead>
                <tbody>
                  {nsf.data.slice(0, 5).map((c) => (
                    <tr key={c.id}>
                      <td>{date(c.caseDate)}</td>
                      <td><Link to={`/loans/${c.loanId}`} className="link">Loan #{c.loanId}</Link></td>
                      <td>{c.reason} ({c.returnCode})</td>
                      <td className="num">{money(c.amount)}</td>
                      <td className="num">{money(c.feeCharged)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Async>
        </section>
      </Async>
    </>
  );
}

// ---------------------------------------------------------------- lender

function LenderDashboard() {
  const { accountId, accountName } = useAuth();
  const loans = useApi(endpoints.loans.list, { initial: [] });
  const payouts = useApi(endpoints.disbursements, { initial: [] });

  const myLoans = (loans.data || []).map((l) => {
    const funding = l.fundings.find((f) => f.lenderId === accountId);
    const share = funding ? Number(funding.fundedAmount) / Number(l.originalBalance) : 0;
    return { ...l, funding, share, myPrincipal: Number(l.principalBalance) * share };
  });
  const pending = (payouts.data || []).filter((d) => d.status === "PENDING");
  const paid = (payouts.data || []).filter((d) => d.status === "PAID");

  return (
    <>
      <PageHeader title={accountName} subtitle="Your share of every loan you funded." />
      <Async loading={loans.loading} error={loans.error}>
        <div className="stats">
          <Stat label="Your principal outstanding" value={money(sum(myLoans, (l) => l.myPrincipal))}
                note={`${myLoans.filter((l) => l.status !== "PAID_OFF").length} open loans`} />
          <Stat label="Waiting to be paid out" value={money(sum(pending, (d) => d.totalAmount))}
                note="sent in the nightly payout" tone="warn" />
          <Stat label="Paid to you" value={money(sum(paid, (d) => d.totalAmount))} note="all time" tone="good" />
          <Stat label="Loans in default" value={myLoans.filter((l) => l.status === "DEFAULT").length}
                tone={myLoans.some((l) => l.status === "DEFAULT") ? "bad" : undefined} />
        </div>

        <section className="panel">
          <div className="panel-head">
            <h2>Your loans</h2>
            <Link to="/disbursements" className="link">Payout history</Link>
          </div>
          {myLoans.length === 0 ? (
            <Empty title="You haven't funded any loans yet." />
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Loan</th><th>Status</th><th className="num">Your share</th><th className="num">Your rate</th>
                  <th className="num">Your principal</th><th>Next due</th>
                </tr>
              </thead>
              <tbody>
                {myLoans.map((l) => (
                  <tr key={l.id}>
                    <td><Link to={`/loans/${l.id}`} className="link">{l.loanNumber}</Link></td>
                    <td><StatusBadge status={l.status} /></td>
                    <td className="num">{Math.round(l.share * 100)}%</td>
                    <td className="num">{l.funding ? `${Number(l.funding.lenderRate).toFixed(2)}%` : "—"}</td>
                    <td className="num">{money(l.myPrincipal)}</td>
                    <td>{date(l.nextDueDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </Async>
    </>
  );
}

// ---------------------------------------------------------------- borrower

function BorrowerDashboard() {
  const { accountName } = useAuth();
  const loans = useApi(endpoints.loans.list, { initial: [] });

  return (
    <>
      <PageHeader title={`Hello, ${accountName}`} subtitle="What you owe and when it's due." />
      <Async loading={loans.loading} error={loans.error}>
        {(loans.data || []).length === 0 ? (
          <Empty title="There are no loans on your account." />
        ) : (
          <div className="loan-cards">
            {loans.data.map((loan) => <BorrowerLoanCard key={loan.id} loan={loan} />)}
          </div>
        )}
      </Async>
    </>
  );
}

function BorrowerLoanCard({ loan }) {
  const due = useApi(loan.status === "PAID_OFF" ? null : endpoints.loans.amountDue(loan.id));
  const late = loan.daysPastDue > 0;

  return (
    <article className={late ? "loan-card loan-card-late" : "loan-card"}>
      <div className="loan-card-top">
        <Link to={`/loans/${loan.id}`} className="loan-card-number">{loan.loanNumber}</Link>
        <StatusBadge status={loan.status} />
      </div>

      {loan.status === "PAID_OFF" ? (
        <p className="loan-card-due">Paid off on {date(loan.paidOffDate)}.</p>
      ) : (
        <>
          <p className="loan-card-label">{late ? `Overdue by ${loan.daysPastDue} days` : "Next payment"}</p>
          <p className="loan-card-amount">{due.data ? money(due.data.totalDue) : "…"}</p>
          <p className="loan-card-due">Due {date(loan.nextDueDate)}</p>
          {Number(loan.unpaidCharges) > 0 && (
            <p className="loan-card-fees">Includes {money(loan.unpaidCharges)} in unpaid fees.</p>
          )}
          <Link to={`/pay?loanId=${loan.id}`} className="button button-primary">Make a payment</Link>
        </>
      )}

      <p className="loan-card-foot">
        Principal left <strong>{money(loan.principalBalance)}</strong> of {money(loan.originalBalance)}
      </p>
    </article>
  );
}
