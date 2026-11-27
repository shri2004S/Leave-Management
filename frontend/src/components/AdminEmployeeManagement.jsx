import { useEffect, useMemo, useState } from "react";
import { ADMIN_EMPLOYEES_API_URL, api } from "../apiConfig";

const ROLE_OPTIONS = ["EMPLOYEE", "MANAGER", "USER", "ADMIN"];

const EMPTY_FORM = {
  name: "",
  email: "",
  password: "",
  role: "EMPLOYEE",
  managerId: "",
  department: "",
  designation: "",
  phone: "",
};

export default function AdminEmployeeManagement({ currentUser }) {
  const [users, setUsers] = useState([]);
  const [managers, setManagers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const [editingUser, setEditingUser] = useState(null);
  const [editForm, setEditForm] = useState(EMPTY_FORM);
  const [deleteTarget, setDeleteTarget] = useState(null);

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [usersRes, mgrsRes] = await Promise.all([
        api.get(ADMIN_EMPLOYEES_API_URL),
        api.get(`${ADMIN_EMPLOYEES_API_URL}/managers`),
      ]);
      setUsers(usersRes.data || []);
      setManagers(mgrsRes.data || []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load employee records.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    Promise.all([
      api.get(ADMIN_EMPLOYEES_API_URL),
      api.get(`${ADMIN_EMPLOYEES_API_URL}/managers`),
    ])
      .then(([usersRes, mgrsRes]) => {
        if (!active) return;
        setUsers(usersRes.data || []);
        setManagers(mgrsRes.data || []);
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message || "Failed to load employee records.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleCreate(e) {
    e.preventDefault();
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim().toLowerCase(),
        password: form.password,
        role: form.role,
        managerId: form.managerId ? Number(form.managerId) : null,
        department: form.department.trim(),
        designation: form.designation.trim(),
        phone: form.phone.trim(),
      };
      const res = await api.post(ADMIN_EMPLOYEES_API_URL, payload);
      setSuccess(res.data?.message || "Record created successfully.");
      setForm(EMPTY_FORM);
      setShowCreateForm(false);
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to create employee record.");
    } finally {
      setSaving(false);
    }
  }

  function openEdit(u) {
    setEditingUser(u);
    setEditForm({
      name: u.name || "",
      email: u.email || "",
      password: "",
      role: u.role || "EMPLOYEE",
      managerId: u.managerId ? String(u.managerId) : "",
      department: u.department || "",
      designation: u.designation || "",
      phone: u.phone || "",
    });
    setError("");
  }

  async function handleUpdate(e) {
    e.preventDefault();
    if (!editingUser) return;
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      const payload = {
        name: editForm.name.trim(),
        role: editForm.role,
        managerId: editForm.managerId ? Number(editForm.managerId) : -1,
        department: editForm.department.trim(),
        designation: editForm.designation.trim(),
        phone: editForm.phone.trim(),
      };
      const res = await api.put(`${ADMIN_EMPLOYEES_API_URL}/${editingUser.id}`, payload);
      setSuccess(res.data?.message || "Employee record updated.");
      setEditingUser(null);
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update employee record.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      const res = await api.delete(`${ADMIN_EMPLOYEES_API_URL}/${deleteTarget.id}`);
      setSuccess(res.data?.message || "User deleted successfully.");
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete user record.");
      setDeleteTarget(null);
    } finally {
      setSaving(false);
    }
  }

  const filteredUsers = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    return users.filter((u) => {
      const matchesRole = roleFilter === "ALL" || u.role === roleFilter;
      const matchesSearch =
        !term ||
        (u.name && u.name.toLowerCase().includes(term)) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        (u.department && u.department.toLowerCase().includes(term));
      return matchesRole && matchesSearch;
    });
  }, [users, searchTerm, roleFilter]);

  return (
    <section className="admin-management-container">
      <div className="admin-section-header">
        <div>
          <span className="admin-eyebrow-pill">ADMIN RBAC CONTROL</span>
          <h2>Employee Records &amp; User Management</h2>
          <p className="admin-subtitle-desc">
            Create, update, assign managers/roles, or delete user records across the organization.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
          <button type="button" className="admin-sync-btn" onClick={loadData} disabled={loading}>
            <span>{loading ? "Loading..." : "Refresh"}</span>
          </button>
          <button
            type="button"
            className="superadmin-primary-btn"
            onClick={() => setShowCreateForm((v) => !v)}
          >
            {showCreateForm ? "Close Form" : "+ Add Employee / User"}
          </button>
        </div>
      </div>

      {success && <div className="admin-alert-banner success" role="status"><span>✓ {success}</span></div>}
      {error && <div className="admin-alert-banner error" role="alert"><span>⚠ {error}</span></div>}

      {showCreateForm && (
        <div className="superadmin-card" style={{ marginBottom: "20px" }}>
          <h3 style={{ margin: "0 0 14px", color: "#0f172a" }}>Create New Employee / User Record</h3>
          <form onSubmit={handleCreate} style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(210px, 1fr))", gap: "14px" }}>
            <div>
              <label htmlFor="create-emp-name" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Full Name *</label>
              <input
                id="create-emp-name"
                type="text"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value.replace(/[^A-Za-z\s.'-]/g, "") })}
                placeholder="Jane Smith"
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              />
            </div>
            <div>
              <label htmlFor="create-emp-email" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Email *</label>
              <input
                id="create-emp-email"
                type="email"
                required
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="jane@settribe.com"
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              />
            </div>
            <div>
              <label htmlFor="create-emp-password" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Password *</label>
              <input
                id="create-emp-password"
                type="password"
                required
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Password@123"
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              />
            </div>
            <div>
              <label htmlFor="create-emp-role" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Assigned Role *</label>
              <select
                id="create-emp-role"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              >
                {ROLE_OPTIONS.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="create-emp-manager" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Reporting Manager</label>
              <select
                id="create-emp-manager"
                value={form.managerId}
                onChange={(e) => setForm({ ...form, managerId: e.target.value })}
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              >
                <option value="">-- Unassigned --</option>
                {managers.map((m) => (
                  <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="create-emp-dept" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Department</label>
              <input
                id="create-emp-dept"
                type="text"
                value={form.department}
                onChange={(e) => setForm({ ...form, department: e.target.value })}
                placeholder="Engineering"
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              />
            </div>
            <div>
              <label htmlFor="create-emp-desig" style={{ display: "block", fontSize: "0.82rem", fontWeight: 600, marginBottom: "4px" }}>Designation</label>
              <input
                id="create-emp-desig"
                type="text"
                value={form.designation}
                onChange={(e) => setForm({ ...form, designation: e.target.value })}
                placeholder="Software Engineer"
                style={{ width: "100%", padding: "9px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
              />
            </div>
            <div style={{ display: "flex", alignItems: "flex-end" }}>
              <button type="submit" className="superadmin-primary-btn" disabled={saving} style={{ width: "100%" }}>
                {saving ? "Creating..." : "Create Record"}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="admin-filter-toolbar">
        <div className="admin-search-wrapper">
          <input
            type="text"
            placeholder="Search by name, email, or department..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="admin-dropdown-group">
          <div className="admin-select-field">
            <label htmlFor="empRoleFilter">Role:</label>
            <select id="empRoleFilter" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
              <option value="ALL">All Roles ({users.length})</option>
              {ROLE_OPTIONS.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="admin-table-frame">
        <table className="admin-data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Employee / User</th>
              <th>Role</th>
              <th>Department &amp; Title</th>
              <th>Assigned Manager</th>
              <th>Leave Stats</th>
              <th className="actions-header-th">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.map((u) => {
              const isSelf = currentUser?.email?.toLowerCase() === u.email?.toLowerCase();
              const isSuper = u.email?.toLowerCase() === "superadmin@gmail.com";
              return (
                <tr key={u.id}>
                  <td><span className="admin-id-pill">#{u.id}</span></td>
                  <td>
                    <div className="emp-text-details">
                      <strong className="emp-name-text">{u.name}</strong>
                      <span className="emp-email-text">{u.email}</span>
                    </div>
                  </td>
                  <td><span className="admin-type-badge casual">{u.role}</span></td>
                  <td>
                    <div className="emp-text-details">
                      <strong>{u.department || "-"}</strong>
                      <span style={{ fontSize: "0.78rem", color: "#64748b" }}>{u.designation || "-"}</span>
                    </div>
                  </td>
                  <td>{u.managerName ? `${u.managerName} (${u.managerEmail})` : "Unassigned"}</td>
                  <td>{u.totalRequests || 0} total ({u.pendingRequests || 0} pending)</td>
                  <td className="admin-actions-cell">
                    <div className="admin-action-btn-pair">
                      <button type="button" className="btn-approve" onClick={() => openEdit(u)}>
                        Edit
                      </button>
                      {!isSelf && !isSuper && (
                        <button type="button" className="btn-reject" onClick={() => setDeleteTarget(u)}>
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {editingUser && (
        <div className="modal-screen-backdrop" onClick={() => setEditingUser(null)}>
          <div className="modal-dialog-panel" onClick={(e) => e.stopPropagation()} style={{ maxWidth: "460px" }}>
            <h3 style={{ margin: "0 0 14px" }}>Edit User #{editingUser.id}</h3>
            <form onSubmit={handleUpdate} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
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
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Role</label>
                <select
                  value={editForm.role}
                  onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}
                  disabled={editingUser.email?.toLowerCase() === "superadmin@gmail.com"}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                >
                  {ROLE_OPTIONS.map((r) => (
                    <option key={r} value={r}>{r}</option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 600 }}>Assigned Manager</label>
                <select
                  value={editForm.managerId}
                  onChange={(e) => setEditForm({ ...editForm, managerId: e.target.value })}
                  style={{ width: "100%", padding: "8px 10px", borderRadius: "8px", border: "1.5px solid #cbd5e1", boxSizing: "border-box" }}
                >
                  <option value="">-- Unassigned --</option>
                  {managers.filter((m) => m.id !== editingUser.id).map((m) => (
                    <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
                  ))}
                </select>
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
              <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end", marginTop: "6px" }}>
                <button type="button" className="confirm-back-btn" onClick={() => setEditingUser(null)}>Cancel</button>
                <button type="submit" className="superadmin-primary-btn" disabled={saving}>{saving ? "Saving..." : "Save Changes"}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div className="modal-screen-backdrop" onClick={() => setDeleteTarget(null)}>
          <div className="modal-dialog-panel confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <h3>Delete User Record?</h3>
            <p>
              Are you sure you want to permanently delete <strong>{deleteTarget.name}</strong> ({deleteTarget.email})?
            </p>
            <div className="confirm-actions-row">
              <button type="button" className="confirm-back-btn" onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button type="button" className="confirm-decision-btn reject-btn" onClick={handleDelete} disabled={saving}>
                Yes, Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
