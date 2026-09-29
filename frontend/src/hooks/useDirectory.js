import { useMemo } from "react";
import endpoints from "../api/endpoints";
import { useAuth } from "../auth/AuthContext";
import useApi from "./useApi";

/**
 * Names for borrower and lender ids.
 * Loans only carry ids (the backend modules are separate), so staff screens
 * look the names up here. Lenders and borrowers can't list other people, so
 * for them this simply returns "#id" (or their own name).
 */
export default function useDirectory() {
  const { isStaff, role, accountId, accountName } = useAuth();
  const borrowers = useApi(isStaff ? endpoints.borrowers.list : null, { initial: [] });
  const lenders = useApi(isStaff ? endpoints.lenders.list : null, { initial: [] });

  return useMemo(() => {
    const borrowerById = new Map((borrowers.data || []).map((b) => [b.id, b]));
    const lenderById = new Map((lenders.data || []).map((l) => [l.id, l]));
    return {
      loading: borrowers.loading || lenders.loading,
      borrowers: borrowers.data || [],
      lenders: lenders.data || [],
      borrowerName(id) {
        if (role === "BORROWER" && id === accountId) return accountName;
        return borrowerById.get(id)?.fullName || `Borrower #${id}`;
      },
      lenderName(id) {
        if (role === "LENDER" && id === accountId) return accountName;
        return lenderById.get(id)?.name || `Lender #${id}`;
      },
      reload() {
        borrowers.reload();
        lenders.reload();
      },
    };
  }, [borrowers, lenders, role, accountId, accountName]);
}
