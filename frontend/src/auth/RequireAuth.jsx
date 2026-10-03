import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

/** Only renders the child routes when logged in (and, optionally, with one of the given roles). */
export default function RequireAuth({ roles }) {
  const { session, role } = useAuth();
  const location = useLocation();

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
