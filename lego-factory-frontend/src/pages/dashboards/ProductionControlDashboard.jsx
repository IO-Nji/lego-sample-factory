import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout } from "../../components";
import "../../styles/DashboardLayout.css";

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

  const renderOrdersTable = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-blue">
        <h2 className="dashboard-box-header-title">🏭 Production Control Orders</h2>
        <button 
          onClick={fetchControlOrders} 
          disabled={loading} 
          className="dashboard-box-header-action"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>
      <div className="dashboard-box-content">
        {loading ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">Loading control orders...</p>
          </div>
        ) : (
          <div>
            {controlOrders.length > 0 ? (
              <table className="dashboard-table">
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
              <div className="dashboard-empty-state">
                <p className="dashboard-empty-state-title">No control orders assigned</p>
                <p className="dashboard-empty-state-text">Orders will appear here when assigned to your workstation</p>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );

  return (
    <DashboardLayout
      title="Production Control Dashboard"
      subtitle={`Workstation ${session?.user?.workstationId || 'N/A'} - Manage production control`}
      icon="🏭"
      layout="default"
      ordersSection={renderOrdersTable()}
      messages={{ error, success: null }}
      onDismissError={() => setError(null)}
    />
  );
}

export default ProductionControlDashboard;
