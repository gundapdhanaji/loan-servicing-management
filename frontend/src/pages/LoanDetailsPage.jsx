import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Alert, Async, Empty, Facts, Field, Modal, StatusBadge, Tabs } from "../components/ui";
import useApi from "../hooks/useApi";
import useDirectory from "../hooks/useDirectory";
import { date, label, money, percent } from "../utils/format";

export default function LoanDetailsPage() {
  const { loanId } = useParams();
  const { role } = useAuth();
  const loan = useApi(endpoints.loans.get(loanId));
  const payments = useApi(endpoints.payments.byLoan(loanId), { initial: [] });
  const [tab, setTab] = useState("terms");

  const reload = () => {
    loan.reload();
    payments.reload();
  };

  return (
    <Async loading={loan.loading && !loan.data} error={loan.error}>
      {loan.data && (
        <>
          <p className="crumb"><Link to="/loans" className="link">Loans</Link> / {loan.data.loanNumber}</p>
          <LoanStatement loan={loan.data} canPay={role === "BORROWER"} />
          <Tabs
            active={tab}
            onChange={setTab}
            tabs={[
              { id: "terms", label: "Terms" },
              { id: "property", label: "Property", count: loan.data.properties.length },
              { id: "funding", label: "Funding", count: loan.data.fundings.length },
              { id: "insurance", label: "Insurance", count: loan.data.insurances.length },
              { id: "charges", label: "Charges", count: loan.data.charges.length },
              { id: "payments", label: "Payments", count: (payments.data || []).length },
            ]}
          />
          <section className="panel tab-panel">
            {tab === "terms" && <TermsTab loan={loan.data} />}
            {tab === "property" && <PropertyTab loan={loan.data} />}
            {tab === "funding" && <FundingTab loan={loan.data} />}
            {tab === "insurance" && <InsuranceTab loan={loan.data} />}
            {tab === "charges" && <ChargesTab loan={loan.data} onChange={reload} />}
            {tab === "payments" && <PaymentsTab payments={payments} />}
          </section>
        </>
      )}
    </Async>
  );
}

/** The top of the page: reads like the header of a loan statement. */
function LoanStatement({ loan, canPay }) {
  const directory = useDirectory();
  const repaid = Number(loan.originalBalance) - Number(loan.principalBalance);
  const repaidPct = Math.max(0, Math.min(100, (repaid / Number(loan.originalBalance)) * 100));
  const primary = loan.properties.find((p) => p.primary) || loan.properties[0];

  return (
    <section className="statement">
      <div className="statement-main">
        <div className="statement-title">
          <h1>{loan.loanNumber}</h1>
          <StatusBadge status={loan.status} />
        </div>
        <p className="muted">
          {directory.borrowerName(loan.borrowerId)}
          {primary && <>, {primary.street}, {primary.city} {primary.state}</>}
        </p>

        <p className="statement-figure-label">Principal balance</p>
        <p className="statement-figure">{money(loan.principalBalance)}</p>
        <div className="repaid" aria-label={`${repaidPct.toFixed(1)}% of principal repaid`}>
          <div className="repaid-bar"><span style={{ width: `${repaidPct}%` }} /></div>
          <span className="repaid-text">
            {money(repaid)} repaid of {money(loan.originalBalance)}
          </span>
        </div>
      </div>

      <div className="statement-side">
        {loan.status === "PAID_OFF" ? (
          <Facts items={[["Paid off", date(loan.paidOffDate)], ["Last payment", date(loan.lastPaymentDate)]]} />
        ) : (
          <Facts
            items={[
              ["Next payment due", date(loan.nextDueDate)],
              ["Days past due", loan.daysPastDue > 0 ? <span className="text-warn">{loan.daysPastDue}</span> : "0"],
              ["Monthly P&I", money(loan.monthlyPiPayment)],
              ["Unpaid fees", money(loan.unpaidCharges)],
              ["Escrow held", money(Number(loan.reserveBalance) + Number(loan.impoundBalance))],
            ]}
          />
        )}
        {canPay && loan.status !== "PAID_OFF" && (
          <Link to={`/pay?loanId=${loan.id}`} className="button button-primary button-block">Make a payment</Link>
        )}
      </div>
    </section>
  );
}

