import { useState, useEffect } from "react";
import axios from "axios";
import { DashboardLayout, Button, Notification } from "../../components";
import "../../styles/DashboardLayout.css";

function PartsSupplyWarehouseDashboard() {
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("PENDING");
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'Parts Supply Warehouse'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  const fetchSupplyOrders = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await axios.get("/api/supply-orders/warehouse", {
        params: { status: filter || undefined },
      });
      setSupplyOrders(response.data);
    } catch (err) {
      setError("Failed to fetch supply orders: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSupplyOrders();
    const interval = setInterval(fetchSupplyOrders, 10000);
    return () => clearInterval(interval);
  }, [filter]);

  const fulfillSupplyOrder = async (orderId) => {
    try {
      const response = await axios.put(`/api/supply-orders/${orderId}/fulfill`);
      addNotification(`Supply order ${orderId} fulfilled`, 'success');
      fetchSupplyOrders();
    } catch (err) {
      setError("Failed to fulfill supply order: " + err.message);
      addNotification("Failed to fulfill supply order", 'error');
    }
  };

  const renderFilterButtons = () => (
    <div style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}>
      {["PENDING", "IN_PROGRESS", "COMPLETED"].map((status) => (
        <Button
          key={status}
          variant={filter === status ? "primary" : "secondary"}
          size="small"
          onClick={() => setFilter(status)}
        >
          {status}
        </Button>
      ))}
    </div>
  );

  const renderOrdersTable = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-orange">
        <h2 className="dashboard-box-header-title">📦 Parts Supply Orders</h2>
        <button 
          onClick={fetchSupplyOrders} 
          disabled={loading} 
          className="dashboard-box-header-action"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>
      <div className="dashboard-box-content">
        {renderFilterButtons()}
        {loading ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">Loading supply orders...</p>
          </div>
        ) : (
          <div>
            {supplyOrders.length > 0 ? (
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Items Needed</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {supplyOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.supplyOrderNumber}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td>{order.quantity || 0}</td>
                      <td>
                        {order.status === "PENDING" && (
                          <Button 
                            variant="success" 
                            size="small"
                            onClick={() => fulfillSupplyOrder(order.id)}
                          >
                            Fulfill
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="dashboard-empty-state">
                <p className="dashboard-empty-state-title">No supply orders found</p>
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
      title="Parts Supply Warehouse Dashboard"
      subtitle="Manage parts supply orders"
      icon="📦"
      layout="default"
      secondaryContent={
        <Notification 
          notifications={notifications}
          title="Supply Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={renderOrdersTable()}
      messages={{ error, success: null }}
      onDismissError={() => setError("")}
    />
  );
}

export default PartsSupplyWarehouseDashboard;
