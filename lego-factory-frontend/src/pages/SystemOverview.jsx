import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import api from "../api/api";
import PageHeader from "../components/PageHeader";
import "../styles/StandardPage.css";
import "../styles/DashboardStandard.css";
import "../styles/AdminDashboard.css";

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

function SystemOverview() {
  const { session } = useAuth();
  const [workstations, setWorkstations] = useState([]);
  const [orders, setOrders] = useState({
    production: [],
    assembly: [],
    supply: [],
  });
  const [workstationStats, setWorkstationStats] = useState({});
  const [stockAlerts, setStockAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedWorkstation, setSelectedWorkstation] = useState(null);
  const [activeTab, setActiveTab] = useState("overview"); // overview, workstations, orders, supply

  useEffect(() => {
    fetchDashboardData();
    // ENHANCED: Auto-refresh every 5 seconds for real-time updates
    const interval = setInterval(fetchDashboardData, 5000);
    return () => clearInterval(interval);
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch all data in parallel
      const [wsResponse, prodResponse, asmResponse, supResponse, alertsResponse] = await Promise.all([
        api.get("/masterdata/workstations"),
        api.get("/production-control-orders"),
        api.get("/assembly-control-orders"),
        api.get("/supply-orders/warehouse"),
        api.get("/stock/alerts"),
      ]);

      // Process and set all data together
      const wsData = Array.isArray(wsResponse.data) ? wsResponse.data : [];
      const prodData = Array.isArray(prodResponse.data) ? prodResponse.data : [];
      const asmData = Array.isArray(asmResponse.data) ? asmResponse.data : [];
      const supData = Array.isArray(supResponse.data) ? supResponse.data : [];
      const alertsData = Array.isArray(alertsResponse.data) ? alertsResponse.data : [];

      setWorkstations(wsData);
      setStockAlerts(alertsData);
      setOrders({
        production: prodData,
        assembly: asmData,
        supply: supData,
      });

      // Calculate stats with fresh data
      calculateStats(wsData, prodData, asmData, supData);
    } catch (err) {
      setError("Failed to load dashboard data: " + (err.message || "Unknown error"));
      console.error("Dashboard fetch error:", err);
    } finally {
      setLoading(false);
    }
  };

  const calculateStats = (ws, prod, asm, supply) => {
    const stats = {};

    ws.forEach((workstation) => {
      const wsId = workstation.id;
      const prodOrders = Array.isArray(prod) ? prod.filter((o) => o.assignedWorkstationId === wsId) : [];
      const asmOrders = Array.isArray(asm) ? asm.filter((o) => o.assignedWorkstationId === wsId) : [];

      stats[wsId] = {
        workstationId: wsId,
        name: workstation.name || `Workstation ${wsId}`,
        type: workstation.workstationType || "Unknown",
        status: calculateWorkstationStatus(prodOrders, asmOrders),
        activeOrders: prodOrders.filter((o) => o.status === "IN_PROGRESS").length +
                      asmOrders.filter((o) => o.status === "IN_PROGRESS").length,
        completedToday: prodOrders.filter((o) => o.status === "COMPLETED").length +
                        asmOrders.filter((o) => o.status === "COMPLETED").length,
        totalOrders: prodOrders.length + asmOrders.length,
      };
    });

    setWorkstationStats(stats);
  };

  const calculateWorkstationStatus = (prodOrders, asmOrders) => {
    const allOrders = [...prodOrders, ...asmOrders];
    if (allOrders.length === 0) return "idle";
    if (allOrders.some((o) => o.status === "IN_PROGRESS")) return "active";
    if (allOrders.some((o) => o.status === "PENDING" || o.status === "SCHEDULED")) return "waiting";
    return "idle";
  };

  const getStatusColor = (status) => {
    const colors = {
      PENDING: "#FFA500",
      SCHEDULED: "#87CEEB",
      IN_PROGRESS: "#4169E1",
      COMPLETED: "#32CD32",
      REJECTED: "#FF6347",
      CANCELLED: "#808080",
      active: "#4169E1",
      idle: "#90EE90",
      waiting: "#FFD700",
    };
    return colors[status] || "#808080";
  };

  const getPriorityColor = (priority) => {
    const colors = {
      HIGH: "#FF4444",
      MEDIUM: "#FFA500",
      LOW: "#4CAF50",
    };
    return colors[priority] || "#808080";
  };

  if (loading && !workstations.length) {
    return (
      <section className="admin-dashboard">
        <h2>🏭 Factory Admin Dashboard</h2>
        <div className="loading-state">Loading dashboard data...</div>
      </section>
    );
  }

  const totalOrders = orders.production.length + orders.assembly.length;
  const pendingOrdersCount = orders.production.filter((o) => o.status === "PENDING" || o.status === "SCHEDULED").length +
                            orders.assembly.filter((o) => o.status === "PENDING" || o.status === "SCHEDULED").length;
  const completedOrdersCount = orders.production.filter((o) => o.status === "COMPLETED").length +
                              orders.assembly.filter((o) => o.status === "COMPLETED").length;
  const activeWorkstations = Object.values(workstationStats).filter((ws) => ws.status === "active").length;
  const lowStockAlertsCount = stockAlerts.filter((alert) => !alert.resolved).length;
  const supplyPendingCount = orders.supply.filter((o) => o.status === "PENDING").length;

  return (
    <div className="standard-page-container">
      <PageHeader
        title="System Overview"
        subtitle="Real-time monitoring and control of factory operations"
        icon="🏭"
        actions={[
          {
            label: loading ? "Refreshing..." : "Refresh",
            onClick: fetchDashboardData,
            disabled: loading,
            icon: "⟳",
          },
        ]}
      />
      <section className="admin-dashboard">

      {error && <div className="error-alert">{error}</div>}

      {/* KPI Cards */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">Total Orders</div>
          <div className="kpi-value">{totalOrders}</div>
          <div className="kpi-detail">Across all workstations</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Pending Orders</div>
          <div className="kpi-value">{pendingOrdersCount}</div>
          <div className="kpi-detail">Waiting to start</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Completed Orders</div>
          <div className="kpi-value">{completedOrdersCount}</div>
          <div className="kpi-detail">Successfully finished</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Active Workstations</div>
          <div className="kpi-value">{activeWorkstations}</div>
          <div className="kpi-detail">Out of {workstations.length}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Low Stock Alerts</div>
          <div className="kpi-value">{lowStockAlertsCount}</div>
          <div className="kpi-detail">{lowStockAlertsCount > 0 ? "Require attention" : "All levels OK"}</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="dashboard-tabs">
        <button
          className={`tab-button ${activeTab === "overview" ? "active" : ""}`}
          onClick={() => setActiveTab("overview")}
        >
          📊 Overview
        </button>
        <button
          className={`tab-button ${activeTab === "workstations" ? "active" : ""}`}
          onClick={() => setActiveTab("workstations")}
        >
          🔧 Workstations ({workstations.length})
        </button>
        <button
          className={`tab-button ${activeTab === "orders" ? "active" : ""}`}
          onClick={() => setActiveTab("orders")}
        >
          📋 Orders ({totalOrders})
        </button>
        <button
          className={`tab-button ${activeTab === "supply" ? "active" : ""}`}
          onClick={() => setActiveTab("supply")}
        >
          📦 Supply ({supplyPendingCount})
        </button>
      </div>

      {/* Overview Tab */}
      {activeTab === "overview" && (
        <div className="tab-content">
          <h3>System Status Overview</h3>
          
          <div className="overview-grid">
            <div className="overview-section">
              <h4>Production Control Orders</h4>
              <div className="status-breakdown">
                {["PENDING", "SCHEDULED", "IN_PROGRESS", "COMPLETED"].map((status) => {
                  const count = orders.production.filter((o) => o.status === status).length;
                  return (
                    <div key={status} className="status-bar">
                      <span className="status-label">{status}</span>
                      <div className="status-bar-fill">
                        <div
                          className="status-bar-value"
                          style={{
                            width: totalOrders > 0 ? `${(count / totalOrders) * 100}%` : "0%",
                            backgroundColor: getStatusColor(status),
                          }}
                        >
                          {count}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="overview-section">
              <h4>Assembly Control Orders</h4>
              <div className="status-breakdown">
                {["PENDING", "SCHEDULED", "IN_PROGRESS", "COMPLETED"].map((status) => {
                  const count = orders.assembly.filter((o) => o.status === status).length;
                  return (
                    <div key={status} className="status-bar">
                      <span className="status-label">{status}</span>
                      <div className="status-bar-fill">
                        <div
                          className="status-bar-value"
                          style={{
                            width: totalOrders > 0 ? `${(count / totalOrders) * 100}%` : "0%",
                            backgroundColor: getStatusColor(status),
                          }}
                        >
                          {count}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="overview-section">
            <h4>Workstation Activity Summary</h4>
            <div className="workstation-mini-grid">
              {workstations.map((ws) => {
                const stat = workstationStats[ws.id];
                if (!stat) return null;
                return (
                  <div
                    key={ws.id}
                    className={`workstation-mini-card ${stat.status}`}
                    onClick={() => {
                      setSelectedWorkstation(ws.id);
                      setActiveTab("workstations");
                    }}
                  >
                    <div className="mini-name">{ws.name}</div>
                    <div className="mini-status" style={{ color: getStatusColor(stat.status) }}>
                      {stat.status.toUpperCase()}
                    </div>
                    <div className="mini-stats">
                      <span>🔄 {stat.activeOrders} active</span>
                      <span>✓ {stat.completedToday} done</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Workstations Tab */}
      {activeTab === "workstations" && (
        <div className="tab-content">
          <h3>Workstation Status</h3>
          <div className="workstations-grid">
            {workstations.map((ws) => {
              const stat = workstationStats[ws.id];
              if (!stat) return null;
              return (
                <div
                  key={ws.id}
                  className={`workstation-card ${stat.status}`}
                  onClick={() => setSelectedWorkstation(ws.id)}
                >
                  <div className="ws-header">
                    <h4>{ws.name}</h4>
                    <span
                      className="ws-status-badge"
                      style={{ backgroundColor: getStatusColor(stat.status) }}
                    >
                      {stat.status.toUpperCase()}
                    </span>
                  </div>
                  <div className="ws-details">
                    <div className="ws-detail-row">
                      <span className="label">Type:</span>
                      <span className="value">{stat.type}</span>
                    </div>
                    <div className="ws-detail-row">
                      <span className="label">Active Orders:</span>
                      <span className="value highlight">{stat.activeOrders}</span>
                    </div>
                    <div className="ws-detail-row">
                      <span className="label">Completed Today:</span>
                      <span className="value">{stat.completedToday}</span>
                    </div>
                    <div className="ws-detail-row">
                      <span className="label">Total Orders:</span>
                      <span className="value">{stat.totalOrders}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {selectedWorkstation && (
            <div className="selected-workstation-detail">
              <button
                className="close-button"
                onClick={() => setSelectedWorkstation(null)}
              >
                ✕
              </button>
              <h4>Workstation {selectedWorkstation} - Assigned Orders</h4>
              <div className="orders-list">
                {orders.production
                  .filter((o) => o.assignedWorkstationId === selectedWorkstation)
                  .map((order) => (
                    <div key={order.id} className="order-item">
                      <div className="order-header">
                        <span className="order-number">PO-{order.id}</span>
                        <span
                          className="order-status"
                          style={{ backgroundColor: getStatusColor(order.status) }}
                        >
                          {order.status}
                        </span>
                        <span
                          className="order-priority"
                          style={{ backgroundColor: getPriorityColor(order.priority) }}
                        >
                          {order.priority}
                        </span>
                      </div>
                      <div className="order-details">
                        <span>{order.productName || "Unknown Product"}</span>
                        <span className="order-quantity">Qty: {order.quantity}</span>
                      </div>
                    </div>
                  ))}
                {orders.assembly
                  .filter((o) => o.assignedWorkstationId === selectedWorkstation)
                  .map((order) => (
                    <div key={order.id} className="order-item">
                      <div className="order-header">
                        <span className="order-number">AO-{order.id}</span>
                        <span
                          className="order-status"
                          style={{ backgroundColor: getStatusColor(order.status) }}
                        >
                          {order.status}
                        </span>
                        <span
                          className="order-priority"
                          style={{ backgroundColor: getPriorityColor(order.priority) }}
                        >
                          {order.priority}
                        </span>
                      </div>
                      <div className="order-details">
                        <span>{order.productName || "Unknown Product"}</span>
                        <span className="order-quantity">Qty: {order.quantity}</span>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Orders Tab */}
      {activeTab === "orders" && (
        <div className="tab-content">
          <h3>All Production & Assembly Orders</h3>
          
          <div className="orders-section">
            <h4>Production Control Orders ({orders.production.length})</h4>
            <div className="orders-table">
              <div className="table-header">
                <div className="col-id">Order ID</div>
                <div className="col-product">Product</div>
                <div className="col-workstation">Workstation</div>
                <div className="col-status">Status</div>
                <div className="col-priority">Priority</div>
                <div className="col-quantity">Qty</div>
              </div>
              <div className="table-body">
                {orders.production.map((order) => (
                  <div key={order.id} className="table-row">
                    <div className="col-id">PO-{order.id}</div>
                    <div className="col-product">
                      {order.orderItems && order.orderItems.length > 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          {order.orderItems.map((item, idx) => (
                            <span key={idx} style={{ fontSize: '0.85rem', padding: '0.25rem 0.5rem', backgroundColor: '#f3f4f6', borderRadius: '0.25rem' }}>
                              <span style={{ fontWeight: '600' }}>{item.itemType || item.name || 'Item'}</span>
                              <span style={{ color: '#999', margin: '0 0.25rem' }}>×</span>
                              <span style={{ color: '#059669', fontWeight: '600' }}>{item.quantity}</span>
                              {item.status && <span style={{ color: '#666', fontSize: '0.75rem', marginLeft: '0.25rem' }}>({item.status})</span>}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span>{order.productName || "Unknown"}</span>
                      )}
                    </div>
                    <div className="col-workstation">
                      WS-{order.assignedWorkstationId || "—"}
                    </div>
                    <div className="col-status">
                      <span
                        className="badge"
                        style={{ backgroundColor: getStatusColor(order.status) }}
                      >
                        {order.status}
                      </span>
                    </div>
                    <div className="col-priority">
                      <span
                        className="badge"
                        style={{ backgroundColor: getPriorityColor(order.priority) }}
                      >
                        {order.priority}
                      </span>
                    </div>
                    <div className="col-quantity">{order.quantity}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="orders-section">
            <h4>Assembly Control Orders ({orders.assembly.length})</h4>
            <div className="orders-table">
              <div className="table-header">
                <div className="col-id">Order ID</div>
                <div className="col-product">Product</div>
                <div className="col-workstation">Workstation</div>
                <div className="col-status">Status</div>
                <div className="col-priority">Priority</div>
                <div className="col-quantity">Qty</div>
              </div>
              <div className="table-body">
                {orders.assembly.map((order) => (
                  <div key={order.id} className="table-row">
                    <div className="col-id">AO-{order.id}</div>
                    <div className="col-product">
                      {order.orderItems && order.orderItems.length > 0 ? (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          {order.orderItems.map((item, idx) => (
                            <span key={idx} style={{ fontSize: '0.85rem', padding: '0.25rem 0.5rem', backgroundColor: '#f3f4f6', borderRadius: '0.25rem' }}>
                              <span style={{ fontWeight: '600' }}>{item.itemType || item.name || 'Item'}</span>
                              <span style={{ color: '#999', margin: '0 0.25rem' }}>×</span>
                              <span style={{ color: '#059669', fontWeight: '600' }}>{item.quantity}</span>
                              {item.status && <span style={{ color: '#666', fontSize: '0.75rem', marginLeft: '0.25rem' }}>({item.status})</span>}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span>{order.productName || "Unknown"}</span>
                      )}
                    </div>
                    <div className="col-workstation">
                      WS-{order.assignedWorkstationId || "—"}
                    </div>
                    <div className="col-status">
                      <span
                        className="badge"
                        style={{ backgroundColor: getStatusColor(order.status) }}
                      >
                        {order.status}
                      </span>
                    </div>
                    <div className="col-priority">
                      <span
                        className="badge"
                        style={{ backgroundColor: getPriorityColor(order.priority) }}
                      >
                        {order.priority}
                      </span>
                    </div>
                    <div className="col-quantity">{order.quantity}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Supply Tab */}
      {activeTab === "supply" && (
        <div className="tab-content">
          <h3>Supply Orders</h3>
          <div className="supply-grid">
            {orders.supply.length === 0 ? (
              <div className="empty-state">No supply orders</div>
            ) : (
              orders.supply.map((order) => (
                <div key={order.id} className="supply-card">
                  <div className="supply-header">
                    <span className="supply-number">SO-{order.supplyOrderNumber}</span>
                    <span
                      className="supply-status"
                      style={{ backgroundColor: getStatusColor(order.status) }}
                    >
                      {order.status}
                    </span>
                  </div>
                  <div className="supply-details">
                    <div className="detail-row">
                      <span className="label">From:</span>
                      <span className="value">
                        WS-{order.requestingWorkstationId}
                      </span>
                    </div>
                    <div className="detail-row">
                      <span className="label">To:</span>
                      <span className="value">
                        WS-{order.supplyWarehouseWorkstationId}
                      </span>
                    </div>
                    <div className="detail-row">
                      <span className="label">Priority:</span>
                      <span
                        className="badge"
                        style={{
                          backgroundColor: getPriorityColor(order.priority),
                        }}
                      >
                        {order.priority}
                      </span>
                    </div>
                    <div className="detail-row">
                      <span className="label">Parts:</span>
                      <span className="value">{order.supplyOrderItems?.length || 0}</span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      <div className="dashboard-footer">
        Last updated: {new Date().toLocaleTimeString()} | Auto-refresh: 15s
      </div>
      </section>
    </div>
  );
}

export default SystemOverview;
