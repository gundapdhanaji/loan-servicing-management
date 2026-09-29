import { Link } from "react-router-dom";
import endpoints from "../api/endpoints";
import { Async, Empty, PageHeader } from "../components/ui";
import useApi from "../hooks/useApi";
import useDirectory from "../hooks/useDirectory";
import { date, money } from "../utils/format";

/** NSF cases: ACH payments the borrower's bank sent back. */
export default function NsfCasesPage() {
  const cases = useApi(endpoints.nsfCases, { initial: [] });
  const directory = useDirectory();

  return (
    <>
      <PageHeader
        title="Returned payments"
        subtitle="Payments the borrower's bank sent back (NSF). Each one was reversed on the loan and charged a fee."
      />
      <Async loading={cases.loading} error={cases.error}>
        {(cases.data || []).length === 0 ? (
          <Empty title="No payments have been returned." />
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Date</th><th>Borrower</th><th>Loan</th><th>Payment</th><th>Reason</th>
                <th className="num">Amount</th><th className="num">Fee charged</th>
              </tr>
            </thead>
            <tbody>
              {cases.data.map((c) => (
                <tr key={c.id}>
                  <td>{date(c.caseDate)}</td>
                  <td>{directory.borrowerName(c.borrowerId)}</td>
                  <td><Link to={`/loans/${c.loanId}`} className="link">Loan #{c.loanId}</Link></td>
                  <td>#{c.paymentId}</td>
                  <td>{c.reason} <span className="cell-code">{c.returnCode}</span></td>
                  <td className="num">{money(c.amount)}</td>
                  <td className="num">{money(c.feeCharged)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
    </>
  );
}
