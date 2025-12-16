import { useEffect, useState } from "react";
import api from "../api/api";

function WebhooksAdminPage() {
  const [subs, setSubs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [form, setForm] = useState({ eventType: "ANY", targetUrl: "", secret: "" });

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get("/orders/webhooks");
      setSubs(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const add = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    if (!form.targetUrl) {
      setError("Target URL is required");
      return;
    }
    try {
      await api.post("/orders/webhooks", form);
      setSuccess("Subscription created");
      setForm({ eventType: "ANY", targetUrl: "", secret: "" });
      load();
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    }
  };

  const remove = async (id) => {
    if (!window.confirm("Delete this subscription?")) return;
    setError(null);
    setSuccess(null);
    try {
      await api.delete(`/orders/webhooks/${id}`);
      setSuccess("Subscription deleted");
      load();
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    }
  };

  return (
    <section style={{ padding: "2rem 1rem", maxWidth: 1000, margin: "0 auto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <div>
          <h2 style={{ fontSize: "1.75rem", fontWeight: 700, color: "#0b5394", margin: 0 }}>🔔 Webhooks Admin</h2>
          <p style={{ margin: 0, color: "#555" }}>Subscribe to audit events and manage callbacks</p>
        </div>
        <button onClick={load} disabled={loading} style={{ padding: "0.5rem 1rem", background: "#3b82f6", color: "#fff", border: 0, borderRadius: 6, cursor: loading ? "not-allowed" : "pointer" }}>
          {loading ? "Refreshing…" : "Refresh"}
        </button>
      </div>

      {error && (
        <div className="error-alert" style={{ marginBottom: "1rem" }}>{error}</div>
      )}
      {success && (
        <div className="form-success-details" style={{ marginBottom: "1rem" }}>{success}</div>
      )}

      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200" style={{ marginBottom: "1rem" }}>
        <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
          <h3 className="text-lg font-semibold text-blue-900" style={{ margin: 0 }}>➕ Add Subscription</h3>
        </div>
        <form onSubmit={add} style={{ padding: "1rem", display: "grid", gridTemplateColumns: "160px 1fr 1fr auto", gap: "0.75rem", alignItems: "end" }}>
          <div>
            <label style={{ display: "block", fontSize: 12, color: "#555", marginBottom: 4 }}>Event Type</label>
            <select name="eventType" value={form.eventType} onChange={onChange} className="input" style={{ width: "100%", padding: "0.5rem", border: "1px solid #ccc", borderRadius: 6 }}>
              <option value="ANY">ANY</option>
              <option value="CUSTOMER.PENDING">CUSTOMER.PENDING</option>
              <option value="CUSTOMER.CONFIRMED">CUSTOMER.CONFIRMED</option>
              <option value="CUSTOMER.PROCESSING">CUSTOMER.PROCESSING</option>
              <option value="CUSTOMER.COMPLETED">CUSTOMER.COMPLETED</option>
              <option value="CUSTOMER.CANCELLED">CUSTOMER.CANCELLED</option>
            </select>
          </div>
          <div>
            <label style={{ display: "block", fontSize: 12, color: "#555", marginBottom: 4 }}>Target URL</label>
            <input name="targetUrl" value={form.targetUrl} onChange={onChange} placeholder="https://example.com/webhook" className="input" style={{ width: "100%", padding: "0.5rem", border: "1px solid #ccc", borderRadius: 6 }} />
          </div>
          <div>
            <label style={{ display: "block", fontSize: 12, color: "#555", marginBottom: 4 }}>Secret (optional)</label>
            <input name="secret" value={form.secret} onChange={onChange} placeholder="shared secret" className="input" style={{ width: "100%", padding: "0.5rem", border: "1px solid #ccc", borderRadius: 6 }} />
          </div>
          <div>
            <button type="submit" className="primary-link" style={{ marginTop: 0 }}>Create</button>
          </div>
        </form>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200">
        <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
          <h3 className="text-lg font-semibold text-blue-900" style={{ margin: 0 }}>Active Subscriptions</h3>
        </div>
        <div className="overflow-x-auto">
          {subs.length > 0 ? (
            <table className="products-table w-full">
              <thead>
                <tr>
                  <th className="px-4 py-3 text-left">ID</th>
                  <th className="px-4 py-3 text-left">Event Type</th>
                  <th className="px-4 py-3 text-left">Target URL</th>
                  <th className="px-4 py-3 text-left">Active</th>
                  <th className="px-4 py-3 text-left">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {subs.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">{s.id}</td>
                    <td className="px-4 py-3">{s.eventType}</td>
                    <td className="px-4 py-3" style={{ maxWidth: 420, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{s.targetUrl}</td>
                    <td className="px-4 py-3">{String(s.active ?? true)}</td>
                    <td className="px-4 py-3">
                      <div className="actions">
                        <button className="delete-btn" onClick={() => remove(s.id)}>🗑 Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div style={{ padding: "1rem", color: "#666" }}>No subscriptions</div>
          )}
        </div>
      </div>
    </section>
  );
}

export default WebhooksAdminPage;
