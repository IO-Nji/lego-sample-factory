import { useState, useEffect } from "react";
import axios from "axios";
import { DashboardLayout, Notification, StatsCard, Button } from "../../components";
import ProductionOrderCard from "../../components/ProductionOrderCard";
import "../../styles/DashboardLayout.css";

function ProductionPlanningDashboard() {
  const [productionOrders, setProductionOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [scheduledOrders, setScheduledOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [schedulingInProgress, setSchedulingInProgress] = useState({});
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [simalResponse, setSimalResponse] = useState(null);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'PROD-PL'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  const fetchProductionOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get("/api/production-orders");
      const orders = Array.isArray(response.data) ? response.data : [];
      setProductionOrders(orders);
      applyFilter(orders, filterStatus);
    } catch (err) {
      setError("Failed to load production orders: " + (err.message || "Unknown error"));
      addNotification("Failed to load production orders", "error");
    } finally {
      setLoading(false);
    }
  };

  const fetchScheduledOrders = async () => {
    try {
      const response = await axios.get("/api/simal/scheduled-orders");
      setScheduledOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to load scheduled orders:", err);
    }
  };

  useEffect(() => {
    fetchProductionOrders();
    fetchScheduledOrders();
    const interval = setInterval(() => {
      fetchProductionOrders();
      fetchScheduledOrders();
    }, 10000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    applyFilter(productionOrders, filterStatus);
  }, [filterStatus, productionOrders]);

  const applyFilter = (orders, status) => {
    if (status === "ALL") {
      setFilteredOrders(orders);
    } else {
      setFilteredOrders(orders.filter(order => order.status === status));
    }
  };

  const handleScheduleWithSimAL = async (order) => {
    setSchedulingInProgress(prev => ({ ...prev, [order.id]: true }));
    setError(null);
    setSuccess(null);

    try {
      // Prepare SimAL request
      const simalRequest = {
        orderNumber: order.productionOrderNumber,
        productionOrderId: order.id,
        priority: order.priority || "MEDIUM",
        dueDate: order.dueDate,
        items: [] // In real implementation, this would come from order details
      };

      // Send to SimAL for scheduling
      const response = await axios.post("/api/simal/schedule", simalRequest);
      
      // Update production order with SimAL schedule ID
      await axios.patch(`/api/production-orders/${order.id}/schedule`, {
        simalScheduleId: response.data.scheduleId,
        status: "SCHEDULED"
      });

      setSimalResponse(response.data);
      setShowScheduleModal(true);
      setSuccess(`Production order ${order.productionOrderNumber} successfully scheduled with SimAL`);
      addNotification(`Order ${order.productionOrderNumber} scheduled`, "success");
      
      await fetchProductionOrders();
      await fetchScheduledOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to schedule with SimAL";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setSchedulingInProgress(prev => ({ ...prev, [order.id]: false }));
    }
  };

  const handleGenerateControlOrders = async (scheduleData) => {
    try {
      // Call backend to generate control orders from SimAL response
      const response = await axios.post("/api/simal/generate-control-orders", {
        scheduleId: scheduleData.scheduleId,
        productionOrderId: selectedOrder?.id,
        scheduledTasks: scheduleData.scheduledTasks
      });

      setSuccess(`Generated ${response.data.productionControlOrders?.length || 0} production control orders and ${response.data.assemblyControlOrders?.length || 0} assembly control orders`);
      addNotification("Control orders generated successfully", "success");
      setShowScheduleModal(false);
      setSimalResponse(null);
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to generate control orders";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  const handleUpdateStatus = async (orderId, newStatus) => {
    try {
      await axios.patch(`/api/production-orders/${orderId}/status`, { status: newStatus });
      setSuccess(`Production order status updated to ${newStatus}`);
      addNotification(`Status updated to ${newStatus}`, "info");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to update status";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm("Are you sure you want to cancel this production order?")) {
      return;
    }

    try {
      await axios.patch(`/api/production-orders/${orderId}/cancel`);
      setSuccess("Production order cancelled successfully");
      addNotification("Production order cancelled", "warning");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to cancel order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  const getStatusBadgeColor = (status) => {
    const colors = {
      CREATED: "#6b7280",
      SUBMITTED: "#3b82f6",
      SCHEDULED: "#8b5cf6",
      IN_PRODUCTION: "#f59e0b",
      COMPLETED: "#10b981",
      CANCELLED: "#ef4444"
    };
    return colors[status] || "#6b7280";
  };

  const getPriorityBadgeColor = (priority) => {
    const colors = {
      LOW: "#10b981",
      MEDIUM: "#f59e0b",
      HIGH: "#ef4444"
    };
    return colors[priority] || "#6b7280";
  };

  // Render Statistics Cards
  const renderStatsCards = () => {
    const statusCounts = productionOrders.reduce((acc, order) => {
      acc[order.status] = (acc[order.status] || 0) + 1;
      return acc;
    }, {});

    return (
      <>
        <StatsCard 
          value={statusCounts.CREATED || 0} 
          label="Pending" 
          variant="pending"
        />
        <StatsCard 
          value={statusCounts.SCHEDULED || 0} 
          label="Scheduled" 
          variant="processing"
        />
        <StatsCard 
          value={statusCounts.IN_PRODUCTION || 0} 
          label="In Production" 
          variant="warning"
        />
        <StatsCard 
          value={statusCounts.COMPLETED || 0} 
          label="Completed" 
          variant="completed"
        />
      </>
    );
  };

  // Render Production Orders Section
  const renderProductionOrders = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-purple">
        <h2 className="dashboard-box-header-title">📋 Production Orders</h2>
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <select 
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            style={{ 
              padding: "0.5rem", 
              borderRadius: "0.375rem", 
              border: "1px solid #d1d5db",
              fontSize: "0.875rem"
            }}
          >
            <option value="ALL">All Orders</option>
            <option value="CREATED">Pending</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="IN_PRODUCTION">In Production</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button 
            onClick={fetchProductionOrders} 
            disabled={loading} 
            className="dashboard-box-header-action"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>
      <div className="dashboard-box-content">
        {loading ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">Loading production orders...</p>
          </div>
        ) : filteredOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <div key={order.id} style={{ position: 'relative' }}>
                <ProductionOrderCard order={order}>
                  <div style={{ 
                    padding: "1rem", 
                    backgroundColor: "#f9fafb",
                    borderTop: "1px solid #e5e7eb",
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.75rem"
                  }}>
                    {/* Order Details */}
                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.875rem" }}>
                      <div>
                        <strong>Order:</strong> {order.productionOrderNumber}
                      </div>
                      <div>
                        <strong>Priority:</strong>{" "}
                        <span style={{
                          padding: "0.125rem 0.5rem",
                          borderRadius: "0.25rem",
                          backgroundColor: getPriorityBadgeColor(order.priority),
                          color: "white",
                          fontSize: "0.75rem",
                          fontWeight: "700"
                        }}>
                          {order.priority || "MEDIUM"}
                        </span>
                      </div>
                    </div>

                    <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.875rem" }}>
                      <div>
                        <strong>Status:</strong>{" "}
                        <span style={{
                          padding: "0.125rem 0.5rem",
                          borderRadius: "0.25rem",
                          backgroundColor: getStatusBadgeColor(order.status),
                          color: "white",
                          fontSize: "0.75rem",
                          fontWeight: "700"
                        }}>
                          {order.status}
                        </span>
                      </div>
                      {order.dueDate && (
                        <div>
                          <strong>Due:</strong> {new Date(order.dueDate).toLocaleDateString()}
                        </div>
                      )}
                    </div>

                    {order.simalScheduleId && (
                      <div style={{ fontSize: "0.875rem", color: "#6b7280" }}>
                        <strong>Schedule ID:</strong> {order.simalScheduleId}
                      </div>
                    )}

                    {order.estimatedDuration && (
                      <div style={{ fontSize: "0.875rem", color: "#6b7280" }}>
                        <strong>Est. Duration:</strong> {order.estimatedDuration} minutes
                      </div>
                    )}

                    {/* Action Buttons */}
                    <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", marginTop: "0.5rem" }}>
                      {order.status === "CREATED" && (
                        <button
                          onClick={() => {
                            setSelectedOrder(order);
                            handleScheduleWithSimAL(order);
                          }}
                          disabled={schedulingInProgress[order.id]}
                          style={{
                            flex: 1,
                            padding: "0.5rem",
                            backgroundColor: "#8b5cf6",
                            color: "white",
                            border: "none",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "600",
                            cursor: schedulingInProgress[order.id] ? "not-allowed" : "pointer",
                            opacity: schedulingInProgress[order.id] ? 0.6 : 1
                          }}
                        >
                          {schedulingInProgress[order.id] ? "Scheduling..." : "📅 Schedule with SimAL"}
                        </button>
                      )}

                      {order.status === "SCHEDULED" && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, "IN_PRODUCTION")}
                          style={{
                            flex: 1,
                            padding: "0.5rem",
                            backgroundColor: "#f59e0b",
                            color: "white",
                            border: "none",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "600",
                            cursor: "pointer"
                          }}
                        >
                          ▶️ Start Production
                        </button>
                      )}

                      {order.status === "IN_PRODUCTION" && (
                        <button
                          onClick={() => handleUpdateStatus(order.id, "COMPLETED")}
                          style={{
                            flex: 1,
                            padding: "0.5rem",
                            backgroundColor: "#10b981",
                            color: "white",
                            border: "none",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "600",
                            cursor: "pointer"
                          }}
                        >
                          ✅ Mark Complete
                        </button>
                      )}

                      {order.status !== "COMPLETED" && order.status !== "CANCELLED" && (
                        <button
                          onClick={() => handleCancelOrder(order.id)}
                          style={{
                            padding: "0.5rem 1rem",
                            backgroundColor: "#ef4444",
                            color: "white",
                            border: "none",
                            borderRadius: "0.375rem",
                            fontSize: "0.875rem",
                            fontWeight: "600",
                            cursor: "pointer"
                          }}
                        >
                          ❌ Cancel
                        </button>
                      )}
                    </div>
                  </div>
                </ProductionOrderCard>
              </div>
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No production orders found</p>
            <p className="dashboard-empty-state-text">
              {filterStatus !== "ALL" 
                ? `No orders with status: ${filterStatus}` 
                : "Orders will appear here when created by Modules Supermarket"}
            </p>
          </div>
        )}
      </div>
    </>
  );

  // Render SimAL Schedule Modal
  const renderScheduleModal = () => {
    if (!showScheduleModal || !simalResponse) return null;

    return (
      <div style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: "rgba(0, 0, 0, 0.5)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000
      }}>
        <div style={{
          backgroundColor: "white",
          borderRadius: "0.5rem",
          padding: "1.5rem",
          maxWidth: "900px",
          width: "90%",
          maxHeight: "80vh",
          overflow: "auto",
          boxShadow: "0 10px 25px rgba(0, 0, 0, 0.2)"
        }}>
          <h2 style={{ marginTop: 0, color: "#8b5cf6" }}>📅 SimAL Scheduling Result</h2>
          
          <div style={{ marginBottom: "1rem", padding: "1rem", backgroundColor: "#f3f4f6", borderRadius: "0.375rem" }}>
            <p><strong>Schedule ID:</strong> {simalResponse.scheduleId}</p>
            <p><strong>Order:</strong> {simalResponse.orderNumber}</p>
            <p><strong>Status:</strong> {simalResponse.status}</p>
            <p><strong>Total Duration:</strong> {simalResponse.totalDuration} minutes</p>
            {simalResponse.estimatedCompletionTime && (
              <p><strong>Est. Completion:</strong> {new Date(simalResponse.estimatedCompletionTime).toLocaleString()}</p>
            )}
          </div>

          <h3>Scheduled Tasks ({simalResponse.scheduledTasks?.length || 0})</h3>
          <div style={{ maxHeight: "300px", overflow: "auto", marginBottom: "1rem" }}>
            <table style={{ width: "100%", fontSize: "0.875rem", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ backgroundColor: "#f9fafb", borderBottom: "2px solid #e5e7eb" }}>
                  <th style={{ padding: "0.5rem", textAlign: "left" }}>Task ID</th>
                  <th style={{ padding: "0.5rem", textAlign: "left" }}>Type</th>
                  <th style={{ padding: "0.5rem", textAlign: "left" }}>Workstation</th>
                  <th style={{ padding: "0.5rem", textAlign: "right" }}>Duration</th>
                  <th style={{ padding: "0.5rem", textAlign: "left" }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {simalResponse.scheduledTasks?.map((task, index) => (
                  <tr key={index} style={{ borderBottom: "1px solid #e5e7eb" }}>
                    <td style={{ padding: "0.5rem" }}>{task.taskId}</td>
                    <td style={{ padding: "0.5rem" }}>{task.type}</td>
                    <td style={{ padding: "0.5rem" }}>{task.workstationName}</td>
                    <td style={{ padding: "0.5rem", textAlign: "right" }}>{task.duration} min</td>
                    <td style={{ padding: "0.5rem" }}>
                      <span style={{
                        padding: "0.125rem 0.5rem",
                        borderRadius: "0.25rem",
                        backgroundColor: "#e0e7ff",
                        color: "#3730a3",
                        fontSize: "0.75rem",
                        fontWeight: "600"
                      }}>
                        {task.status || "SCHEDULED"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end" }}>
            <button
              onClick={() => handleGenerateControlOrders(simalResponse)}
              style={{
                padding: "0.75rem 1.5rem",
                backgroundColor: "#8b5cf6",
                color: "white",
                border: "none",
                borderRadius: "0.375rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                cursor: "pointer"
              }}
            >
              ✨ Generate Control Orders
            </button>
            <button
              onClick={() => {
                setShowScheduleModal(false);
                setSimalResponse(null);
              }}
              style={{
                padding: "0.75rem 1.5rem",
                backgroundColor: "#6b7280",
                color: "white",
                border: "none",
                borderRadius: "0.375rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                cursor: "pointer"
              }}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    );
  };

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
      statsCards={renderStatsCards()}
      secondaryContent={
        <Notification 
          notifications={notifications}
          title="Planning Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={renderProductionOrders()}
      messages={{ error, success }}
      onDismissError={() => setError(null)}
      onDismissSuccess={() => setSuccess(null)}
    >
      {renderScheduleModal()}
    </DashboardLayout>
  );
}

export default ProductionPlanningDashboard;
