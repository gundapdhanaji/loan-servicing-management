import { useState } from "react";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Alert, Async, Empty, Field, Modal, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import { date } from "../utils/format";

const EMPTY = { name: "", email: "", password: "", phone: "", taxId: "", routingNumber: "", accountNumber: "" };

export default function LendersPage() {
  const { isAdmin } = useAuth();
  const lenders = useApi(endpoints.lenders.list, { initial: [] });
  const [adding, setAdding] = useState(false);

  return (
    <>
      <PageHeader
        title="Lenders"
        subtitle="Investors who fund loans and receive a share of every payment."
        actions={isAdmin && <button type="button" className="button button-primary" onClick={() => setAdding(true)}>Add a lender</button>}
      />
      <Async loading={lenders.loading} error={lenders.error}>
        {(lenders.data || []).length === 0 ? (
          <Empty title="No lenders yet." />
        ) : (
          <table className="table">
            <thead>
              <tr><th>Name</th><th>Email</th><th>Phone</th><th>Tax id</th><th>Payout account</th><th>Since</th></tr>
            </thead>
            <tbody>
              {lenders.data.map((l) => (
                <tr key={l.id}>
                  <td>{l.name}</td>
                  <td>{l.email}</td>
                  <td>{l.phone || "—"}</td>
                  <td>{l.taxIdMasked}</td>
                  <td>{l.accountNumberMasked}, routing {l.routingNumber}</td>
                  <td>{date(l.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
      {adding && <AddLenderModal onClose={() => setAdding(false)} onSaved={lenders.reload} />}
    </>
  );
}

function AddLenderModal({ onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(endpoints.lenders.create, form);
      onSaved();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title="Add a lender" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Alert>{error}</Alert>
        <p className="muted">This also creates the lender's sign-in. Payouts go to the bank account below.</p>
        <div className="grid grid-2">
          <Field label="Lender name" wide><input required value={form.name} onChange={set("name")} /></Field>
          <Field label="Email"><input type="email" required value={form.email} onChange={set("email")} /></Field>
          <Field label="Password" hint="At least 8 characters.">
            <input type="password" minLength={8} required value={form.password} onChange={set("password")} />
          </Field>
          <Field label="Phone"><input value={form.phone} onChange={set("phone")} /></Field>
          <Field label="Tax id (EIN)"><input required value={form.taxId} onChange={set("taxId")} /></Field>
          <Field label="Routing number">
            <input required inputMode="numeric" pattern="\d{9}" maxLength={9} value={form.routingNumber} onChange={set("routingNumber")} />
          </Field>
          <Field label="Account number">
            <input required inputMode="numeric" pattern="\d{4,17}" maxLength={17} value={form.accountNumber} onChange={set("accountNumber")} />
          </Field>
        </div>
        <div className="form-actions">
          <button type="button" className="button" onClick={onClose}>Cancel</button>
          <button type="submit" className="button button-primary" disabled={busy}>Add lender</button>
        </div>
      </form>
    </Modal>
  );
}
