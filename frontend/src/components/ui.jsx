import { useEffect, useRef } from "react";
import { label } from "../utils/format";

export function PageHeader({ title, subtitle, actions }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}

const STATUS_TONE = {
  ACTIVE: "good",
  POSTED: "good",
  PAID: "good",
  DEFAULT: "bad",
  RETURNED: "bad",
  OPEN: "warn",
  PENDING: "warn",
  PAID_OFF: "neutral",
  WAIVED: "neutral",
  CANCELLED: "neutral",
};

export function StatusBadge({ status }) {
  return <span className={`badge badge-${STATUS_TONE[status] || "neutral"}`}>{label(status)}</span>;
}

export function Alert({ tone = "error", children }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${tone}`} role={tone === "error" ? "alert" : "status"}>
      {children}
    </div>
  );
}

export function Loading({ text = "Loading…" }) {
  return <p className="muted loading">{text}</p>;
}

export function Empty({ title, children }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {children && <div className="empty-body">{children}</div>}
    </div>
  );
}

/** Renders loading / error / content in one place. */
export function Async({ loading, error, children }) {
  if (loading) return <Loading />;
  if (error) return <Alert>{error}</Alert>;
  return children;
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={active === tab.id ? "tab tab-active" : "tab"}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
          {tab.count !== undefined && <span className="tab-count">{tab.count}</span>}
        </button>
      ))}
    </div>
  );
}

export function Modal({ title, onClose, children }) {
  const ref = useRef(null);
  useEffect(() => {
    const onKey = (e) => e.key === "Escape" && onClose();
    document.addEventListener("keydown", onKey);
    ref.current?.querySelector("input, select, textarea, button")?.focus();
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} ref={ref}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Field({ label: text, hint, children, wide }) {
  return (
    <label className={wide ? "field field-wide" : "field"}>
      <span className="field-label">{text}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

/** A label/value pair list, used for loan terms, bank details... */
export function Facts({ items }) {
  return (
    <dl className="facts">
      {items.map(([term, value]) => (
        <div key={term} className="fact">
          <dt>{term}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function Stat({ label: text, value, note, tone }) {
  return (
    <div className={tone ? `stat stat-${tone}` : "stat"}>
      <span className="stat-label">{text}</span>
      <span className="stat-value">{value}</span>
      {note && <span className="stat-note">{note}</span>}
    </div>
  );
}
