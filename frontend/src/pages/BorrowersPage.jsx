import { useMemo, useState } from "react";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import { Alert, Async, Empty, Field, Modal, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import { date } from "../utils/format";

const EMPTY = {
  firstName: "", lastName: "", email: "", password: "", phone: "",
  street: "", city: "", state: "", zipCode: "", tinType: "SSN", tin: "",
  sendLateNotices: true, sendPaymentReceipts: true,
};

export default function BorrowersPage() {
  const { isAdmin } = useAuth();
  const borrowers = useApi(endpoints.borrowers.list, { initial: [] });
  const [adding, setAdding] = useState(false);
  const [query, setQuery] = useState("");

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (borrowers.data || []).filter((b) => !q || `${b.fullName} ${b.email} ${b.city}`.toLowerCase().includes(q));
  }, [borrowers.data, query]);

  return (
    <>
      <PageHeader
        title="Borrowers"
        subtitle={`${(borrowers.data || []).length} borrowers`}
        actions={isAdmin && <button type="button" className="button button-primary" onClick={() => setAdding(true)}>Add a borrower</button>}
      />
      <div className="toolbar">
        <input type="search" className="search" placeholder="Search name, email or city" value={query}
               onChange={(e) => setQuery(e.target.value)} aria-label="Search borrowers" />
      </div>
      <Async loading={borrowers.loading} error={borrowers.error}>
        {rows.length === 0 ? (
          <Empty title="No borrowers found." />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Name</th><th>Email</th><th>Phone</th><th>Address</th><th>Tax id</th><th>Notices</th><th>Since</th></tr>
              </thead>
              <tbody>
                {rows.map((b) => (
                  <tr key={b.id}>
                    <td>{b.fullName}</td>
                    <td>{b.email}</td>
                    <td>{b.phone || "—"}</td>
                    <td>{[b.street, b.city, b.state].filter(Boolean).join(", ") || "—"}</td>
                    <td>{b.tinType} {b.tinMasked}</td>
                    <td>
                      {[b.sendPaymentReceipts && "Receipts", b.sendLateNotices && "Late notices"].filter(Boolean).join(", ") || "None"}
                    </td>
                    <td>{date(b.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Async>
      {adding && <AddBorrowerModal onClose={() => setAdding(false)} onSaved={borrowers.reload} />}
    </>
  );
}

function AddBorrowerModal({ onClose, onSaved }) {
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) =>
    setForm({ ...form, [key]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.post(endpoints.borrowers.create, form);
      onSaved();
      onClose();
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title="Add a borrower" onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Alert>{error}</Alert>
        <p className="muted">This also creates the borrower's sign-in.</p>
        <div className="grid grid-2">
          <Field label="First name"><input required value={form.firstName} onChange={set("firstName")} /></Field>
          <Field label="Last name"><input required value={form.lastName} onChange={set("lastName")} /></Field>
          <Field label="Email"><input type="email" required value={form.email} onChange={set("email")} /></Field>
          <Field label="Password" hint="At least 8 characters.">
            <input type="password" minLength={8} required value={form.password} onChange={set("password")} />
          </Field>
          <Field label="Phone"><input value={form.phone} onChange={set("phone")} /></Field>
          <Field label="Street"><input value={form.street} onChange={set("street")} /></Field>
          <Field label="City"><input value={form.city} onChange={set("city")} /></Field>
          <Field label="State"><input value={form.state} onChange={set("state")} /></Field>
          <Field label="ZIP code"><input value={form.zipCode} onChange={set("zipCode")} /></Field>
          <Field label="Tax id type">
            <select value={form.tinType} onChange={set("tinType")}>
              <option value="SSN">SSN (person)</option>
              <option value="EIN">EIN (company)</option>
            </select>
          </Field>
          <Field label="Tax id"><input required value={form.tin} onChange={set("tin")} /></Field>
        </div>
        <label className="check">
          <input type="checkbox" checked={form.sendPaymentReceipts} onChange={set("sendPaymentReceipts")} />
          Email a receipt for every payment
        </label>
        <label className="check">
          <input type="checkbox" checked={form.sendLateNotices} onChange={set("sendLateNotices")} />
          Email late payment notices
        </label>
        <div className="form-actions">
          <button type="button" className="button" onClick={onClose}>Cancel</button>
          <button type="submit" className="button button-primary" disabled={busy}>Add borrower</button>
        </div>
      </form>
    </Modal>
  );
}