function TermsTab({ loan }) {
  return (
    <div className="two-col">
      <div>
        <h3>Loan</h3>
        <Facts
          items={[
            ["Category", label(loan.category)],
            ["Purpose", label(loan.purpose)],
            ["Lien position", `${loan.lienPriority}${["st", "nd", "rd"][loan.lienPriority - 1] || "th"}`],
            ["Original amount", money(loan.originalBalance)],
            ["Note rate", percent(loan.noteRate, 3)],
            ["Term", `${loan.termMonths} months`],
            ["Closing date", date(loan.closingDate)],
            ["First payment", date(loan.firstPaymentDate)],
            ["Maturity", date(loan.maturityDate)],
          ]}
        />
      </div>
      <div>
        <h3>Monthly payment and fees</h3>
        <Facts
          items={[
            ["Principal & interest", money(loan.monthlyPiPayment)],
            ["Reserve (escrow)", money(loan.reservePayment)],
            ["Impound (tax, insurance)", money(loan.impoundPayment)],
            ["Grace period", `${loan.graceDays} days`],
            ["Late fee", `${percent(loan.lateChargePercent)} of P&I, at least ${money(loan.lateChargeMinimum)}`],
            ["Reserve balance", money(loan.reserveBalance)],
            ["Impound balance", money(loan.impoundBalance)],
            ["Last payment", date(loan.lastPaymentDate)],
          ]}
        />
      </div>
    </div>
  );
}

function PropertyTab({ loan }) {
  if (loan.properties.length === 0) return <Empty title="No property is recorded for this loan." />;
  return (
    <div className="card-list">
      {loan.properties.map((p) => (
        <div key={p.id} className="sub-card">
          <h3>
            {p.street}, {p.city} {p.state} {p.zipCode}
            {p.primary && <span className="badge badge-neutral">Primary</span>}
          </h3>
          <Facts
            items={[
              ["Type", p.propertyType || "—"],
              ["Occupancy", p.occupancy || "—"],
              ["Appraised value", money(p.appraisedValue)],
              ["Loan to value", p.appraisedValue ? percent((Number(loan.originalBalance) / Number(p.appraisedValue)) * 100, 1) : "—"],
              ["Flood zone", p.floodZone || "—"],
            ]}
          />
        </div>
      ))}
    </div>
  );
}

