import { useEffect, useState } from "react";
import { MANAGER_API_URL, api } from "../apiConfig";
import { calculateLeaveDays } from "../utils/dateUtils";

export default function ManagerPortal({ currentUser }) {
  const [employees, setEmployees] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [editingEmp, setEditingEmp] = useState(null);
  const [editForm, setEditForm] = useState({ name: "", department: "", designation: "", phone: "" });
  const [saving, setSaving] = useState(false);

  async function loadManagerData() {
    setLoading(true);
    setError("");
    try {
      const [empRes, leaveRes] = await Promise.all([
        api.get(`${MANAGER_API_URL}/employees`),
        api.get(`${MANAGER_API_URL}/leaves`),
      ]);
      setEmployees(empRes.data || []);
      setLeaves(leaveRes.data || []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load assigned team data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    Promise.all([
      api.get(`${MANAGER_API_URL}/employees`),
      api.get(`${MANAGER_API_URL}/leaves`),
    ])
      .then(([empRes, leaveRes]) => {
        if (!active) return;
        setEmployees(empRes.data || []);
        setLeaves(leaveRes.data || []);
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message || "Failed to load assigned team data.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function openEditEmployee(emp) {
    setEditingEmp(emp);
    setEditForm({
      name: emp.name || "",
      department: emp.department || "",
      designation: emp.designation || "",
      phone: emp.phone || "",
    });
    setError("");
    setSuccess("");
  }

  async function handleSaveEmployee(e) {
    e.preventDefault();
    if (!editingEmp) return;
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const res = await api.put(`${MANAGER_API_URL}/employees/${editingEmp.id}`, {
        name: editForm.name.trim(),
        department: editForm.department.trim(),
        designation: editForm.designation.trim(),
        phone: editForm.phone.trim(),
      });
      setSuccess(res.data?.message || "Assigned employee details updated.");
      setEditingEmp(null);
      await loadManagerData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update assigned employee.");
    } finally {
      setSaving(false);
    }
  }

  async function handleLeaveDecision(id, status) {
    setError("");
    setSuccess("");
    try {
      const res = await api.put(`${MANAGER_API_URL}/leaves/${id}/status`, { status });
      setSuccess(res.data?.message || `Leave #${id} marked ${status}.`);
      await loadManagerData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update leave decision.");
    }
  }

  return (
    <section className="admin-management-container">
      <div className="admin-section-header">
        <div>
          <span className="admin-eyebrow-pill">MANAGER TEAM WORKSPACE</span>
          <h2>Assigned Employees &amp; Leave Approvals</h2>
          <p className="admin-subtitle-desc">
            Viewing employees and leave requests assigned to {currentUser?.name || currentUser?.email}.
          </p>
        </div>
        <button type="button" className="admin-sync-btn" onClick={loadManagerData} disabled={loading}>
          <span>{loading ? "Refreshing..." : "Refresh Team Data"}</span>
        </button>
      </div>

      {success && <div className="admin-alert-banner success" role="status"><span>✓ {success}</span></div>}
      {error && <div className="admin-alert-banner error" role="alert"><span>⚠ {error}</span></div>}

      <div className="superadmin-card" style={{ marginBottom: "24px" }}>
        <h3 style={{ margin: "0 0 14px", color: "#0f172a" }}>Assigned Employees ({employees.length})</h3>
        {employees.length === 0 ? (
          <p style={{ color: "#64748b", margin: 0 }}>No employees are currently assigned to you.</p>
        ) : (
          <div className="admin-table-frame">
            <table className="admin-data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Employee</th>
                  <th>Department</th>
                  <th>Designation</th>
                  <th>Phone</th>
                  <th className="actions-header-th">Permitted Edit</th>
                </tr>
              </thead>
              <tbody>
                {employees.map((emp) => (
                  <tr key={emp.id}>
                    <td><span className="admin-id-pill">#{emp.id}</span></td>
                    <td>
                      <div className="emp-text-details">
                        <strong>{emp.name}</strong>
                        <span className="emp-email-text">{emp.email}</span>
                      </div>
                    </td>
                    <td>{emp.department || "-"}</td>
                    <td>{emp.designation || "-"}</td>
                    <td>{emp.phone || "-"}</td>
                    <td className="admin-actions-cell">
                      <button type="button" className="btn-approve" onClick={() => openEditEmployee(emp)}>
                        Edit Permitted Info
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="superadmin-card">
        <h3 style={{ margin: "0 0 14px", color: "#0f172a" }}>Assigned Leave Requests ({leaves.length})</h3>
        {leaves.length === 0 ? (
          <p style={{ color: "#64748b", margin: 0 }}>No leave requests assigned to you yet.</p>
        ) : (
          <div className="admin-table-frame">
            <table className="admin-data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Employee</th>
                  <th>Type</th>
                  <th>Dates &amp; Days</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th className="actions-header-th">Actions</th>
                </tr>
              </thead>
              <tbody>
                {leaves.map((l) => {
                  const days = calculateLeaveDays(l.fromDate, l.toDate);
                  return (
                    <tr key={l.id}>
                      <td><span className="admin-id-pill">#{l.id}</span></td>
                      <td>
                        <div className="emp-text-details">
                          <strong>{l.employeeName}</strong>
                          <span className="emp-email-text">{l.employeeEmail}</span>
                        </div>
                      </td>
                      <td><span className={`admin-type-badge ${String(l.leaveType).toLowerCase()}`}>{l.leaveType}</span></td>
                      <td>
                        <div>{l.fromDate} → {l.toDate}</div>
                        <small style={{ color: "#64748b" }}>{days.workingDays} work days</small>
                      </td>
                      <td>{l.reason}</td>
                      <td><span className={`admin-status-pill ${String(l.status).toLowerCase()}`}>{l.status}</span></td>
                      <td className="admin-actions-cell">
                        {l.status === "PENDING" ? (
                          <div className="admin-action-btn-pair">
                            <button type="button" className="btn-approve" onClick={() => handleLeaveDecision(l.id, "APPROVED")}>
                              Approve
                            </button>
                            <button type="button" className="btn-reject" onClick={() => handleLeaveDecision(l.id, "REJECTED")}>
                              Reject
                            </button>
                          </div>
                        ) : (
                          <span>{l.status}</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {editingEmp && (
        <div className="modal-screen-backdrop" onClick={() => setEditingEmp(null)}>
          <div className="modal-dialog-panel" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "440px" }}>
            <h3 style={{ margin: "0 0 12px" }}>Edit Permitted Employee Info</h3>
            <p style={{ fontSize: "0.82rem", color: "#64748b", margin: "0 0 14px" }}>
              Managers can update name, department, designation, and phone for assigned team members. Role changes require Admin approval.
            </p>
            <form onSubmit={handleSaveEmployee} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Full Name *</label>
                <input
                  type="text"
                  required
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value.replace(/[^A-Za-z\s.'-]/g, "") })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Department</label>
                <input
                  type="text"
                  value={editForm.department}
                  onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Designation</label>
                <input
                  type="text"
                  value={editForm.designation}
                  onChange={(e) => setEditForm({ ...editForm, designation: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Phone</label>
                <input
                  type="text"
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                />
              </div>
              <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end", marginTop: "6px" }}>
                <button type="button" className="confirm-back-btn" onClick={() => setEditingEmp(null)}>Cancel</button>
                <button type="submit" className="superadmin-primary-btn" disabled={saving}>{saving ? "Saving..." : "Save Changes"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
