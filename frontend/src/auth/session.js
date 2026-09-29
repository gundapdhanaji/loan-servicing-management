// Session storage uses the same localStorage keys as the LoanLinq front-end,
// so the behaviour is familiar:
//   AUTH-TOKEN                 { ...get_auth_token response, expiration: <login time in ms> }
//   apex_userid                user id
//   email_for_forgot_password  email used to log in
//   accessed_user              access_account response (roles + accounts)

export const SESSION_LIFETIME_MS = 86400000; // 24 hours, same as the backend token

const KEYS = ["AUTH-TOKEN", "apex_userid", "email_for_forgot_password", "accessed_user", "authentication"];

export function saveSession(tokenResponse, accessAccount) {
  localStorage.setItem("AUTH-TOKEN", JSON.stringify({ ...tokenResponse, expiration: Date.now() }));
  localStorage.setItem("apex_userid", String(tokenResponse.userid));
  localStorage.setItem("email_for_forgot_password", tokenResponse.email);
  localStorage.setItem("accessed_user", JSON.stringify(accessAccount));
  localStorage.setItem(
    "authentication",
    JSON.stringify({
      isAuthenticated: true,
      roles: (accessAccount.role || []).map((r) => r.role_name),
    })
  );
}

/** Returns the current session or null. Never throws. */
export function readSession() {
  try {
    const token = JSON.parse(localStorage.getItem("AUTH-TOKEN") || "null");
    if (!token?.token) return null;
    const account = JSON.parse(localStorage.getItem("accessed_user") || "null");
    const roles = (account?.role || []).map((r) => r.role_name);
    return {
      token: token.token,
      expiration: token.expiration,
      userid: token.userid,
      email: token.email,
      role: roles[0] || token.role,
      accounts: account?.account || [],
    };
  } catch {
    return null;
  }
}

export function clearSession() {
  KEYS.forEach((key) => localStorage.removeItem(key));
}
