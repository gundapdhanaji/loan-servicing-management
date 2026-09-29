import axios from "axios";
import { clearSession, readSession, SESSION_LIFETIME_MS } from "../auth/session";

// Empty = same origin; the Vite dev server proxies /api to the backend.
export const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

const api = axios.create({
  baseURL: API_BASE,
  headers: { "Content-Type": "application/json" },
});

// Same idea as the LoanLinq axios interceptor:
//   - the token goes in the "jwt" header
//   - a "user" header carries {email, userId}
//   - a session older than 24 hours is thrown away
api.interceptors.request.use((config) => {
  const session = readSession();
  if (session) {
    if (Date.now() - session.expiration > SESSION_LIFETIME_MS) {
      clearSession();
      window.location.assign("/login?expired=1");
      return Promise.reject(new axios.CanceledError("Session expired"));
    }
    config.headers["jwt"] = session.token;
    config.headers["user"] = JSON.stringify({
      email: session.email,
      userId: String(session.userid),
    });
  }
  return config;
});

// A 401 on any call except login means the token is no longer accepted: go back to login.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || "";
    if (status === 401 && !url.includes("get_auth_token")) {
      clearSession();
      window.location.assign("/login?expired=1");
    }
    return Promise.reject(error);
  }
);

/** Turns an axios error into one readable sentence, using the backend's ApiError body. */
export function errorMessage(error) {
  if (axios.isCancel(error)) return "Request cancelled.";
  const data = error?.response?.data;
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.entries(data.fieldErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join("; ");
  }
  if (data?.message) return data.message;
  if (error?.response?.status === 403) return "You don't have access to this.";
  if (!error?.response) return "Can't reach the server. Is the backend running on port 8080?";
  return error.message || "Something went wrong.";
}

export default api;