function FundingTab({ loan }) {
  const directory = useDirectory();
  return (
    <>
      <p className="muted tab-intro">
        Lenders earn their own rate on their share. The servicer keeps the difference to the note rate
        ({percent(loan.noteRate)}) as its servicing fee.
      </p>
      <table className="table">
        <thead>
          <tr>
            <th>Lender</th><th className="num">Funded</th><th className="num">Share</th>
            <th className="num">Lender rate</th><th className="num">Servicing spread</th><th>Funded on</th>
          </tr>
        </thead>
        <tbody>
          {loan.fundings.map((f) => (
            <tr key={f.id}>
              <td>{directory.lenderName(f.lenderId)}</td>
              <td className="num">{money(f.fundedAmount)}</td>
              <td className="num">{percent((Number(f.fundedAmount) / Number(loan.originalBalance)) * 100, 1)}</td>
              <td className="num">{percent(f.lenderRate)}</td>
              <td className="num">{percent(Number(loan.noteRate) - Number(f.lenderRate))}</td>
              <td>{date(f.fundingDate)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

function InsuranceTab({ loan }) {
  if (loan.insurances.length === 0) return <Empty title="No insurance policy is recorded." />;
  return (
    <table className="table">
      <thead>
        <tr><th>Company</th><th>Policy</th><th className="num">Coverage</th><th>Expires</th><th>Agent</th></tr>
      </thead>
      <tbody>
        {loan.insurances.map((i) => (
          <tr key={i.id}>
            <td>{i.companyName}</td>
            <td>{i.policyNumber}</td>
            <td className="num">{money(i.coverageAmount)}</td>
            <td>
              {date(i.expirationDate)}
              {i.expiringSoon && <span className="cell-sub cell-warn">Expires within 30 days</span>}
            </td>
            <td>
              {i.agentName}
              <span className="cell-sub">{i.agentPhone}</span>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ChargesTab({ loan, onChange }) {
  const { isStaff, isAdmin } = useAuth();
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState(null);

  const waive = async (chargeId) => {
    setError(null);
    try {
      await api.post(endpoints.loans.waiveCharge(loan.id, chargeId));
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <>
      <div className="panel-head">
        <p className="muted">Fees are paid before interest and principal on the next payment.</p>
        {isStaff && loan.status !== "PAID_OFF" && (
          <button type="button" className="button" onClick={() => setAdding(true)}>Add a charge</button>
        )}
      </div>
      <Alert>{error}</Alert>
      {loan.charges.length === 0 ? (
        <Empty title="No charges on this loan." />
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Date</th><th>Type</th><th>Description</th><th className="num">Amount</th>
              <th className="num">Still owed</th><th>Status</th>{isAdmin && <th />}
            </tr>
          </thead>
          <tbody>
            {loan.charges.map((c) => (
              <tr key={c.id}>
                <td>{date(c.chargeDate)}</td>
                <td>{label(c.type)}</td>
                <td>{c.description}</td>
                <td className="num">{money(c.originalAmount)}</td>
                <td className="num">{money(c.balance)}</td>
                <td><StatusBadge status={c.status} /></td>
                {isAdmin && (
                  <td className="num">
                    {c.status === "OPEN" && (
                      <button type="button" className="link-button" onClick={() => waive(c.id)}>Waive</button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {adding && <AddChargeModal loanId={loan.id} onClose={() => setAdding(false)} onSaved={onChange} />}
    </>
  );
}

function AddChargeModal({ loanId, onClose, onSaved }) {
  const [form, setForm] = useState({ type: "ADMIN_FEE", amount: "", description: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(endpoints.loans.addCharge(loanId), { ...form, amount: Number(form.amount) });
      onSaved();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title="Add a charge" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Alert>{error}</Alert>
        <Field label="Type">
          <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
            {["LATE_FEE", "NSF_FEE", "ADMIN_FEE", "PROCESSING_FEE", "OTHER"].map((t) => (
              <option key={t} value={t}>{label(t)}</option>
            ))}
          </select>
        </Field>
        <Field label="Amount (USD)">
          <input type="number" min="0.01" step="0.01" required value={form.amount}
                 onChange={(e) => setForm({ ...form, amount: e.target.value })} />
        </Field>
        <Field label="Description">
          <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </Field>
        <div className="form-actions">
          <button type="button" className="button" onClick={onClose}>Cancel</button>
          <button type="submit" className="button button-primary" disabled={busy}>Add charge</button>
        </div>
      </form>
    </Modal>
  );
}

function PaymentsTab({ payments }) {
  return (
    <Async loading={payments.loading} error={payments.error}>
      {(payments.data || []).length === 0 ? (
        <Empty title="No payments yet." />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Date</th><th>Status</th><th className="num">Amount</th><th className="num">Fees</th>
                <th className="num">Interest</th><th className="num">Principal</th><th className="num">Escrow</th>
                <th>Installment</th><th>Bank trace</th>
              </tr>
            </thead>
            <tbody>
              {payments.data.map((p) => (
                <tr key={p.id} className={p.status === "RETURNED" ? "row-muted" : undefined}>
                  <td>{date(p.paymentDate)}</td>
                  <td>
                    <StatusBadge status={p.status} />
                    {p.status === "RETURNED" && (
                      <span className="cell-sub cell-bad">{p.returnReason} ({p.returnCode}), {date(p.returnedDate)}</span>
                    )}
                  </td>
                  <td className="num">{money(p.amount)}</td>
                  <td className="num">{money(p.chargesPaid)}</td>
                  <td className="num">{money(p.interestPaid)}</td>
                  <td className="num">{money(Number(p.principalPaid) + Number(p.extraPrincipalPaid))}</td>
                  <td className="num">{money(Number(p.reservePaid) + Number(p.impoundPaid))}</td>
                  <td>{date(p.dueDateBefore)}</td>
                  <td className="cell-code">{p.achReference}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Async>
  );
}
