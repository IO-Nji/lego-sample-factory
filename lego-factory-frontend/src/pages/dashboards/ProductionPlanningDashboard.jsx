import { useState, useEffect } from "react";
import axios from "axios";
import { DashboardLayout } from "../../components";
import "../../styles/DashboardLayout.css";

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

  const renderOrdersTable = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-blue">
        <h2 className="dashboard-box-header-title">📋 Production Orders</h2>
        <button 
          onClick={fetchProductionOrders} 
          disabled={loading} 
          className="dashboard-box-header-action"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>
      <div className="dashboard-box-content">
        {loading ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">Loading production orders...</p>
          </div>
        ) : (
          <div>
            {productionOrders.length > 0 ? (
              <table className="dashboard-table">
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
              <div className="dashboard-empty-state">
                <p className="dashboard-empty-state-title">No production orders found</p>
                <p className="dashboard-empty-state-text">Orders will appear here when created</p>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );

  return (
    <DashboardLayout
      title="Production Planning Dashboard"
      subtitle="Manage production orders and scheduling"
      icon="📋"
      layout="default"
      ordersSection={renderOrdersTable()}
      messages={{ error, success: null }}
      onDismissError={() => setError(null)}
    />
  );
}

export default ProductionPlanningDashboard;
