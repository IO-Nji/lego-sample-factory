import { useState, useEffect } from "react";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function PartsSupplyWarehouseDashboard() {
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("PENDING");

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
      fetchSupplyOrders();
    } catch (err) {
      setError("Failed to fulfill supply order: " + err.message);
    }
  };

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Parts Supply Warehouse Dashboard"
        subtitle="Manage parts supply orders"
        icon="📦"
      />

      {error && <div className="error-alert">{error}</div>}

      <div style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}>
        {["PENDING", "IN_PROGRESS", "COMPLETED"].map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            style={{
              padding: "0.5rem 1rem",
              background: filter === status ? "#0b5394" : "#e0e0e0",
              color: filter === status ? "white" : "#333",
              border: "none",
              borderRadius: "0.375rem",
              cursor: "pointer",
              fontWeight: "600",
            }}
          >
            {status}
          </button>
        ))}
      </div>

      {loading ? (
        <div>Loading supply orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {supplyOrders.length > 0 ? (
              <table>
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
                          <button onClick={() => fulfillSupplyOrder(order.id)} style={{ padding: "0.5rem 1rem", background: "#27ae60", color: "white", border: "none", borderRadius: "0.375rem", cursor: "pointer", fontWeight: "600", fontSize: "0.875rem" }}>
                            Fulfill
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No supply orders found</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

export default PartsSupplyWarehouseDashboard;
