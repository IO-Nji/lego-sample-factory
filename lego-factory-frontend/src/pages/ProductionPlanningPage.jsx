import { useState, useEffect, useMemo } from "react";
import { useAuth } from "../context/AuthContext";
import api from "../api/api";
import "../styles/DashboardStandard.css";

/**
 * Product name mapping
 * Maps itemId to product variant name
 */
const PRODUCT_NAMES = {
  1: { name: "Technic Truck Yellow" },
  2: { name: "Technic Truck Red" },
  3: { name: "Creator House" },
  4: { name: "Friends Cafe" },
};

function ProductionPlanningPage() {
  const { session } = useAuth();
  const [productionOrders, setProductionOrders] = useState([]);
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [schedulesLoading, setSchedulesLoading] = useState(false);

  // Planning form state
  const [orderNumber, setOrderNumber] = useState("");
  const [lineItems, setLineItems] = useState([
    { itemId: 1001, itemName: "Module A", quantity: 1, workstationType: "MANUFACTURING", estimatedDuration: 30 },
    { itemId: 2001, itemName: "Final Assembly", quantity: 1, workstationType: "ASSEMBLY", estimatedDuration: 40 },
  ]);
  const [planning, setPlanning] = useState(false);
  const [planResult, setPlanResult] = useState(null);
  const [createdControlOrders, setCreatedControlOrders] = useState(null);

  // Helper: Plan from Customer Order
  const [coIdInput, setCoIdInput] = useState("");

  // Schedules filtering + pagination
  const [schedFrom, setSchedFrom] = useState("");
  const [schedTo, setSchedTo] = useState("");
  const [schedWsFilter, setSchedWsFilter] = useState("ALL");
  const [schedPage, setSchedPage] = useState(1);
  const [schedPageSize, setSchedPageSize] = useState(10);

  useEffect(() => {
    fetchProductionOrders();
    fetchSchedules();
    // ENHANCED: Refresh every 5 seconds for real-time updates
    const interval = setInterval(() => {
      fetchProductionOrders();
      fetchSchedules();
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchProductionOrders = async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch production control orders from the API
      const response = await api.get("/production-control-orders");
      setProductionOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError(
        "Failed to load production orders: " +
          (err.response?.data?.message || err.message)
      );
      console.error("Production orders fetch error:", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSchedules = async () => {
    setSchedulesLoading(true);
    try {
      const resp = await api.get("/simal/scheduled-orders");
      setSchedules(Array.isArray(resp.data) ? resp.data : []);
    } catch (err) {
      // Non-fatal; surface error in shared error banner
      setError(
        "Failed to load schedules: " + (err.response?.data?.message || err.message)
      );
    } finally {
      setSchedulesLoading(false);
    }
  };

  const planFromCustomerOrder = async () => {
    if (!coIdInput.trim()) {
      setError("Enter a Customer Order ID to plan from");
      return;
    }
    try {
      const resp = await api.get(`/customer-orders/${encodeURIComponent(coIdInput.trim())}`);
      const order = resp.data || {};
      const items = Array.isArray(order.orderItems) ? order.orderItems : [];

      if (items.length === 0) {
        setError("Customer Order has no items; cannot derive a plan");
        return;
      }

      const derived = [];
      items.forEach((it, idx) => {
        const qty = Number(it.quantity) || 1;
        derived.push({
          itemId: it.itemId,
          itemName: `Manufacture for ${it.itemType || 'ITEM'} ${it.itemId}`,
          quantity: qty,
          workstationType: "MANUFACTURING",
          estimatedDuration: Math.max(15, 15 * qty),
          sequence: (idx + 1) * 2 - 1,
        });
        derived.push({
          itemId: it.itemId,
          itemName: `Final Assembly for ${it.itemType || 'ITEM'} ${it.itemId}`,
          quantity: qty,
          workstationType: "ASSEMBLY",
          estimatedDuration: Math.max(20, 20 * qty),
          sequence: (idx + 1) * 2,
        });
      });

      setOrderNumber(order.orderNumber || `CO-${coIdInput.trim()}`);
      setLineItems(derived);

      // Auto-plan immediately
      setPlanning(true);
      const payload = { orderNumber: order.orderNumber || `CO-${coIdInput.trim()}`, lineItems: derived };
      const scheduleResp = await api.post("/simal/schedule", payload);
      setPlanResult(scheduleResp.data);
      setCreatedControlOrders(null);
      fetchSchedules();
    } catch (err) {
      setError("Failed to plan from Customer Order: " + (err.response?.data?.message || err.message));
    } finally {
      setPlanning(false);
    }
  };

  const addLine = () => {
    setLineItems((prev) => [
      ...prev,
      { itemId: Date.now() % 100000, itemName: "New Task", quantity: 1, workstationType: "MANUFACTURING", estimatedDuration: 20 },
    ]);
  };

  const removeLine = (idx) => {
    setLineItems((prev) => prev.filter((_, i) => i !== idx));
  };

  const updateLine = (idx, patch) => {
    setLineItems((prev) => prev.map((l, i) => (i === idx ? { ...l, ...patch } : l)));
  };

  const submitPlan = async () => {
    if (!orderNumber.trim()) {
      setError("Order number is required");
      return;
    }
    setPlanning(true);
    setError(null);
    setPlanResult(null);
    try {
      const payload = {
        orderNumber,
        lineItems: lineItems.map((l, i) => ({
          itemId: l.itemId,
          itemName: l.itemName,
          quantity: Number(l.quantity) || 1,
          workstationType: l.workstationType,
          estimatedDuration: Number(l.estimatedDuration) || 20,
          sequence: i + 1,
        })),
      };
      const resp = await api.post("/simal/schedule", payload);
      setPlanResult(resp.data);
      fetchSchedules();
    } catch (err) {
      setError("Failed to plan schedule: " + (err.response?.data?.message || err.message));
    } finally {
      setPlanning(false);
    }
  };

  const createControlOrders = async (scheduleId) => {
    try {
      const resp = await api.post(`/simal/scheduled-orders/${scheduleId}/create-control-orders`);
      const payload = resp?.data || {};
      setCreatedControlOrders(payload.controlOrdersCreated || payload.controlOrders || null);
      fetchSchedules();
    } catch (err) {
      setError("Failed to create control orders: " + (err.response?.data?.message || err.message));
    }
  };

  const tasksGrouped = useMemo(() => {
    const grp = {};
    (planResult?.scheduledTasks || []).forEach((t) => {
      const ws = t.workstationName || t.workstationId || "Workstation";
      grp[ws] = grp[ws] || [];
      grp[ws].push(t);
    });
    Object.values(grp).forEach((arr) => arr.sort((a, b) => (a.sequence || 0) - (b.sequence || 0)));
    return grp;
  }, [planResult]);

  const getStatusColor = (status) => {
    const colors = {
      PENDING: "#fbbf24",
      SCHEDULED: "#60a5fa",
      IN_PROGRESS: "#10b981",
      COMPLETED: "#6366f1",
      CANCELLED: "#ef4444",
    };
    return colors[status] || "#9ca3af";
  };

  const getPriorityColor = (priority) => {
    const colors = {
      LOW: "#6366f1",
      MEDIUM: "#f59e0b",
      HIGH: "#ef4444",
    };
    return colors[priority] || "#9ca3af";
  };

  return (
    <section className="dashboard-page">
      <div className="page-header">
        <h2>📋 Production Planning</h2>
        <p>Plan and schedule production workflows across manufacturing and assembly stations</p>
      </div>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: "1rem" }}>
          {error}
        </div>
      )}

      {/* Planning UI */}
      <div className="dashboard-box" style={{ marginBottom: "1rem" }}>
        <div className="box-header box-header-primary">
          <h3>Plan New Schedule</h3>
          <button
            onClick={fetchSchedules}
            disabled={schedulesLoading}
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: "#3b82f6",
              color: "white",
              border: "none",
              borderRadius: "0.375rem",
              cursor: schedulesLoading ? "not-allowed" : "pointer",
              opacity: schedulesLoading ? 0.6 : 1,
              fontSize: "0.875rem",
              fontWeight: "500",
            }}
          >
            {schedulesLoading ? "⟳ Refreshing…" : "⟳ Refresh Schedules"}
          </button>
        </div>
        <div className="box-content">
            <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", alignItems: "center", marginBottom: "0.75rem" }}>
              <label style={{ fontWeight: 600 }}>Plan from Customer Order</label>
              <input value={coIdInput} onChange={(e) => setCoIdInput(e.target.value)} placeholder="Customer Order ID" />
              <button className="secondary-link" onClick={planFromCustomerOrder} disabled={planning}>
                {planning ? "Planning…" : "Load & Plan"}
              </button>
            </div>
          <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", alignItems: "center", marginBottom: "0.75rem" }}>
            <label style={{ fontWeight: 600 }}>Order Number</label>
            <input value={orderNumber} onChange={(e) => setOrderNumber(e.target.value)} placeholder="CO-12345" />
            <button className="primary-link" onClick={submitPlan} disabled={planning}>
              {planning ? "Planning…" : "Plan Schedule"}
            </button>
          </div>
          <table className="products-table" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th>Item</th>
                <th>Qty</th>
                <th>Workstation Type</th>
                <th>Est. Duration (min)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {lineItems.map((l, idx) => (
                <tr key={idx}>
                  <td>
                    <input value={l.itemName} onChange={(e) => updateLine(idx, { itemName: e.target.value })} />
                  </td>
                  <td>
                    <input type="number" min="1" value={l.quantity}
                           onChange={(e) => updateLine(idx, { quantity: e.target.value })} />
                  </td>
                  <td>
                    <select value={l.workstationType}
                            onChange={(e) => updateLine(idx, { workstationType: e.target.value })}>
                      <option value="MANUFACTURING">MANUFACTURING</option>
                      <option value="ASSEMBLY">ASSEMBLY</option>
                    </select>
                  </td>
                  <td>
                    <input type="number" min="5" step="5" value={l.estimatedDuration}
                           onChange={(e) => updateLine(idx, { estimatedDuration: e.target.value })} />
                  </td>
                  <td>
                    <button className="danger-link" onClick={() => removeLine(idx)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {planResult && (
        <div className="dashboard-box" style={{ marginBottom: "1rem" }}>
          <div className="box-header box-header-secondary">
            <h3>Planned Schedule: {planResult.scheduleId}</h3>
            <button className="primary-link" onClick={() => createControlOrders(planResult.scheduleId)}>
              Create Control Orders
            </button>
          </div>
          <div className="box-content">
            <p>Order: <strong>{planResult.orderNumber}</strong> · Status: <strong>{planResult.status}</strong> · Est. complete: {planResult.estimatedCompletionTime}</p>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem", marginTop: "0.5rem" }}>
              {Object.entries(tasksGrouped).map(([ws, tasks]) => (
                <div key={ws} style={{ display: "grid", gridTemplateColumns: "220px 1fr", gap: "0.75rem", alignItems: "center" }}>
                  <div style={{ fontWeight: 600, color: "#0b5394" }}>{ws}</div>
                  <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                    {tasks.map((t) => (
                      <div key={t.taskId} title={`${t.itemName} (${t.quantity})`} style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                        <div style={{ height: 8, width: `${Math.min(100, (t.duration || 20) * 2)}px`, background: "linear-gradient(90deg,#0b5394,#1565c0)", borderRadius: 4 }} />
                        <div style={{ fontSize: "0.85rem", color: "#333" }}>{t.itemName} · {t.duration}m</div>
                        {createdControlOrders && createdControlOrders[ws] && (
                          <a href="/production-control" className="primary-link" style={{ marginLeft: 8 }}>
                            View Order {createdControlOrders[ws]}
                          </a>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="dashboard-box">
        <div className="box-header box-header-primary">
          <h3>Production Control Orders ({productionOrders.length})</h3>
          <button
            onClick={fetchProductionOrders}
            disabled={loading}
            style={{
              padding: "0.5rem 1rem",
              backgroundColor: "#3b82f6",
              color: "white",
              border: "none",
              borderRadius: "0.375rem",
              cursor: loading ? "not-allowed" : "pointer",
              opacity: loading ? 0.6 : 1,
              fontSize: "0.875rem",
              fontWeight: "500",
            }}
          >
            {loading ? "⟳ Refreshing..." : "⟳ Refresh"}
          </button>
        </div>
        <div className="box-content">
          {loading ? (
            <p style={{ textAlign: "center", color: "#666" }}>Loading production orders...</p>
          ) : productionOrders.length === 0 ? (
            <div className="empty-state">
              <p>No production orders available.</p>
              <p style={{ fontSize: "0.9rem", color: "#999" }}>
                Check back soon for incoming production requests.
              </p>
            </div>
          ) : (
            <div className="dashboard-table">
              <table className="products-table" style={{ width: "100%" }}>
                <thead>
                  <tr style={{ borderBottom: "2px solid #e5e7eb", backgroundColor: "#f9fafb" }}>
                    <th style={{ textAlign: "left", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Order ID</th>
                    <th style={{ textAlign: "left", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Items</th>
                    <th style={{ textAlign: "left", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Workstation</th>
                    <th style={{ textAlign: "left", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Status</th>
                    <th style={{ textAlign: "left", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Priority</th>
                    <th style={{ textAlign: "center", padding: "0.75rem", fontWeight: "600", color: "#374151" }}>Qty</th>
                  </tr>
                </thead>
                <tbody>
                  {productionOrders.map((order) => (
                    <tr
                      key={order.id}
                      style={{
                        borderBottom: "1px solid #e5e7eb",
                        backgroundColor: order.status === "IN_PROGRESS" ? "#f0fdf4" : "white",
                      }}
                    >
                      <td style={{ padding: "0.75rem", fontWeight: "500" }}>PO-{order.id}</td>
                      <td style={{ padding: "0.75rem" }}>
                        {order.orderItems && order.orderItems.length > 0 ? (
                          <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                            {order.orderItems.map((item, idx) => {
                              const productName = PRODUCT_NAMES[item.itemId]?.name || item.name || item.itemType || "Item";
                              return (
                              <span
                                key={idx}
                                style={{
                                  fontSize: "0.85rem",
                                  padding: "0.25rem 0.5rem",
                                  backgroundColor: "#f3f4f6",
                                  borderRadius: "0.25rem",
                                  display: "inline-block",
                                  width: "fit-content",
                                }}
                              >
                                <span style={{ fontWeight: "600" }}>{productName}</span>
                                <span style={{ color: "#999", margin: "0 0.25rem" }}>×</span>
                                <span style={{ color: "#059669", fontWeight: "600" }}>{item.quantity}</span>
                                {item.status && (
                                  <span style={{ color: "#666", fontSize: "0.75rem", marginLeft: "0.25rem" }}>
                                    ({item.status})
                                  </span>
                                )}
                              </span>
                              );
                            })}
                          </div>
                        ) : (
                          <span>{order.productName || "Unknown"}</span>
                        )}
                      </td>
                      <td style={{ padding: "0.75rem" }}>WS-{order.assignedWorkstationId || "—"}</td>
                      <td style={{ padding: "0.75rem" }}>
                        <span
                          style={{
                            display: "inline-block",
                            padding: "0.25rem 0.75rem",
                            backgroundColor: getStatusColor(order.status),
                            color: "white",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "500",
                          }}
                        >
                          {order.status}
                        </span>
                      </td>
                      <td style={{ padding: "0.75rem" }}>
                        <span
                          style={{
                            display: "inline-block",
                            padding: "0.25rem 0.75rem",
                            backgroundColor: getPriorityColor(order.priority),
                            color: "white",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "500",
                          }}
                        >
                          {order.priority}
                        </span>
                      </td>
                      <td style={{ padding: "0.75rem", textAlign: "center" }}>{order.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Scheduled Orders with filters and pagination */}
      <div className="dashboard-box" style={{ marginTop: "1rem" }}>
        <div className="box-header box-header-primary">
          <h3>Scheduled Orders ({schedules.length})</h3>
        </div>
        <div className="box-content">
          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
            <label style={{ fontWeight: 600 }}>From</label>
            <input type="datetime-local" value={schedFrom} onChange={(e) => setSchedFrom(e.target.value)} />
            <label style={{ fontWeight: 600 }}>To</label>
            <input type="datetime-local" value={schedTo} onChange={(e) => setSchedTo(e.target.value)} />
            <label style={{ fontWeight: 600 }}>Workstation</label>
            <select value={schedWsFilter} onChange={(e) => setSchedWsFilter(e.target.value)}>
              <option value="ALL">All</option>
              {[...new Set((schedules || []).flatMap(s => (s.scheduledTasks || []).map(t => t.workstationName || t.workstationId)).filter(Boolean))]
                .map(ws => (<option key={ws} value={ws}>{ws}</option>))}
            </select>
            <label style={{ fontWeight: 600 }}>Page</label>
            <input type="number" min={1} value={schedPage} onChange={(e) => setSchedPage(Math.max(1, Number(e.target.value) || 1))} style={{ width: 80 }} />
            <label style={{ fontWeight: 600 }}>Size</label>
            <select value={schedPageSize} onChange={(e) => { setSchedPageSize(Number(e.target.value)); setSchedPage(1); }}>
              {[10, 20, 50].map(s => (<option key={s} value={s}>{s}</option>))}
            </select>
          </div>
          {schedulesLoading ? (
            <div className="loading-state"><p>Loading schedules…</p></div>
          ) : (
            (() => {
              const filtered = (schedules || []).filter(s => {
                const est = s.estimatedCompletionTime || "";
                const wsList = (s.scheduledTasks || []).map(t => t.workstationName || t.workstationId);
                const inRange = (!schedFrom || (est && est >= schedFrom)) && (!schedTo || (est && est <= schedTo));
                const wsOk = schedWsFilter === 'ALL' || wsList.includes(schedWsFilter);
                return inRange && wsOk;
              });
              const start = (schedPage - 1) * schedPageSize;
              const pageItems = filtered.slice(start, start + schedPageSize);
              return (
                <table className="products-table" style={{ width: '100%' }}>
                  <thead>
                    <tr>
                      <th>Schedule ID</th>
                      <th>Order Number</th>
                      <th>Status</th>
                      <th>Tasks</th>
                      <th>Estimated Completion</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pageItems.map((s) => (
                      <tr key={s.scheduleId}>
                        <td>{s.scheduleId}</td>
                        <td>{s.orderNumber}</td>
                        <td>{s.status}</td>
                        <td>{s.scheduledTasks?.length || 0}</td>
                        <td>{s.estimatedCompletionTime}</td>
                        <td>
                          <button className="secondary-link" onClick={() => setPlanResult(s)}>View</button>
                          <button className="primary-link" onClick={() => createControlOrders(s.scheduleId)}>
                            Create Control Orders
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              );
            })()
          )}
        </div>
      </div>
    </section>
  );
}

export default ProductionPlanningPage;
