import { useEffect, useState } from "react";
import { PROFILE_API_URL, api } from "../apiConfig";
import { validateName, getInitials } from "../utils/validationUtils";

export default function ProfilePage({ user, onProfileUpdated }) {
  const [profile, setProfile] = useState(user || null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [name, setName] = useState(user?.name || "");
  const [phone, setPhone] = useState(user?.phone || "");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    async function loadProfile() {
      setLoading(true);
      setError("");
      try {
        const res = await api.get(PROFILE_API_URL);
        if (active && res.data) {
          setProfile(res.data);
          setName(res.data.name || "");
          setPhone(res.data.phone || "");
        }
      } catch (err) {
        if (active) {
          setError(err.response?.data?.message || "Could not load profile from server.");
        }
      } finally {
        if (active) setLoading(false);
      }
    }
    loadProfile();
    return () => {
      active = false;
    };
  }, []);

  async function handleSave(e) {
    e.preventDefault();
    setError("");
    setSuccess("");

    const nameErr = validateName(name);
    if (nameErr) {
      setError(nameErr);
      return;
    }

    setSaving(true);
    try {
      const res = await api.put(PROFILE_API_URL, {
        name: name.trim().replace(/\s+/g, " "),
        phone: phone.trim(),
      });
      setProfile(res.data);
      setSuccess("Personal information updated successfully.");
      if (onProfileUpdated) {
        onProfileUpdated(res.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update profile.");
    } finally {
      setSaving(false);
    }
  }

  const display = profile || user || {};
  const initials = getInitials(display.name, display.email);
  const permissions = Array.isArray(display.permissions) ? display.permissions : [];

  return (
    <section className="superadmin-page-container">
      <div className="superadmin-card">
        <div style={{ display: "flex", alignItems: "center", gap: "18px", flexWrap: "wrap", marginBottom: "20px" }}>
          <div
            style={{
              width: "64px",
              height: "64px",
              borderRadius: "50%",
              background: "linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)",
              color: "#fff",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "1.4rem",
              fontWeight: 700,
            }}
          >
            {initials}
          </div>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
              <h2 style={{ margin: 0, color: "#0f172a", fontSize: "1.35rem" }}>{display.name || "User"}</h2>
              <span className="admin-member-badge standard">{display.role || "USER"}</span>
            </div>
            <p style={{ margin: "4px 0 0", color: "#64748b", fontSize: "0.9rem" }}>{display.email}</p>
          </div>
        </div>

        {loading ? (
          <p style={{ color: "#64748b" }}>Loading profile details...</p>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "12px", marginBottom: "24px" }}>
            <div style={{ padding: "12px 14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: 600 }}>USER ID</div>
              <div style={{ fontSize: "0.95rem", color: "#0f172a", fontWeight: 700 }}>#{display.userId || display.id || "-"}</div>
            </div>
            <div style={{ padding: "12px 14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: 600 }}>DEPARTMENT</div>
              <div style={{ fontSize: "0.95rem", color: "#0f172a", fontWeight: 700 }}>{display.department || "Not Assigned"}</div>
            </div>
            <div style={{ padding: "12px 14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: 600 }}>DESIGNATION</div>
              <div style={{ fontSize: "0.95rem", color: "#0f172a", fontWeight: 700 }}>{display.designation || display.role || "Member"}</div>
            </div>
            <div style={{ padding: "12px 14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.75rem", color: "#64748b", fontWeight: 600 }}>ASSIGNED MANAGER</div>
              <div style={{ fontSize: "0.95rem", color: "#0f172a", fontWeight: 700 }}>
                {display.managerName ? `${display.managerName} (${display.managerEmail})` : "None"}
              </div>
            </div>
          </div>
        )}

        {error && <div className="superadmin-alert error" role="alert"><span>{error}</span></div>}
        {success && <div className="superadmin-alert success" role="status"><span>{success}</span></div>}

        <form onSubmit={handleSave} style={{ maxWidth: "520px" }}>
          <h3 style={{ margin: "0 0 12px", fontSize: "1.05rem", color: "#0f172a" }}>Update Personal Information</h3>
          <div style={{ marginBottom: "14px" }}>
            <label htmlFor="profile-name-input" style={{ display: "block", fontSize: "0.84rem", fontWeight: 600, marginBottom: "6px", color: "#334155" }}>
              Full Name (letters only) *
            </label>
            <input
              id="profile-name-input"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value.replace(/[^A-Za-z\s.'-]/g, ""))}
              required
              style={{ width: "100%", padding: "10px 12px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
            />
          </div>

          <div style={{ marginBottom: "16px" }}>
            <label htmlFor="profile-phone-input" style={{ display: "block", fontSize: "0.84rem", fontWeight: 600, marginBottom: "6px", color: "#334155" }}>
              Phone Number
            </label>
            <input
              id="profile-phone-input"
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="e.g. +91 98765 43210"
              style={{ width: "100%", padding: "10px 12px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
            />
          </div>

          <button type="submit" className="superadmin-primary-btn" disabled={saving}>
            {saving ? "Saving..." : "Save Profile Changes"}
          </button>
        </form>

        {permissions.length > 0 && (
          <div style={{ marginTop: "24px", paddingTop: "18px", borderTop: "1px solid #e2e8f0" }}>
            <h4 style={{ margin: "0 0 10px", fontSize: "0.9rem", color: "#475569" }}>Granted Role Permissions ({permissions.length})</h4>
            <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
              {permissions.map((perm) => (
                <span
                  key={perm}
                  style={{
                    fontSize: "0.75rem",
                    padding: "4px 10px",
                    borderRadius: "999px",
                    background: "#eff6ff",
                    color: "#1d4ed8",
                    border: "1px solid #bfdbfe",
                    fontWeight: 600,
                  }}
                >
                  {perm}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
