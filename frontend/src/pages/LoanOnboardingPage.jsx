import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { Alert, Async, Empty, Field, PageHeader } from "../components/ui";
import useDirectory from "../hooks/useDirectory";
import { money } from "../utils/format";

const today = new Date();
// yyyy-mm-dd in the browser's own time zone (toISOString would use UTC and can be a day off)
const iso = (d) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 1);

const INITIAL = {
  loanNumber: "",
  borrowerId: "",
  category: "RESIDENTIAL",
  purpose: "PURCHASE",
  lienPriority: 1,
  originalBalance: "",
  noteRate: "12",
  termMonths: 360,
  monthlyPiPayment: "",
  reservePayment: "150",
  impoundPayment: "250",
  graceDays: 10,
  lateChargePercent: "5",
  lateChargeMinimum: "50",
  closingDate: iso(today),
  firstPaymentDate: iso(nextMonth),
  property: {
    street: "", city: "", state: "", zipCode: "", propertyType: "Single Family",
    occupancy: "Owner Occupied", appraisedValue: "", floodZone: "X - Low Risk", legalDescription: "",
  },
  insurance: { companyName: "", policyNumber: "", coverageAmount: "", expirationDate: "", agentName: "", agentPhone: "", agentEmail: "" },
  fundings: [{ lenderId: "", fundedAmount: "", lenderRate: "10" }],
};

const num = (v) => (v === "" || v === null || v === undefined ? null : Number(v));

