import { useState, useEffect } from "react";
import api from "../../api/api";
import { StandardDashboardLayout, CompactScheduleTimeline, OrdersSection, StatisticsGrid, Button } from "../../components";
import ProductionOrderCard from "../../components/ProductionOrderCard";
import "../../styles/DashboardLayout.css";

/**
 * ProductionPlanningDashboard - 4-Step Production Order Workflow
 * 
 * Step 1: View - Orders come in as CREATED status
 * Step 2: Confirm - User clicks Confirm → CREATED → CONFIRMED
 * Step 3: Schedule - User clicks Schedule → Shows preview popup → User confirms → SCHEDULED
 * Step 4: Submit - User clicks Submit → Creates control orders → DISPATCHED
 */
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
  const [showSchedulePreview, setShowSchedulePreview] = useState(false);
  const [orderToSchedule, setOrderToSchedule] = useState(null);

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

  /**
   * Step 2: Confirm Production Order (CREATED → CONFIRMED)
   * Confirms that the production planner has reviewed the order
   */
  const handleConfirmOrder = async (orderId) => {
    setError(null);
    setSuccess(null);
    
    try {
      await api.put(`/production-planning/${orderId}/confirm`);
      setSuccess("Production order confirmed - ready for scheduling");
      addNotification("Order confirmed and ready for scheduling", "success");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to confirm order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    }
  };

  /**
   * Step 3a: Show Schedule Preview (before calling SimAL)
   * Shows the order details and asks user to confirm scheduling
   */
  const handleShowSchedulePreview = (order) => {
    setOrderToSchedule(order);
    setShowSchedulePreview(true);
  };

  /**
   * Step 3b: Execute Schedule with SimAL (after user confirms preview)
   */
  const handleConfirmSchedule = async () => {
    if (!orderToSchedule) return;
    
    setShowSchedulePreview(false);
    await handleScheduleWithSimAL(orderToSchedule);
    setOrderToSchedule(null);
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
      // Call SimAL to schedule the production order
      const response = await api.post(`/simal/schedule-production-order/${order.id}`);
      
      // Update production order with SimAL schedule ID and set status to SCHEDULED
      try {
        await api.patch(`/production-orders/${order.id}/schedule`, {
          simalScheduleId: response.data.scheduleId,
          status: "SCHEDULED"
        });
        setSuccess(`Production order ${order.productionOrderNumber} successfully scheduled. Click Submit to create control orders.`);
        addNotification(`Order ${order.productionOrderNumber} scheduled - ready for submission`, "success");
      } catch (updateErr) {
        console.warn('Failed to update production order status, but SimAL schedule was created:', updateErr);
        setSuccess(`SimAL schedule ${response.data.scheduleId} created. Refresh page to see updated order status.`);
        addNotification(`Schedule created: ${response.data.scheduleId}`, "success");
      }
      
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
      { value: statusCounts.CREATED || 0, label: 'Pending', variant: 'pending', icon: '⏳' },
      { value: statusCounts.CONFIRMED || 0, label: 'Confirmed', variant: 'info', icon: '✓' },
      { value: statusCounts.SCHEDULED || 0, label: 'Scheduled', variant: 'processing', icon: '📅' },
      { value: statusCounts.DISPATCHED || 0, label: 'Dispatched', variant: 'info', icon: '🚀' },
      { value: statusCounts.IN_PRODUCTION || 0, label: 'In Production', variant: 'warning', icon: '⚙️' },
      { value: statusCounts.COMPLETED || 0, label: 'Completed', variant: 'success', icon: '✅' },
      { value: scheduledOrders.length, label: 'Active Schedules', variant: 'info', icon: '📊' },
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
        { value: 'CONFIRMED', label: 'Confirmed' },
        { value: 'SCHEDULED', label: 'Scheduled' },
        { value: 'DISPATCHED', label: 'Dispatched' },
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
          onConfirm={(orderId) => handleConfirmOrder(orderId)}
          onSchedule={(order) => {
            setSelectedOrder(order);
            handleShowSchedulePreview(order);
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

  // Render Schedule Preview Modal (Step 3a - before SimAL call)
  const renderSchedulePreviewModal = () => {
    if (!showSchedulePreview || !orderToSchedule) return null;

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
          maxWidth: "600px",
          width: "90%",
          boxShadow: "0 10px 25px rgba(0, 0, 0, 0.2)"
        }}>
          <h2 style={{ marginTop: 0, color: "#8b5cf6" }}>📅 Confirm Scheduling</h2>
          
          <p style={{ color: "#4b5563", marginBottom: "1rem" }}>
            You are about to schedule the following production order with SimAL. 
            This will generate a schedule with tasks for the appropriate workstations.
          </p>

          <div style={{ 
            marginBottom: "1.5rem", 
            padding: "1rem", 
            backgroundColor: "#f3f4f6", 
            borderRadius: "0.375rem" 
          }}>
            <p style={{ margin: "0.25rem 0" }}><strong>Order Number:</strong> {orderToSchedule.productionOrderNumber}</p>
            <p style={{ margin: "0.25rem 0" }}><strong>Priority:</strong> {orderToSchedule.priority || 'NORMAL'}</p>
            <p style={{ margin: "0.25rem 0" }}><strong>Due Date:</strong> {orderToSchedule.dueDate ? new Date(orderToSchedule.dueDate).toLocaleDateString() : 'Not set'}</p>
            {orderToSchedule.sourceWarehouseOrderId && (
              <p style={{ margin: "0.25rem 0" }}><strong>Source Warehouse Order:</strong> WO-{orderToSchedule.sourceWarehouseOrderId}</p>
            )}
            {orderToSchedule.notes && (
              <p style={{ margin: "0.25rem 0" }}><strong>Notes:</strong> {orderToSchedule.notes}</p>
            )}
          </div>

          {orderToSchedule.productionOrderItems && orderToSchedule.productionOrderItems.length > 0 && (
            <>
              <h4 style={{ marginBottom: "0.5rem" }}>Items to Produce ({orderToSchedule.productionOrderItems.length})</h4>
              <div style={{ maxHeight: "200px", overflow: "auto", marginBottom: "1rem" }}>
                <table style={{ width: "100%", fontSize: "0.875rem", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ backgroundColor: "#f9fafb", borderBottom: "2px solid #e5e7eb" }}>
                      <th style={{ padding: "0.5rem", textAlign: "left" }}>Item</th>
                      <th style={{ padding: "0.5rem", textAlign: "right" }}>Qty</th>
                      <th style={{ padding: "0.5rem", textAlign: "left" }}>Type</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orderToSchedule.productionOrderItems.map((item, index) => (
                      <tr key={index} style={{ borderBottom: "1px solid #e5e7eb" }}>
                        <td style={{ padding: "0.5rem" }}>{item.itemName}</td>
                        <td style={{ padding: "0.5rem", textAlign: "right" }}>{item.quantity}</td>
                        <td style={{ padding: "0.5rem" }}>{item.workstationType}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end" }}>
            <button
              onClick={handleConfirmSchedule}
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
              📅 Schedule with SimAL
            </button>
            <button
              onClick={() => {
                setShowSchedulePreview(false);
                setOrderToSchedule(null);
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
              Cancel
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
      {renderSchedulePreviewModal()}
    </>
  );
}

export default ProductionPlanningDashboard;
