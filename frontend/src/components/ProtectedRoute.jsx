import { Navigate, useLocation } from "react-router-dom";
import { normalizeUserRole } from "../utils/validationUtils";

/**
 * Route guard enforcing authentication and role-based access control on frontend URLs.
 */
export default function ProtectedRoute({
  user,
  allowedRoles,
  requireSuperAdmin = false,
  children,
}) {
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  const normalized = normalizeUserRole(user);
  const isSuperAdmin =
    normalized?.email?.trim().toLowerCase() === "superadmin@gmail.com";

  if (requireSuperAdmin && !isSuperAdmin) {
    return (
      <Navigate
        to="/unauthorized"
        replace
        state={{ attemptedPath: location.pathname }}
      />
    );
  }

  if (
    Array.isArray(allowedRoles) &&
    allowedRoles.length > 0 &&
    !allowedRoles.includes(normalized.role)
  ) {
    return (
      <Navigate
        to="/unauthorized"
        replace
        state={{ attemptedPath: location.pathname }}
      />
    );
  }

  return children;
}
