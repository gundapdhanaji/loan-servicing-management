import { useState } from "react";
import api, { errorMessage } from "../api/client";
import endpoints from "../api/endpoints";
import { Alert, Async, Facts, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import { date } from "../utils/format";

const JOBS = [
  ["ach-returns", "Process bank returns", "Bounced payments (account ending 0000) are reversed and charged an NSF fee."],
  ["late-charges", "Charge late fees", "Loans past their grace period get a late fee, once per installment."],
  ["defaults", "Flag defaults", "Loans 31+ days past due move to Default."],
  ["disbursements", "Pay lenders", "Pending lender shares are sent by (fake) ACH."],
  ["run-all", "Run all nightly jobs", "The four jobs above, in the nightly order."],
];

/**
 * Local-only tools from the backend's devtools module (/api/v1/dev/**).
 * Move the app's date forward to test late fees and defaults without waiting.
 */
export default function DevToolsPage() {
  const clock = useApi(endpoints.dev.clock);
  const [days, setDays] = useState(10);
  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(null);

  const call = async (key, url) => {
    setBusy(key);
    setError(null);
    try {
      const { data } = await api.post(url);
      return data;
    } catch (err) {
      setError(errorMessage(err));
      return null;
    } finally {
      setBusy(null);
    }
  };

  const moveClock = async (url) => {
    const data = await call("clock", url);
    if (data) clock.setData(data);
  };

  const runJob = async (name, title) => {
    const data = await call(name, endpoints.dev.job(name));
    if (data) setResults({ title, data });
  };

  return (
    <>
      <PageHeader title="Dev tools" subtitle="Only available when the backend runs with the local profile." />
      <Alert>{error}</Alert>

      <div className="two-col">
        <section className="panel">
          <h2>App date</h2>
          <Async loading={clock.loading} error={clock.error}>
            {clock.data && (
              <>
                <p className="dev-date">{date(clock.data.today)}</p>
                <p className="muted">
                  {clock.data.offsetDays === 0 ? "Same as the real date." : `${clock.data.offsetDays} days ahead of the real date.`}
                </p>
              </>
            )}
          </Async>
          <div className="inline-form">
            <label className="field">
              <span className="field-label">Move forward by (days)</span>
              <input type="number" min="1" value={days} onChange={(e) => setDays(e.target.value)} />
            </label>
            <button type="button" className="button button-primary" disabled={busy === "clock"}
                    onClick={() => moveClock(endpoints.dev.advanceClock(Number(days) || 1))}>
              Move date
            </button>
            <button type="button" className="button" disabled={busy === "clock"}
                    onClick={() => moveClock(endpoints.dev.resetClock)}>
              Back to today
            </button>
          </div>
        </section>

        <section className="panel">
          <h2>Nightly jobs</h2>
          <ul className="job-list">
            {JOBS.map(([name, title, text]) => (
              <li key={name}>
                <div>
                  <p className="job-title">{title}</p>
                  <p className="muted">{text}</p>
                </div>
                <button type="button" className={name === "run-all" ? "button button-primary" : "button"}
                        disabled={busy === name} onClick={() => runJob(name, title)}>
                  {busy === name ? "Running…" : "Run"}
                </button>
              </li>
            ))}
          </ul>
          {results && (
            <div className="job-result">
              <p className="job-title">{results.title}: done</p>
              <Facts items={Object.entries(results.data).map(([k, v]) => [k.replace(/([A-Z])/g, " $1").toLowerCase(), String(v)])} />
            </div>
          )}
        </section>
      </div>
    </>
  );
}