/** Same tabs as the loan onboarding screen: details, terms, property, funding, insurance - one request. */
export default function LoanOnboardingPage() {
  const navigate = useNavigate();
  const directory = useDirectory();
  const [form, setForm] = useState(INITIAL);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });
  const setIn = (group, key) => (e) => setForm({ ...form, [group]: { ...form[group], [key]: e.target.value } });
  const setFunding = (i, key) => (e) =>
    setForm({ ...form, fundings: form.fundings.map((f, j) => (i === j ? { ...f, [key]: e.target.value } : f)) });

  const fundedTotal = form.fundings.reduce((t, f) => t + Number(f.fundedAmount || 0), 0);
  const loanAmount = Number(form.originalBalance || 0);
  const fundingMatches = loanAmount > 0 && Math.abs(fundedTotal - loanAmount) < 0.005;

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!fundingMatches) {
      setError(`Fundings add up to ${money(fundedTotal)} but the loan amount is ${money(loanAmount)}.`);
      return;
    }
    setBusy(true);
    const p = form.property;
    const ins = form.insurance;
    const body = {
      loanNumber: form.loanNumber || null,
      borrowerId: Number(form.borrowerId),
      category: form.category,
      purpose: form.purpose,
      lienPriority: Number(form.lienPriority),
      originalBalance: num(form.originalBalance),
      noteRate: num(form.noteRate),
      termMonths: Number(form.termMonths),
      monthlyPiPayment: num(form.monthlyPiPayment),
      reservePayment: num(form.reservePayment),
      impoundPayment: num(form.impoundPayment),
      graceDays: Number(form.graceDays),
      lateChargePercent: num(form.lateChargePercent),
      lateChargeMinimum: num(form.lateChargeMinimum),
      closingDate: form.closingDate,
      firstPaymentDate: form.firstPaymentDate,
      properties: p.street ? [{ ...p, appraisedValue: num(p.appraisedValue), primary: true }] : [],
      fundings: form.fundings.map((f) => ({
        lenderId: Number(f.lenderId),
        fundedAmount: num(f.fundedAmount),
        lenderRate: num(f.lenderRate),
        fundingDate: form.closingDate,
      })),
      insurances: ins.companyName
        ? [{ ...ins, coverageAmount: num(ins.coverageAmount), expirationDate: ins.expirationDate || null }]
        : [],
    };
    try {
      const { data } = await api.post(endpoints.loans.create, body);
      navigate(`/loans/${data.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const noParties = directory.borrowers.length === 0 || directory.lenders.length === 0;

  return (
    <>
      <PageHeader title="Onboard a loan" subtitle="Enter an existing loan so it can be serviced." />
      <Alert>{error}</Alert>
      <Async loading={directory.loading} error={null}>
        {noParties ? (
          <Empty title="You need at least one borrower and one lender first.">
            <Link to="/borrowers" className="link">Add a borrower</Link> and <Link to="/lenders" className="link">add a lender</Link>.
          </Empty>
        ) : (
          <form onSubmit={submit} className="onboard">
            <fieldset className="panel">
              <legend>Loan details</legend>
              <div className="grid">
                <Field label="Borrower">
                  <select required value={form.borrowerId} onChange={set("borrowerId")}>
                    <option value="">Choose a borrower</option>
                    {directory.borrowers.map((b) => <option key={b.id} value={b.id}>{b.fullName} ({b.email})</option>)}
                  </select>
                </Field>
                <Field label="Loan number" hint="Leave empty to generate one.">
                  <input value={form.loanNumber} onChange={set("loanNumber")} />
                </Field>
                <Field label="Category">
                  <select value={form.category} onChange={set("category")}>
                    <option value="RESIDENTIAL">Residential</option>
                    <option value="COMMERCIAL">Commercial</option>
                    <option value="CONSTRUCTION">Construction</option>
                    <option value="LAND">Land</option>
                  </select>
                </Field>
                <Field label="Purpose">
                  <select value={form.purpose} onChange={set("purpose")}>
                    <option value="PURCHASE">Purchase</option>
                    <option value="REFINANCE">Refinance</option>
                    <option value="CONSTRUCTION">Construction</option>
                    <option value="CASH_OUT">Cash out</option>
                  </select>
                </Field>
                <Field label="Lien position">
                  <select value={form.lienPriority} onChange={set("lienPriority")}>
                    <option value={1}>1st</option><option value={2}>2nd</option><option value={3}>3rd</option>
                  </select>
                </Field>
                <Field label="Closing date"><input type="date" required value={form.closingDate} onChange={set("closingDate")} /></Field>
              </div>
            </fieldset>

            <fieldset className="panel">
              <legend>Terms</legend>
              <div className="grid">
                <Field label="Loan amount (USD)">
                  <input type="number" min="1000" step="0.01" required value={form.originalBalance} onChange={set("originalBalance")} />
                </Field>
                <Field label="Note rate (% per year)">
                  <input type="number" min="0" step="0.001" required value={form.noteRate} onChange={set("noteRate")} />
                </Field>
                <Field label="Term (months)">
                  <input type="number" min="1" required value={form.termMonths} onChange={set("termMonths")} />
                </Field>
                <Field label="Monthly P&I (USD)" hint="Leave empty to calculate it (EMI).">
                  <input type="number" min="0" step="0.01" value={form.monthlyPiPayment} onChange={set("monthlyPiPayment")} />
                </Field>
                <Field label="First payment date">
                  <input type="date" required value={form.firstPaymentDate} onChange={set("firstPaymentDate")} />
                </Field>
                <Field label="Reserve per month (USD)">
                  <input type="number" min="0" step="0.01" value={form.reservePayment} onChange={set("reservePayment")} />
                </Field>
                <Field label="Impound per month (USD)">
                  <input type="number" min="0" step="0.01" value={form.impoundPayment} onChange={set("impoundPayment")} />
                </Field>
                <Field label="Grace period (days)">
                  <input type="number" min="0" value={form.graceDays} onChange={set("graceDays")} />
                </Field>
                <Field label="Late fee (% of P&I)">
                  <input type="number" min="0" step="0.01" value={form.lateChargePercent} onChange={set("lateChargePercent")} />
                </Field>
                <Field label="Minimum late fee (USD)">
                  <input type="number" min="0" step="0.01" value={form.lateChargeMinimum} onChange={set("lateChargeMinimum")} />
                </Field>
              </div>
            </fieldset>

            <fieldset className="panel">
              <legend>Funding</legend>
              <p className="muted">
                Who put up the money. The amounts must add up to the loan amount, and each lender's rate
                can't be higher than the note rate (the difference is the servicing fee).
              </p>
              {form.fundings.map((f, i) => (
                <div key={i} className="funding-row">
                  <Field label="Lender">
                    <select required value={f.lenderId} onChange={setFunding(i, "lenderId")}>
                      <option value="">Choose a lender</option>
                      {directory.lenders.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                    </select>
                  </Field>
                  <Field label="Amount (USD)">
                    <input type="number" min="0.01" step="0.01" required value={f.fundedAmount} onChange={setFunding(i, "fundedAmount")} />
                  </Field>
                  <Field label="Lender rate (%)">
                    <input type="number" min="0" step="0.001" required value={f.lenderRate} onChange={setFunding(i, "lenderRate")} />
                  </Field>
                  {form.fundings.length > 1 && (
                    <button type="button" className="link-button funding-remove"
                            onClick={() => setForm({ ...form, fundings: form.fundings.filter((_, j) => j !== i) })}>
                      Remove
                    </button>
                  )}
                </div>
              ))}
              <div className="funding-foot">
                <button type="button" className="button"
                        onClick={() => setForm({ ...form, fundings: [...form.fundings, { lenderId: "", fundedAmount: "", lenderRate: form.noteRate }] })}>
                  Add another lender
                </button>
                <span className={fundingMatches ? "text-good" : "text-warn"}>
                  Funded {money(fundedTotal)} of {money(loanAmount)}
                </span>
              </div>
            </fieldset>

            <fieldset className="panel">
              <legend>Property</legend>
              <div className="grid">
                <Field label="Street" wide><input value={form.property.street} onChange={setIn("property", "street")} /></Field>
                <Field label="City"><input value={form.property.city} onChange={setIn("property", "city")} /></Field>
                <Field label="State"><input value={form.property.state} onChange={setIn("property", "state")} /></Field>
                <Field label="ZIP code"><input value={form.property.zipCode} onChange={setIn("property", "zipCode")} /></Field>
                <Field label="Property type">
                  <select value={form.property.propertyType} onChange={setIn("property", "propertyType")}>
                    {["Single Family", "Multi-Family", "Condo", "Townhouse", "Commercial", "Land", "Mixed Use"].map((t) => <option key={t}>{t}</option>)}
                  </select>
                </Field>
                <Field label="Occupancy">
                  <select value={form.property.occupancy} onChange={setIn("property", "occupancy")}>
                    {["Owner Occupied", "Investment", "Second Home", "Vacant"].map((t) => <option key={t}>{t}</option>)}
                  </select>
                </Field>
                <Field label="Appraised value (USD)">
                  <input type="number" min="0" step="0.01" value={form.property.appraisedValue} onChange={setIn("property", "appraisedValue")} />
                </Field>
                <Field label="Flood zone">
                  <select value={form.property.floodZone} onChange={setIn("property", "floodZone")}>
                    {["X - Low Risk", "AE - High Risk", "VE - Very High Risk", "D - Undetermined Risk"].map((t) => <option key={t}>{t}</option>)}
                  </select>
                </Field>
              </div>
            </fieldset>

            <fieldset className="panel">
              <legend>Insurance</legend>
              <div className="grid">
                <Field label="Insurance company"><input value={form.insurance.companyName} onChange={setIn("insurance", "companyName")} /></Field>
                <Field label="Policy number"><input value={form.insurance.policyNumber} onChange={setIn("insurance", "policyNumber")} /></Field>
                <Field label="Coverage (USD)">
                  <input type="number" min="0" step="0.01" value={form.insurance.coverageAmount} onChange={setIn("insurance", "coverageAmount")} />
                </Field>
                <Field label="Expires on">
                  <input type="date" value={form.insurance.expirationDate} onChange={setIn("insurance", "expirationDate")} />
                </Field>
                <Field label="Agent name"><input value={form.insurance.agentName} onChange={setIn("insurance", "agentName")} /></Field>
                <Field label="Agent phone"><input value={form.insurance.agentPhone} onChange={setIn("insurance", "agentPhone")} /></Field>
              </div>
            </fieldset>

            <div className="form-actions sticky-actions">
              <Link to="/loans" className="button">Cancel</Link>
              <button type="submit" className="button button-primary" disabled={busy}>
                {busy ? "Onboarding…" : "Onboard loan"}
              </button>
            </div>
          </form>
        )}
      </Async>
    </>
  );
}
