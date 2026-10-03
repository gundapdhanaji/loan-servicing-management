import { useState } from "react";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { Alert, Async, Empty, Field, Modal, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import { label } from "../utils/format";

const EMPTY = { bankName: "", accountHolderName: "", routingNumber: "", accountNumber: "", accountType: "CHECKING" };

export default function BankAccountsPage() {
  const accounts = useApi(endpoints.borrowers.myBankAccounts, { initial: [] });
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState(null);

  const remove = async (id) => {
    if (!window.confirm("Remove this bank account? Past payments from it stay on record.")) return;
    setError(null);
    try {
      await api.delete(endpoints.borrowers.removeBankAccount(id));
      accounts.reload();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <>
      <PageHeader
        title="Bank accounts"
        subtitle="Accounts you can pay from."
        actions={<button type="button" className="button button-primary" onClick={() => setAdding(true)}>Add a bank account</button>}
      />
      <Alert>{error}</Alert>
      <Async loading={accounts.loading} error={accounts.error}>
        {(accounts.data || []).length === 0 ? (
          <Empty title="No bank accounts yet.">Add one to start making payments.</Empty>
        ) : (
          <div className="card-list">
            {accounts.data.map((a) => (
              <div key={a.id} className="sub-card bank-card">
                <div>
                  <h3>{a.bankName}</h3>
                  <p className="muted">{a.accountHolderName}, {label(a.accountType)}</p>
                  <p className="bank-number">Account {a.accountNumberMasked}, routing {a.routingNumber}</p>
                </div>
                <button type="button" className="link-button" onClick={() => remove(a.id)}>Remove</button>
              </div>
            ))}
          </div>
        )}
      </Async>
      {adding && <AddAccountModal onClose={() => setAdding(false)} onSaved={accounts.reload} />}
    </>
  );
}

function AddAccountModal({ onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(endpoints.borrowers.myBankAccounts, form);
      onSaved();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title="Add a bank account" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Alert>{error}</Alert>
        <Field label="Bank name"><input required value={form.bankName} onChange={set("bankName")} /></Field>
        <Field label="Name on the account">
          <input required value={form.accountHolderName} onChange={set("accountHolderName")} />
        </Field>
        <Field label="Routing number" hint="9 digits, printed at the bottom left of a check.">
          <input required inputMode="numeric" pattern="\d{9}" maxLength={9} value={form.routingNumber}
                 onChange={set("routingNumber")} />
        </Field>
        <Field label="Account number" hint="Local testing: an account ending in 0000 will bounce (NSF).">
          <input required inputMode="numeric" pattern="\d{4,17}" maxLength={17} value={form.accountNumber}
                 onChange={set("accountNumber")} />
        </Field>
        <Field label="Account type">
          <select value={form.accountType} onChange={set("accountType")}>
            <option value="CHECKING">Checking</option>
            <option value="SAVINGS">Savings</option>
          </select>
        </Field>
        <div className="form-actions">
          <button type="button" className="button" onClick={onClose}>Cancel</button>
          <button type="submit" className="button button-primary" disabled={busy}>Save account</button>
        </div>
      </form>
    </Modal>
  );
}
