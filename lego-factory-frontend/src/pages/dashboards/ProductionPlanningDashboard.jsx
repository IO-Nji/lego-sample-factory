import { useState, useEffect } from "react";
import api from "../../api/api";
import { StandardDashboardLayout, CompactScheduleTimeline, OrdersSection, StatisticsGrid, Button } from "../../components";
import ProductionOrderCard from "../../components/ProductionOrderCard";
import "../../styles/DashboardLayout.css";

function ProductionPlanningDashboard() {
  const [productionOrders, setProductionOrders] = useState([]);
  const [scheduledOrders, setScheduledOrders] = useState([]);
  const [scheduledTasks, setScheduledTasks] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
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
      const response = await api.get("/production-orders");
      const orders = Array.isArray(response.data) ? response.data : [];
      setProductionOrders(orders);
    } catch (err) {
      setError("Failed to load production orders: " + (err.message || "Unknown error"));
      addNotification("Failed to load production orders", "error");
    } finally {
      setLoading(false);
    }
  };

  const fetchScheduledOrders = async () => {
    try {
      const response = await api.get("/simal/scheduled-orders");
      const orders = Array.isArray(response.data) ? response.data : [];
      setScheduledOrders(orders);
      
      // Extract and format all tasks for timeline
      const allTasks = [];
      orders.forEach(order => {
        if (order.scheduledTasks && Array.isArray(order.scheduledTasks)) {
          order.scheduledTasks.forEach(task => {
            allTasks.push({
              ...task,
              id: task.taskId,
              workstationName: task.workstationName,
              startTime: task.startTime,
              endTime: task.endTime,
              status: order.status || 'SCHEDULED',
              orderId: order.id,
              scheduleId: order.scheduleId
            });
          });
        }
      });
      console.log('📊 Scheduled Tasks for Timeline:', allTasks.length, 'tasks');
      console.log('📊 Sample task:', allTasks[0]);
      console.log('📊 All tasks:', allTasks);
      setScheduledTasks(allTasks);
    } catch (err) {
      console.error("Failed to load scheduled orders:", err);
    }
  };

  useEffect(() => {
    const loadData = async () => {
      await fetchProductionOrders();
      await fetchScheduledOrders();
    };
    
    loadData();
    
    // Background refresh without full page re-render
    const interval = setInterval(() => {
      loadData();
    }, 30000); // Increased to 30s to reduce jerky behavior
    return () => clearInterval(interval);
  }, []);
  
  // Refresh scheduled tasks when production orders change
  useEffect(() => {
    if (productionOrders.length > 0 && scheduledOrders.length > 0) {
      fetchScheduledOrders();
    }
  }, [productionOrders]);

  const handleScheduleWithSimAL = async (order) => {
    setSchedulingInProgress(prev => ({ ...prev, [order.id]: true }));
    setError(null);
    setSuccess(null);

    try {
      // Use new endpoint that auto-creates control orders
      const response = await api.post(`/simal/schedule-production-order/${order.id}`);
      
      setSimalResponse(response.data);
      setShowScheduleModal(true);
      
      // Try to update production order with SimAL schedule ID
      // If this fails due to auth, the SimAL schedule is already created
      try {
        await api.patch(`/production-orders/${order.id}/schedule`, {
          simalScheduleId: response.data.scheduleId,
          status: "SCHEDULED"
        });
        setSuccess(`Production order ${order.productionOrderNumber} successfully scheduled with SimAL`);
        addNotification(`Order ${order.productionOrderNumber} scheduled`, "success");
      } catch (updateErr) {
        // Log the error but don't fail the whole operation
        console.warn('Failed to update production order status, but SimAL schedule was created:', updateErr);
        setSuccess(`SimAL schedule ${response.data.scheduleId} created. Refresh page to see updated order status.`);
        addNotification(`Schedule created: ${response.data.scheduleId}`, "success");
      }
      
      await fetchProductionOrders();
      await fetchScheduledOrders();
    } catch (err) {
      // Only fail if SimAL scheduling itself failed
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
      const response = await api.post("/simal/generate-control-orders", {
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
      await api.patch(`/production-orders/${orderId}/status`, { status: newStatus });
      setSuccess(`Production order status updated to ${newStatus}`);
      addNotification(`Status updated to ${newStatus}`, "info");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to update status";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  const handleDispatchProduction = async (orderId) => {
    try {
      const response = await api.post(`/production-planning/${orderId}/dispatch`);
      setSuccess(`Production order dispatched - Control orders created and sent to workstations`);
      addNotification("Production dispatched to workstations", "success");
      await fetchProductionOrders();
      await fetchScheduledOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to dispatch production";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm("Are you sure you want to cancel this production order?")) {
      return;
    }

    try {
      await api.patch(`/production-orders/${orderId}/cancel`);
      setSuccess("Production order cancelled successfully");
      addNotification("Production order cancelled", "warning");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to cancel order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };
  
  const handleTaskClick = (task) => {
    // Find the related production order
    const relatedOrder = productionOrders.find(order => {
      return scheduledOrders.some(sched => 
        sched.productionOrderId === order.id && 
        sched.tasks?.some(t => t.id === task.id || t.taskId === task.taskId)
      );
    });
    
    if (relatedOrder) {
      setSelectedOrder(relatedOrder);
      addNotification(`Viewing task: ${task.taskType || task.operationType || 'Task'} from Order ${relatedOrder.productionOrderNumber}`, 'info');
    }
  };

  const getStatusBadgeColor = (status) => {
    const colors = {
      CREATED: "#6b7280",
      SUBMITTED: "#3b82f6",
      SCHEDULED: "#8b5cf6",
      DISPATCHED: "#0891b2",
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
  // Stats data for StatisticsGrid
  const statsData = (() => {
    const statusCounts = productionOrders.reduce((acc, order) => {
      acc[order.status] = (acc[order.status] || 0) + 1;
      return acc;
    }, {});

    return [
      { value: productionOrders.length, label: 'Total Orders', variant: 'default', icon: '📦' },
      { value: statusCounts.PENDING || statusCounts.CREATED || 0, label: 'Pending', variant: 'pending', icon: '⏳' },
      { value: statusCounts.SCHEDULED || 0, label: 'Scheduled', variant: 'processing', icon: '📅' },
      { value: statusCounts.DISPATCHED || 0, label: 'Dispatched', variant: 'info', icon: '🚀' },
      { value: statusCounts.IN_PRODUCTION || 0, label: 'In Production', variant: 'warning', icon: '⚙️' },
      { value: statusCounts.COMPLETED || 0, label: 'Completed', variant: 'success', icon: '✅' },
      { value: scheduledOrders.length, label: 'Active Schedules', variant: 'info', icon: '📊' },
      { value: scheduledOrders.reduce((sum, s) => sum + (s.scheduledTasks?.length || 0), 0), label: 'Total Tasks', variant: 'default', icon: '✓' },
    ];
  })();

  // Render Production Orders Section using standardized OrdersSection
  const renderProductionOrders = () => (
    <OrdersSection
      title="Production Orders"
      icon="📋"
      orders={productionOrders}
      filterOptions={[
        { value: 'ALL', label: 'All Orders' },
        { value: 'CREATED', label: 'Pending' },
        { value: 'SUBMITTED', label: 'Submitted' },
        { value: 'SCHEDULED', label: 'Scheduled' },
        { value: 'IN_PRODUCTION', label: 'In Production' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'CANCELLED', label: 'Cancelled' }
      ]}
      sortOptions={[
        { value: 'productionOrderNumber', label: 'Order Number' },
        { value: 'createdAt', label: 'Created Date' },
        { value: 'priority', label: 'Priority' },
        { value: 'status', label: 'Status' }
      ]}
      renderCard={(order) => (
        <ProductionOrderCard
          key={order.id}
          order={order}
          onSchedule={(order) => {
            setSelectedOrder(order);
            handleScheduleWithSimAL(order);
          }}
          onStart={(orderId) => handleDispatchProduction(orderId)}
          onComplete={(orderId) => handleUpdateStatus(orderId, "COMPLETED")}
          onCancel={handleCancelOrder}
          isScheduling={schedulingInProgress[order.id]}
        />
      )}
      searchPlaceholder="Search by order number..."
      emptyMessage="Orders will appear here when created by Modules Supermarket"
    />
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

  // Render Production Schedule Timeline
  const renderScheduleTimeline = () => (
    <CompactScheduleTimeline
      scheduledTasks={scheduledTasks}
      onTaskClick={handleTaskClick}
      title="Production Schedule Timeline"
    />
  );

  // Render Statistics
  const renderStats = () => <StatisticsGrid stats={statsData} />;

  // Render placeholder for Gantt chart/schedule view
  const renderScheduleView = () => (
    <div className="dashboard-info-box" style={{ padding: '0.75rem' }}>
      <h3 style={{ fontWeight: 'bold', fontSize: '0.875rem', marginBottom: '0.5rem', color: '#8b5cf6' }}>
        Schedule Overview
      </h3>
      <ul style={{ marginLeft: '1rem', fontSize: '0.8125rem', lineHeight: '1.4', color: '#6b7280', marginBottom: '0' }}>
        <li><strong>Active Schedules:</strong> {scheduledOrders.length}</li>
        <li><strong>Total Tasks:</strong> {scheduledOrders.reduce((sum, s) => sum + (s.scheduledTasks?.length || 0), 0)}</li>
        <li><strong>SimAL Integration:</strong> Enabled</li>
      </ul>
    </div>
  );

  return (
    <>
      <StandardDashboardLayout
        title="Production Planning"
        subtitle="Factory-Wide Scheduling & Order Management"
        icon="📅"
        activityContent={renderScheduleTimeline()}
        statsContent={renderStats()}
        formContent={renderScheduleView()}
        contentGrid={renderProductionOrders()}
        inventoryContent={null}
      />
      {renderScheduleModal()}
    </>
  );
}

export default ProductionPlanningDashboard;
