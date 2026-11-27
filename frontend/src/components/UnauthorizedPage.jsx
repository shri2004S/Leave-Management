import { useNavigate, useLocation } from "react-router-dom";
import { getDefaultRouteForRole } from "../utils/validationUtils";

export default function UnauthorizedPage({ user }) {
  const navigate = useNavigate();
  const location = useLocation();
  const attemptedPath = location.state?.attemptedPath;
  const fallbackRoute = getDefaultRouteForRole(user);

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        minHeight: "380px",
        padding: "24px",
      }}
    >
      <div
        style={{
          background: "#ffffff",
          border: "1px solid #fecaca",
          borderRadius: "16px",
          padding: "36px",
          maxWidth: "480px",
          width: "100%",
          textAlign: "center",
          boxShadow: "0 8px 24px rgba(15, 23, 42, 0.06)",
        }}
      >
        <div
          style={{
            width: "56px",
            height: "56px",
            margin: "0 auto 16px",
            borderRadius: "50%",
            background: "#fef2f2",
            color: "#dc2626",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "1.6rem",
            fontWeight: 800,
          }}
        >
          403
        </div>
        <h2 style={{ margin: "0 0 8px", color: "#0f172a", fontSize: "1.35rem" }}>
          Access Denied (Unauthorized)
        </h2>
        <p style={{ margin: "0 0 18px", color: "#64748b", fontSize: "0.92rem", lineHeight: 1.5 }}>
          Your current role (<strong>{user?.role || "GUEST"}</strong>) does not have permission to access{" "}
          {attemptedPath ? <code>{attemptedPath}</code> : "this page"}.
        </p>
        <button
          type="button"
          onClick={() => navigate(fallbackRoute, { replace: true })}
          style={{
            padding: "10px 20px",
            borderRadius: "8px",
            border: "none",
            background: "#2563eb",
            color: "#ffffff",
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          Go to My Allowed Dashboard
        </button>
      </div>
    </div>
  );
}
