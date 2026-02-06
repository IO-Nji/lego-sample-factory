import { useState, useEffect, useCallback } from "react";
import api from "../../api/api";
import { logger } from "../../utils/logger";
import { 
  CompactScheduleTimeline, 
  OrdersGrid,
  GridCard,
  CompactCard,
  ListRow,
  Button, 
  DashboardPanel,
  Footer
} from "../../components";
import DashboardHeader from "../../components/dashboard/DashboardHeader";
import AdminActivityLog from "../../components/AdminActivityLog";
import "../../styles/WorkstationDashboard.css";
import "../../styles/DashboardHeader.css";
import "../../styles/DashboardPanels.css";
import "./AdminDashboard.css";

// Order actions for production orders
const ORDER_ACTIONS = [
  { type: 'CONFIRM', label: 'Confirm', icon: '✓', variant: 'info', showFor: ['CREATED'] },
  { type: 'SCHEDULE', label: 'Schedule', icon: '📅', variant: 'primary', showFor: ['CONFIRMED'] },
  { type: 'DISPATCH', label: 'Dispatch', icon: '🚀', variant: 'success', showFor: ['SCHEDULED'] },
  { type: 'COMPLETE', label: 'Complete', icon: '✅', variant: 'success', showFor: ['IN_PRODUCTION'] },
  { type: 'CANCEL', label: 'Cancel', icon: '✕', variant: 'danger', showFor: ['CREATED', 'CONFIRMED'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'CREATED', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'DISPATCHED', label: 'Dispatched' },
  { value: 'IN_PRODUCTION', label: 'In Production' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' }
];

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
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [schedulingInProgress, setSchedulingInProgress] = useState({});
  const [error, setError] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [showSchedulePreview, setShowSchedulePreview] = useState(false);
  const [orderToSchedule, setOrderToSchedule] = useState(null);
  const [success, setSuccess] = useState(null);

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
    setProcessingOrderId(orderId);
    setError(null);
    
    try {
      await api.post(`/production-orders/${orderId}/confirm`);
      addNotification("Order confirmed and ready for scheduling", "success");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to confirm order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setProcessingOrderId(null);
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
      logger.debug('ProductionPlanning', 'Fetched scheduled orders', orders.length);
      setScheduledOrders(orders);
      
      // Extract and format all tasks for timeline
      const allTasks = [];
      orders.forEach(order => {
        logger.debug('ProductionPlanning', `Processing order ${order.orderNumber}`, { status: order.status, tasks: order.scheduledTasks?.length || 0 });
        if (order.scheduledTasks && Array.isArray(order.scheduledTasks)) {
          order.scheduledTasks.forEach(task => {
            const formattedTask = {
              ...task,
              id: task.taskId,
              workstationName: task.workstationName,
              startTime: task.startTime,
              endTime: task.endTime,
              status: task.status || 'SCHEDULED', // Use task's own status, not order status
              orderId: order.id,
              scheduleId: order.scheduleId
            };
            logger.debug('ProductionPlanning', `Task ${task.taskId}`, { ws: task.workstationName, status: task.status });
            allTasks.push(formattedTask);
          });
        }
      });
      logger.debug('ProductionPlanning', 'Total tasks extracted', allTasks.length);
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
      logger.info('ProductionPlanning', `Scheduling production order ${order.productionOrderNumber}`, order.id);
      const response = await api.post(`/simal/schedule-production-order/${order.id}`);
      logger.success('ProductionPlanning', 'SimAL schedule response', response.data);
      
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
      
      // Allow a brief moment for backend to finalize the data
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Refresh both order lists and scheduled tasks
      logger.debug('ProductionPlanning', 'Refreshing production orders and scheduled data');
      await fetchProductionOrders();
      await fetchScheduledOrders();
      logger.success('ProductionPlanning', 'Data refresh complete');
    } catch (err) {
      console.error('Scheduling error:', err);
      const errorMsg = err.response?.data?.message || err.message || "Failed to schedule with SimAL";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setSchedulingInProgress(prev => ({ ...prev, [order.id]: false }));
      setProcessingOrderId(null);
    }
  };

  const handleDispatchProduction = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-planning/${orderId}/dispatch`);
      addNotification("Production dispatched to workstations", "success");
      await fetchProductionOrders();
      await fetchScheduledOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to dispatch production";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCancelOrder = async (orderId) => {
    if (!window.confirm("Are you sure you want to cancel this production order?")) {
      return;
    }

    setProcessingOrderId(orderId);
    try {
      await api.patch(`/production-orders/${orderId}/cancel`);
      addNotification("Production order cancelled", "warning");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to cancel order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  /**
   * Complete a production order with full orchestration:
   * - Credits Modules Supermarket with produced modules
   * - Notifies source WarehouseOrder that modules are ready
   * - WarehouseOrder can then proceed to fulfill and create Final Assembly orders
   */
  const handleCompleteProductionOrder = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-orders/${orderId}/complete-with-notification`);
      addNotification("Production completed - Modules Supermarket credited", "success");
      await fetchProductionOrders();
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message || "Failed to complete production order";
      setError(errorMsg);
      addNotification(errorMsg, "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Unified action handler for OrdersGrid
  const handleAction = useCallback((actionType, orderId, order) => {
    switch (actionType) {
      case 'CONFIRM': handleConfirmOrder(orderId); break;
      case 'SCHEDULE': 
        setSelectedOrder(order);
        handleShowSchedulePreview(order);
        break;
      case 'DISPATCH': handleDispatchProduction(orderId); break;
      case 'COMPLETE': handleCompleteProductionOrder(orderId); break;
      case 'CANCEL': handleCancelOrder(orderId); break;
      default: logger.warn('ProductionPlanning', 'Unknown action:', actionType);
    }
  }, []);

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

  // Stats data for panel row
  const statusCounts = productionOrders.reduce((acc, order) => {
    acc[order.status] = (acc[order.status] || 0) + 1;
    return acc;
  }, {});
  const totalTasks = scheduledOrders.reduce((sum, s) => sum + (s.scheduledTasks?.length || 0), 0);

  // Render order extra info (priority, schedule status)
  const renderOrderExtra = (order) => (
    <>
      {order.priority && order.priority !== 'NORMAL' && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: order.priority === 'URGENT' ? 'rgba(239, 68, 68, 0.1)' :
                     order.priority === 'HIGH' ? 'rgba(245, 158, 11, 0.1)' :
                     'rgba(59, 130, 246, 0.1)',
          color: order.priority === 'URGENT' ? '#dc2626' :
                 order.priority === 'HIGH' ? '#d97706' :
                 '#2563eb',
          fontWeight: 600
        }}>
          {order.priority}
        </span>
      )}
      {order.simalScheduleId && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: 'rgba(139, 92, 246, 0.1)',
          color: '#7c3aed',
          fontWeight: 600
        }}>
          SimAL: {order.simalScheduleId}
        </span>
      )}
    </>
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

  // Handle task click for timeline
  const handleTaskClick = (task) => {
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

  if (loading && productionOrders.length === 0) {
    return (
      <div className="admin-page">
        <div className="admin-loading">
          <div className="admin-spinner" />
          <p>Loading Production Planning...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="admin-bg" />
      
      {/* Header */}
      <DashboardHeader
        icon="📅"
        title="Production Planning"
        subtitle="Factory Scheduling • SimAL Integration"
        onRefresh={async () => {
          await fetchProductionOrders();
          await fetchScheduledOrders();
        }}
        themeClass="ws-theme-planning"
      />

      {/* Error/Success Messages */}
      {error && (
        <div className="admin-error">
          ⚠️ {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}
      {success && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0.625rem 1rem',
          background: 'rgba(209, 250, 229, 0.9)',
          border: '1px solid rgba(16, 185, 129, 0.4)',
          borderRadius: '8px',
          color: '#065f46',
          fontSize: '0.8125rem',
          fontWeight: 500,
          zIndex: 1
        }}>
          ✅ {success}
          <button onClick={() => setSuccess(null)} style={{ background: 'none', border: 'none', color: '#065f46', cursor: 'pointer', fontSize: '1rem' }}>✕</button>
        </div>
      )}

      {/* Row 1: Stats Panels */}
      <div className="admin-grid">
        {/* Production Orders Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📋</span>
            <h2 className="admin-panel__title">Production Orders</h2>
            <span className="admin-panel__badge">{productionOrders.length}</span>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat admin-stat--pending">
              <span className="admin-stat__val">{statusCounts.CREATED || 0}</span>
              <span className="admin-stat__lbl">Pending</span>
            </div>
            <div className="admin-stat admin-stat--processing">
              <span className="admin-stat__val">{statusCounts.CONFIRMED || 0}</span>
              <span className="admin-stat__lbl">Confirmed</span>
            </div>
            <div className="admin-stat admin-stat--highlight">
              <span className="admin-stat__val">{statusCounts.SCHEDULED || 0}</span>
              <span className="admin-stat__lbl">Scheduled</span>
            </div>
            <div className="admin-stat admin-stat--rate">
              <span className="admin-stat__val">{statusCounts.IN_PRODUCTION || 0}</span>
              <span className="admin-stat__lbl">Active</span>
            </div>
          </div>
        </div>

        {/* SimAL Scheduling Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📅</span>
            <h2 className="admin-panel__title">SimAL Scheduling</h2>
            <span className="admin-panel__badge admin-panel__badge--active">{scheduledOrders.length} schedules</span>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat">
              <span className="admin-stat__val">{scheduledOrders.length}</span>
              <span className="admin-stat__lbl">Schedules</span>
            </div>
            <div className="admin-stat">
              <span className="admin-stat__val">{totalTasks}</span>
              <span className="admin-stat__lbl">Tasks</span>
            </div>
            <div className="admin-stat admin-stat--completed">
              <span className="admin-stat__val">✓</span>
              <span className="admin-stat__lbl">SimAL</span>
            </div>
          </div>
        </div>

        {/* Dispatch Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">🚀</span>
            <h2 className="admin-panel__title">Dispatch</h2>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat admin-stat--processing">
              <span className="admin-stat__val">{statusCounts.DISPATCHED || 0}</span>
              <span className="admin-stat__lbl">Dispatched</span>
            </div>
            <div className="admin-stat admin-stat--completed">
              <span className="admin-stat__val">{statusCounts.COMPLETED || 0}</span>
              <span className="admin-stat__lbl">Done</span>
            </div>
          </div>
        </div>

        {/* Overview Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📊</span>
            <h2 className="admin-panel__title">Overview</h2>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat admin-stat--rate">
              <span className="admin-stat__val">
                {productionOrders.length > 0 ? `${Math.round((statusCounts.COMPLETED || 0) / productionOrders.length * 100)}%` : '—'}
              </span>
              <span className="admin-stat__lbl">Efficiency</span>
            </div>
            <div className={`admin-stat ${(statusCounts.CANCELLED || 0) > 0 ? 'admin-stat--warning' : ''}`}>
              <span className="admin-stat__val">{statusCounts.CANCELLED || 0}</span>
              <span className="admin-stat__lbl">Cancelled</span>
            </div>
          </div>
        </div>
      </div>

      {/* Row 2: Timeline + Activity (same as Admin) */}
      <div className="admin-timeline-row">
        <div className="admin-timeline-section glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📅</span>
            <h2 className="admin-panel__title">Production Schedule</h2>
          </div>
          <div className="admin-timeline">
            <CompactScheduleTimeline
              scheduledTasks={scheduledTasks}
              showTitle={false}
              showCurrentTime={true}
              showMiniControls={true}
              onTaskClick={handleTaskClick}
            />
          </div>
        </div>
        
        <div className="admin-activity-section glass">
          <AdminActivityLog maxItems={20} />
        </div>
      </div>

      {/* Row 3: Orders Table */}
      <div className="glass" style={{ padding: '0.875rem', zIndex: 1 }}>
        <OrdersGrid
          title="Production Orders"
          icon="📋"
          orders={productionOrders}
          filterOptions={FILTER_OPTIONS}
          sortOptions={SORT_OPTIONS}
          searchPlaceholder="Search PRO-XXX..."
          emptyMessage="No production orders"
          emptySubtext="Orders will appear here when created by Modules Supermarket"
          searchKeys={['productionOrderNumber', 'notes']}
          renderCard={(order) => (
            <GridCard
              order={order}
              onAction={(type, id) => handleAction(type, id, order)}
              actions={ORDER_ACTIONS}
              renderExtra={renderOrderExtra}
              isProcessing={processingOrderId === order.id || schedulingInProgress[order.id]}
            />
          )}
          renderCompactCard={(order) => (
            <CompactCard
              order={order}
              onAction={(type, id) => handleAction(type, id, order)}
              actions={ORDER_ACTIONS}
              isProcessing={processingOrderId === order.id || schedulingInProgress[order.id]}
            />
          )}
          renderListItem={(order) => (
            <ListRow
              order={order}
              onAction={(type, id) => handleAction(type, id, order)}
              actions={ORDER_ACTIONS}
              columns={['orderNumber', 'status', 'date', 'priority']}
              isProcessing={processingOrderId === order.id || schedulingInProgress[order.id]}
            />
          )}
        />
      </div>

      <Footer />
      {renderSchedulePreviewModal()}
    </div>
  );
}

export default ProductionPlanningDashboard;
