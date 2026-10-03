// Formatting helpers. Amounts from the backend are numbers (BigDecimal serialised as JSON numbers).

const usd = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });

export function money(value) {
  if (value === null || value === undefined || value === "") return "—";
  return usd.format(Number(value));
}

export function percent(value, digits = 2) {
  if (value === null || value === undefined) return "—";
  return `${Number(value).toFixed(digits)}%`;
}

/** "2026-10-09" -> "Oct 9, 2026". Parsed as a plain date so time zones can't shift the day. */
export function date(value) {
  if (!value) return "—";
  const [y, m, d] = String(value).slice(0, 10).split("-").map(Number);
  if (!y || !m || !d) return String(value);
  return new Date(y, m - 1, d).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

/** "LATE_FEE" -> "Late fee" */
export function label(value) {
  if (!value) return "—";
  const text = String(value).replace(/_/g, " ").toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function sum(items, pick) {
  return items.reduce((total, item) => total + Number(pick(item) || 0), 0);
}

/** Unique key for a payment request, so a double click or a retry never charges twice. */
export function newIdempotencyKey() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
