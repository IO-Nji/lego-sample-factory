import { useState, useEffect } from "react";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function ProductionPlanningDashboard() {
  const [productionOrders, setProductionOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchProductionOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get("/api/production-control-orders");
      setProductionOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError("Failed to load production orders: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProductionOrders();
    const interval = setInterval(fetchProductionOrders, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Production Planning Dashboard"
        subtitle="Manage production orders and scheduling"
        icon="📋"
        actions={[
          <button
            key="refresh"
            onClick={fetchProductionOrders}
            disabled={loading}
            className="standard-btn standard-btn-primary"
            style={{
              cursor: loading ? "not-allowed" : "pointer",
              opacity: loading ? 0.6 : 1,
            }}
          >
            {loading ? "⟳ Refreshing..." : "⟳ Refresh"}
          </button>
        ]}
      />

      {error && <div className="error-alert">{error}</div>}

      {loading ? (
        <div>Loading production orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {productionOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Items</th>
                    <th>Workstation</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {productionOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status || "UNKNOWN"}
                        </span>
                      </td>
                      <td>{order.quantity || 0}</td>
                      <td>{order.workstationName || "N/A"}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt || new Date()).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No production orders at this time</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

export default ProductionPlanningDashboard;
