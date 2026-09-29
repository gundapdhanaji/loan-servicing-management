import { createContext, useCallback, useContext, useMemo, useState } from "react";
import api from "../api/client";
import endpoints from "../api/endpoints";
import { clearSession, readSession, saveSession } from "./session";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readSession());

  // Two steps, like the LoanLinq login page:
  //   1. get_auth_token  -> token + userid
  //   2. access_account  -> roles + accounts
  const login = useCallback(async (email, password) => {
    clearSession(); // an old session's headers must not be sent with the new login
    const { data: token } = await api.post(endpoints.auth.getAuthToken, {
      username: email,
      password,
    });
    const { data: account } = await api.post(
      endpoints.auth.accessAccount,
      { userid: token.userid },
      { headers: { jwt: token.token, user: JSON.stringify({ email: token.email, userId: String(token.userid) }) } }
    );
    saveSession(token, account);
    const next = readSession();
    setSession(next);
    return next;
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  const value = useMemo(() => {
    const role = session?.role;
    return {
      session,
      role,
      isStaff: role === "ADMIN" || role === "CSR",
      isAdmin: role === "ADMIN",
      /** For a lender or borrower: their own lender/borrower id (from access_account). */
      accountId: session?.accounts?.[0]?.recid ?? null,
      accountName: session?.accounts?.[0]?.name ?? session?.email,
      login,
      logout,
    };
  }, [session, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
