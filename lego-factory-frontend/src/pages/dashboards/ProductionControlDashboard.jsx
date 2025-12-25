import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function ProductionControlDashboard() {
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchControlOrders = async () => {
    const workstationId = session?.user?.workstationId;
    if (!workstationId) return;

    setLoading(true);
    setError(null);

    try {
      const response = await axios.get(`/api/production-control-orders/workstation/${workstationId}`);
      setControlOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError("Failed to load control orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchControlOrders();
      const interval = setInterval(fetchControlOrders, 5000);
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Production Control Dashboard"
        subtitle="Manage production control orders"
        icon="🏭"
        actions={[
          <button
            key="refresh"
            onClick={fetchControlOrders}
            disabled={loading}
            className="standard-btn standard-btn-primary"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        ]}
      />

      {error && <div className="error-alert">{error}</div>}

      {loading ? (
        <div>Loading control orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {controlOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Quantity</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {controlOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td>{order.quantity}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No production control orders assigned yet</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

export default ProductionControlDashboard;
