import { useEffect, useState } from "react";
import { LEAVES_API_URL, api } from "../apiConfig";

export default function EmployeeReports() {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadReport() {
    setLoading(true);
    setError("");
    try {
      const res = await api.get(`${LEAVES_API_URL}/report`);
      setReport(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load personal leave report.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;
    api.get(`${LEAVES_API_URL}/report`)
      .then((res) => {
        if (active) setReport(res.data);
      })
      .catch((err) => {
        if (active) setError(err.response?.data?.message || "Failed to load personal leave report.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function downloadCsv() {
    if (!report?.history?.length) return;
    const headers = ["ID", "Leave Type", "From Date", "To Date", "Working Days", "Status", "Approver", "Reason"];
    const rows = report.history.map((item) => [
      item.id,
      item.leaveType,
      item.fromDate,
      item.toDate,
      item.workingDays,
      item.status,
      `"${String(item.assignedAdmin || "").replace(/"/g, '""')}"`,
      `"${String(item.reason || "").replace(/"/g, '""')}"`,
    ]);
    const csv = [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "my-leave-report.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  if (loading) {
    return <div className="superadmin-card"><p>Generating your personal leave report...</p></div>;
  }

  if (error) {
    return (
      <div className="superadmin-card">
        <div className="superadmin-alert error" role="alert"><span>{error}</span></div>
      </div>
    );
  }

  const categories = report?.workingDaysByCategory || {};
  const history = report?.history || [];

  return (
    <section className="admin-management-container">
      <div className="admin-section-header">
        <div>
          <span className="admin-eyebrow-pill">PERSONAL LEAVE ANALYTICS</span>
          <h2>My Leave Summary &amp; Report</h2>
          <p className="admin-subtitle-desc">
            Personal attendance breakdown for {report?.employeeName} ({report?.employeeEmail}).
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px" }}>
          <button type="button" className="admin-sync-btn" onClick={loadReport}>Refresh</button>
          <button type="button" className="superadmin-primary-btn" onClick={downloadCsv} disabled={!history.length}>
            Export CSV
          </button>
        </div>
      </div>

      <div className="admin-metrics-grid">
        <div className="admin-metric-card stat-total">
          <div className="metric-text-box">
            <span className="metric-tag">TOTAL SUBMISSIONS</span>
            <h3 className="metric-number">{report?.totalRequests ?? 0}</h3>
          </div>
        </div>
        <div className="admin-metric-card stat-approved">
          <div className="metric-text-box">
            <span className="metric-tag">APPROVED WORK DAYS</span>
            <h3 className="metric-number">{report?.totalApprovedWorkingDays ?? 0}</h3>
          </div>
        </div>
        <div className="admin-metric-card stat-pending">
          <div className="metric-text-box">
            <span className="metric-tag">PENDING REQUESTS</span>
            <h3 className="metric-number">{report?.pendingRequests ?? 0}</h3>
          </div>
        </div>
        <div className="admin-metric-card stat-rejected">
          <div className="metric-text-box">
            <span className="metric-tag">REJECTED REQUESTS</span>
            <h3 className="metric-number">{report?.rejectedRequests ?? 0}</h3>
          </div>
        </div>
      </div>

      <div className="superadmin-card" style={{ marginBottom: "20px" }}>
        <h3 style={{ margin: "0 0 12px" }}>Approved Working Days by Category</h3>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))", gap: "12px" }}>
          {Object.entries(categories).map(([cat, days]) => (
            <div key={cat} style={{ padding: "12px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
              <div style={{ fontSize: "0.78rem", color: "#64748b", fontWeight: 700 }}>{cat}</div>
              <div style={{ fontSize: "1.25rem", fontWeight: 800, color: "#0f172a" }}>{days} days</div>
            </div>
          ))}
        </div>
      </div>

      <div className="admin-table-frame">
        <table className="admin-data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Category</th>
              <th>Dates</th>
              <th>Working Days</th>
              <th>Approver</th>
              <th>Status</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {history.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: "center", color: "#64748b" }}>No leave records found.</td></tr>
            ) : (
              history.map((item) => (
                <tr key={item.id}>
                  <td><span className="admin-id-pill">#{item.id}</span></td>
                  <td><span className={`admin-type-badge ${String(item.leaveType).toLowerCase()}`}>{item.leaveType}</span></td>
                  <td>{item.fromDate} → {item.toDate}</td>
                  <td>{item.workingDays}</td>
                  <td>{item.assignedAdmin || "-"}</td>
                  <td><span className={`admin-status-pill ${String(item.status).toLowerCase()}`}>{item.status}</span></td>
                  <td>{item.reason}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
