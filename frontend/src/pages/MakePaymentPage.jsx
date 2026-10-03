import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { Alert, Async, Empty, Facts, Field, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import { date, money, newIdempotencyKey } from "../utils/format";

export default function MakePaymentPage() {
  const [params] = useSearchParams();
  const loans = useApi(endpoints.loans.list, { initial: [] });
  const accounts = useApi(endpoints.borrowers.myBankAccounts, { initial: [] });
  const openLoans = (loans.data || []).filter((l) => l.status !== "PAID_OFF");

  const [loanId, setLoanId] = useState(params.get("loanId") || "");
  const [accountId, setAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  // One key per payment attempt: pressing "Pay" twice sends the same key, so the backend charges once.
  const idempotencyKey = useRef(newIdempotencyKey());

  const due = useApi(loanId ? endpoints.loans.amountDue(loanId) : null);

  // Sensible defaults once data arrives
  useEffect(() => {
    if (!loanId && openLoans.length > 0) setLoanId(String(openLoans[0].id));
  }, [loanId, openLoans]);
  useEffect(() => {
    if (!accountId && (accounts.data || []).length > 0) setAccountId(String(accounts.data[0].id));
  }, [accountId, accounts.data]);
  useEffect(() => {
    if (due.data) setAmount(String(due.data.totalDue));
  }, [due.data]);

  const pay = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const { data } = await api.post(
        endpoints.payments.make,
        { loanId: Number(loanId), bankAccountId: Number(accountId), amount: Number(amount) },
        { headers: { "Idempotency-Key": idempotencyKey.current } }
      );
      setResult(data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const startOver = () => {
    idempotencyKey.current = newIdempotencyKey();
    setResult(null);
    due.reload();
  };

  if (result) return <PaymentReceipt payment={result} onDone={startOver} />;

  const extra = due.data ? Number(amount || 0) - Number(due.data.totalDue) : 0;

  return (
    <>
      <PageHeader title="Make a payment" subtitle="Paid by bank transfer (ACH) from your account." />
      <Async loading={loans.loading || accounts.loading} error={loans.error || accounts.error}>
        {openLoans.length === 0 ? (
          <Empty title="You have no loans to pay." />
        ) : (accounts.data || []).length === 0 ? (
          <Empty title="Add a bank account first.">
            <Link to="/bank-accounts" className="button button-primary">Add a bank account</Link>
          </Empty>
        ) : (
          <div className="pay-layout">
            <form className="panel stack" onSubmit={pay}>
              <Alert>{error}</Alert>
              <Field label="Loan">
                <select value={loanId} onChange={(e) => setLoanId(e.target.value)}>
                  {openLoans.map((l) => (
                    <option key={l.id} value={l.id}>{l.loanNumber}, due {date(l.nextDueDate)}</option>
                  ))}
                </select>
              </Field>
              <Field label="Pay from">
                <select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
                  {accounts.data.map((a) => (
                    <option key={a.id} value={a.id}>{a.bankName} {a.accountNumberMasked}</option>
                  ))}
                </select>
              </Field>
              <Field
                label="Amount (USD)"
                hint={due.data && `At least ${money(due.data.totalDue)}. Anything above that pays down principal early.`}
              >
                <input type="number" step="0.01" min={due.data?.totalDue || 0.01} required value={amount}
                       onChange={(e) => setAmount(e.target.value)} />
              </Field>
              {due.data && extra > 0 && (
                <p className="muted">{money(extra)} will go to extra principal.</p>
              )}
              <button type="submit" className="button button-primary button-block" disabled={busy || !due.data}>
                {busy ? "Sending payment…" : `Pay ${money(amount || 0)}`}
              </button>
            </form>

            <aside className="panel">
              <h2>What this payment covers</h2>
              <Async loading={due.loading} error={due.error}>
                {due.data && (
                  <>
                    <p className="muted">Installment due {date(due.data.dueDate)}, applied in this order:</p>
                    <ol className="waterfall">
                      <li><span>Unpaid fees</span><span>{money(due.data.unpaidCharges)}</span></li>
                      <li><span>Interest</span><span>{money(due.data.interest)}</span></li>
                      <li><span>Principal</span><span>{money(due.data.principal)}</span></li>
                      <li><span>Reserve (escrow)</span><span>{money(due.data.reserve)}</span></li>
                      <li><span>Impound (tax, insurance)</span><span>{money(due.data.impound)}</span></li>
                      <li className="waterfall-total"><span>Minimum payment</span><span>{money(due.data.totalDue)}</span></li>
                    </ol>
                    <p className="muted">To pay off the whole loan today: {money(due.data.payoffAmount)}.</p>
                  </>
                )}
              </Async>
            </aside>
          </div>
        )}
      </Async>
    </>
  );
}

function PaymentReceipt({ payment, onDone }) {
  return (
    <>
      <PageHeader title="Payment sent" subtitle={`Bank trace ${payment.achReference}`} />
      <section className="panel receipt">
        <p className="receipt-amount">{money(payment.amount)}</p>
        <p className="muted">
          Applied to the installment due {date(payment.dueDateBefore)}.
          {payment.dueDateAfter ? ` Your next payment is due ${date(payment.dueDateAfter)}.` : " Your loan is paid off."}
        </p>
        <Facts
          items={[
            ["Fees", money(payment.chargesPaid)],
            ["Interest", money(payment.interestPaid)],
            ["Principal", money(payment.principalPaid)],
            ["Extra principal", money(payment.extraPrincipalPaid)],
            ["Reserve", money(payment.reservePaid)],
            ["Impound", money(payment.impoundPaid)],
          ]}
        />
        <div className="form-actions">
          <Link to={`/loans/${payment.loanId}`} className="button">View loan</Link>
          <button type="button" className="button button-primary" onClick={onDone}>Make another payment</button>
        </div>
      </section>
    </>
  );
}
