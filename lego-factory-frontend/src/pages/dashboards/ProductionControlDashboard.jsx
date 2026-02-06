import { useState, useEffect, useCallback } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { logger } from "../../utils/logger";
import { 
  OrdersGrid,
  GridCard,
  CompactCard,
  ListRow,
  Button, 
  Badge,
  DashboardPanel
} from "../../components";
import { WorkstationDashboard, ActivityPanel, DashboardPanelRow } from "../../components/dashboard";
import "../../styles/WorkstationDashboard.css";
import "../../styles/DashboardHeader.css";
import "../../styles/DashboardPanels.css";

// Order actions for control orders
const ORDER_ACTIONS = [
  { type: 'CONFIRM', label: 'Confirm', icon: '✓', variant: 'info', showFor: ['PENDING'] },
  { type: 'REQUEST_PARTS', label: 'Request Parts', icon: '📦', variant: 'primary', showFor: ['CONFIRMED'], condition: (order) => !order.supplyOrderId },
  { type: 'WAITING', label: 'Waiting for Parts', icon: '⏳', variant: 'default', showFor: ['CONFIRMED'], condition: (order) => order.supplyOrderId && order.supplyOrderStatus !== 'FULFILLED', disabled: true },
  { type: 'DISPATCH', label: 'Dispatch', icon: '🚀', variant: 'success', showFor: ['CONFIRMED'], condition: (order) => order.supplyOrderStatus === 'FULFILLED' },
  { type: 'START', label: 'Start', icon: '▶️', variant: 'primary', showFor: ['ASSIGNED'] },
  { type: 'COMPLETE', label: 'Complete', icon: '✅', variant: 'success', showFor: ['IN_PROGRESS'] },
  { type: 'HALT', label: 'Halt', icon: '⏸️', variant: 'warning', showFor: ['IN_PROGRESS'] },
  { type: 'RESUME', label: 'Resume', icon: '▶️', variant: 'info', showFor: ['HALTED'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'ASSIGNED', label: 'Assigned' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'HALTED', label: 'Halted' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' }
];

function ProductionControlDashboard() {
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'PROD-CTRL'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };
  
  // Modal states
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showSupplyOrderModal, setShowSupplyOrderModal] = useState(false);
  const [availableParts, setAvailableParts] = useState([]);
  const [modalError, setModalError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [supplyOrderForm, setSupplyOrderForm] = useState({
    parts: [{ partId: "", quantityRequested: 1, unit: "piece", notes: "" }],
    priority: "MEDIUM",
    notes: ""
  });

  // Fetch all production control orders (Production Control manages all manufacturing orders)
  // Then enrich with supply order status for proper button display
  const fetchControlOrders = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.get('/production-control-orders');
      const ordersList = Array.isArray(response.data) ? response.data : [];
      
      // Enrich orders with supply order status for button logic
      const enrichedOrders = await Promise.all(
        ordersList.map(async (order) => {
          // Only fetch supply orders for CONFIRMED orders (workflow: Confirm → Request Parts → Dispatch)
          if (order.status === 'CONFIRMED') {
            try {
              const supplyResponse = await api.get(`/supply-orders/source/${order.id}?type=PRODUCTION`);
              const supplyOrders = Array.isArray(supplyResponse.data) ? supplyResponse.data : [];
              
              if (supplyOrders.length > 0) {
                // Get the most recent supply order
                const latestSupply = supplyOrders[0];
                return {
                  ...order,
                  supplyOrderId: latestSupply.id,
                  supplyOrderStatus: latestSupply.status,
                  supplyOrderNumber: latestSupply.supplyOrderNumber,
                };
              }
            } catch (err) {
              // No supply orders or error - continue without enrichment
              console.debug(`No supply orders for control order ${order.id}`);
            }
          }
          return order;
        })
      );
      
      setControlOrders(enrichedOrders);
    } catch (err) {
      setError("Failed to load control orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Fetch available parts for supply order form
  const fetchAvailableParts = async () => {
    try {
      const response = await api.get('/masterdata/parts');
      setAvailableParts(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Failed to load parts:', err);
    }
  };

  // Fetch supply orders for this workstation
  const fetchSupplyOrders = async () => {
    const workstationId = session?.user?.workstationId;
    if (!workstationId) {
      setSupplyOrders([]);
      return;
    }

    try {
      const response = await api.get(`/supply-orders/workstation/${workstationId}`);
      setSupplyOrders(Array.isArray(response.data) ? response.data : []);;
    } catch (err) {
      console.error("Failed to load supply orders:", err);
    }
  };

  useEffect(() => {
    // Control users don't have a workstationId - they manage ALL manufacturing orders
    if (session?.user) {
      fetchControlOrders();
      fetchSupplyOrders();
      fetchAvailableParts();
      const interval = setInterval(() => {
        fetchControlOrders();
        fetchSupplyOrders();
      }, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [session?.user]);

  const handleConfirmOrder = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.put(`/production-control-orders/${orderId}/confirm`);
      addNotification("Order confirmed - ready to request parts", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to confirm order", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleStartProduction = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-control-orders/${orderId}/start`);
      addNotification("Production started", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to start production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to start production", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCompleteProduction = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-control-orders/${orderId}/complete`);
      addNotification("Production completed", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to complete production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to complete production", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleHaltProduction = async (orderId, reason) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-control-orders/${orderId}/halt`, { reason });
      addNotification("Production halted", "warning");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to halt production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to halt production", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleResumeProduction = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-control-orders/${orderId}/resume`);
      addNotification("Production resumed", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to resume production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to resume production", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Dispatch order to workstation (CONFIRMED → ASSIGNED)
  const handleDispatchToWorkstation = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/production-control-orders/${orderId}/dispatch`);
      addNotification("Order dispatched to workstation", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to dispatch order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to dispatch order", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  // Create supply order with automatic BOM lookup (no manual part selection needed)
  const handleCreateSupplyOrder = async (order) => {
    setProcessingOrderId(order.id);
    setError(null);
    
    try {
      const requestBody = {
        controlOrderId: order.id,
        controlOrderType: "PRODUCTION",
        priority: order.priority || "MEDIUM"
      };

      logger.info('ProductionControl', 'Creating supply order from control order', requestBody);
      await api.post('/supply-orders/from-control-order', requestBody);
      
      addNotification("Supply order created from BOM", "success");
      fetchSupplyOrders();
      fetchControlOrders(); // Refresh control orders to update card button states
    } catch (err) {
      console.error('Supply order creation error:', err);
      setError("Failed to create supply order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to create supply order", "error");
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Unified action handler for OrdersGrid
  const handleAction = useCallback((actionType, orderId, order) => {
    switch (actionType) {
      case 'CONFIRM': handleConfirmOrder(orderId); break;
      case 'START': handleStartProduction(orderId); break;
      case 'COMPLETE': handleCompleteProduction(orderId); break;
      case 'HALT': handleHaltProduction(orderId, "Operator initiated halt"); break;
      case 'RESUME': handleResumeProduction(orderId); break;
      case 'REQUEST_PARTS': handleCreateSupplyOrder(order); break;
      case 'DISPATCH': handleDispatchToWorkstation(orderId); break;
      default: logger.warn('ProductionControl', 'Unknown action:', actionType);
    }
  }, []);

  // Legacy handlers for manual part selection (kept for reference but not used)
  const handleAddPart = () => {
    setSupplyOrderForm(prev => ({
      ...prev,
      parts: [...prev.parts, { partId: "", quantityRequested: 1, unit: "piece", notes: "" }]
    }));
  };

  const handleRemovePart = (index) => {
    setSupplyOrderForm(prev => ({
      ...prev,
      parts: prev.parts.filter((_, i) => i !== index)
    }));
  };

  const handlePartChange = (index, field, value) => {
    setSupplyOrderForm(prev => ({
      ...prev,
      parts: prev.parts.map((part, i) => 
        i === index ? { ...part, [field]: value } : part
      )
    }));
  };

  const handleSubmitSupplyOrder = async () => {
    setModalError(null);
    
    // Validate required fields
    if (!selectedOrder) {
      setModalError("No order selected");
      return;
    }
    
    if (supplyOrderForm.parts.some(p => !p.partId || p.quantityRequested <= 0)) {
      setModalError("Please select a part and enter a valid quantity for all items");
      return;
    }
    
    if (!selectedOrder.assignedWorkstationId) {
      setModalError("Order has no assigned workstation - cannot create supply order");
      return;
    }

    setSubmitting(true);
    try {
      const requestBody = {
        sourceControlOrderId: selectedOrder.id,
        sourceControlOrderType: "PRODUCTION",
        requestingWorkstationId: selectedOrder.assignedWorkstationId,
        priority: supplyOrderForm.priority,
        requestedByTime: selectedOrder.targetStartTime,
        requiredItems: supplyOrderForm.parts.map(p => ({
          partId: parseInt(p.partId),
          quantityRequested: parseInt(p.quantityRequested),
          unit: p.unit,
          notes: p.notes
        })),
        notes: supplyOrderForm.notes
      };

      logger.info('ProductionControl', 'Creating supply order', requestBody);
      await api.post('/supply-orders', requestBody);
      
      setSuccess("Supply order created successfully");
      addNotification("Supply order created", "success");
      setShowSupplyOrderModal(false);
      fetchSupplyOrders();
      fetchControlOrders(); // Refresh control orders to update card button states
    } catch (err) {
      console.error('Supply order creation error:', err);
      setModalError("Failed to create supply order: " + (err.response?.data?.message || err.message));
    } finally {
      setSubmitting(false);
    }
  };

  // Stats data for panel row
  const total = controlOrders.length;
  const pending = controlOrders.filter(o => o.status === "PENDING").length;
  const confirmed = controlOrders.filter(o => o.status === "CONFIRMED").length;
  const assigned = controlOrders.filter(o => o.status === "ASSIGNED").length;
  const inProgress = controlOrders.filter(o => o.status === "IN_PROGRESS").length;
  const completed = controlOrders.filter(o => o.status === "COMPLETED").length;
  const halted = controlOrders.filter(o => o.status === "HALTED").length;
  const activeSupply = supplyOrders.filter(o => o.status !== "FULFILLED" && o.status !== "REJECTED").length;

  // Panel Row Content (4 panels like warehouse dashboards)
  const panelRowContent = (
    <>
      {/* Control Orders Panel */}
      <DashboardPanel
        icon="🏭"
        title="Control Orders"
        badge={total > 0 ? `${total}` : null}
        badgeVariant="info"
        themeClass="production-control-theme"
        stats={[
          { label: 'Pending', value: pending, variant: 'pending' },
          { label: 'Confirmed', value: confirmed, variant: 'info' },
          { label: 'Active', value: inProgress, variant: 'progress' },
          { label: 'Done', value: completed, variant: 'completed' },
        ]}
        alerts={halted > 0 ? [{ message: `${halted} halted`, variant: 'warning' }] : []}
      />

      {/* Supply Orders Panel */}
      <DashboardPanel
        icon="🚚"
        title="Supply Orders"
        badge={supplyOrders.length > 0 ? `${supplyOrders.length}` : null}
        badgeVariant={activeSupply > 0 ? "warning" : "default"}
        themeClass="production-control-theme"
        stats={[
          { label: 'Active', value: activeSupply, variant: 'warning' },
          { label: 'Fulfilled', value: supplyOrders.filter(o => o.status === "FULFILLED").length, variant: 'completed' },
        ]}
      />

      {/* Workstations Panel */}
      <DashboardPanel
        icon="⚙️"
        title="Workstations"
        themeClass="production-control-theme"
        stats={[
          { label: 'WS-1', value: controlOrders.filter(o => o.assignedWorkstationId === 1).length },
          { label: 'WS-2', value: controlOrders.filter(o => o.assignedWorkstationId === 2).length },
          { label: 'WS-3', value: controlOrders.filter(o => o.assignedWorkstationId === 3).length },
        ]}
        subStats={[{ icon: '🔄', label: 'Assigned', value: assigned }]}
      />

      {/* Status Panel */}
      <DashboardPanel
        icon="📊"
        title="Status"
        themeClass="production-control-theme"
        stats={[
          { label: 'Efficiency', value: total > 0 ? `${Math.round(completed / total * 100)}%` : '—', variant: completed > 0 ? 'completed' : 'default' },
          { label: 'Halted', value: halted, variant: halted > 0 ? 'warning' : 'default' },
        ]}
      />
    </>
  );

  // Render order extra info (workstation, priority)
  const renderOrderExtra = (order) => (
    <>
      {order.assignedWorkstationId && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: 'rgba(59, 130, 246, 0.1)',
          color: '#2563eb',
          fontWeight: 600
        }}>
          WS-{order.assignedWorkstationId}
        </span>
      )}
      {order.supplyOrderStatus && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: order.supplyOrderStatus === 'FULFILLED' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.1)',
          color: order.supplyOrderStatus === 'FULFILLED' ? '#059669' : '#d97706',
          fontWeight: 600
        }}>
          Supply: {order.supplyOrderStatus}
        </span>
      )}
    </>
  );

  // Render control orders using OrdersGrid
  const ordersContent = (
    <OrdersGrid
      title="Production Control Orders"
      icon="🏭"
      orders={controlOrders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search PCO-XXX..."
      emptyMessage="No control orders"
      emptySubtext="Control orders will appear here when assigned by Production Planning"
      searchKeys={['controlOrderNumber', 'productionInstructions']}
      renderCard={(order) => (
        <GridCard
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          renderExtra={renderOrderExtra}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderCompactCard={(order) => (
        <CompactCard
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderListItem={(order) => (
        <ListRow
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          columns={['orderNumber', 'status', 'date', 'priority']}
          isProcessing={processingOrderId === order.id}
        />
      )}
    />
  );

  // Supply orders display (compact for sidebar)
  const renderSupplyOrders = () => {
    return (
      <div className="ws-sidebar-section">
        <div className="ws-sidebar-header">
          <span className="ws-sidebar-title">🚚 Supply Orders</span>
          <span className="ws-sidebar-count">{supplyOrders.length}</span>
        </div>
        {supplyOrders.length === 0 ? (
          <div className="ws-sidebar-empty">No active supply orders</div>
        ) : (
          <div className="ws-sidebar-list">
            {supplyOrders.slice(0, 5).map((order) => (
              <div key={order.id} className="ws-sidebar-item">
                <div className="ws-sidebar-item-header">
                  <span className="ws-sidebar-item-title">{order.supplyOrderNumber}</span>
                  <Badge 
                    variant={
                      order.status === "FULFILLED" ? "success" :
                      order.status === "IN_PROGRESS" ? "warning" :
                      order.status === "REJECTED" ? "danger" :
                      "default"
                    }
                    size="small"
                  >
                    {order.status}
                  </Badge>
                </div>
                <div className="ws-sidebar-item-meta">
                  <span>{order.supplyOrderItems?.length || 0} items</span>
                  <span>•</span>
                  <span>{order.priority}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  // Details modal
  const renderDetailsModal = () => {
    if (!showDetailsModal || !selectedOrder) return null;

    return (
      <div className="modal-overlay" onClick={() => setShowDetailsModal(false)}>
        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <h2>Control Order: {selectedOrder.controlOrderNumber}</h2>
            <Button variant="ghost" size="small" onClick={() => setShowDetailsModal(false)} ariaLabel="Close modal">×</Button>
          </div>
          <div className="modal-body">
            <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "1rem" }}>
              <div><strong>Status:</strong> <Badge variant={
                selectedOrder.status === "COMPLETED" ? "success" :
                selectedOrder.status === "IN_PROGRESS" ? "warning" :
                "default"
              }>{selectedOrder.status}</Badge></div>
              <div><strong>Priority:</strong> {selectedOrder.priority || "MEDIUM"}</div>
              <div><strong>SimAL Schedule ID:</strong> {selectedOrder.simalScheduleId}</div>
              <div><strong>Source Production Order:</strong> #{selectedOrder.sourceProductionOrderId}</div>
              <div><strong>Target Start:</strong> {new Date(selectedOrder.targetStartTime).toLocaleString()}</div>
              <div><strong>Target Completion:</strong> {new Date(selectedOrder.targetCompletionTime).toLocaleString()}</div>
              {selectedOrder.actualStartTime && (
                <div><strong>Actual Start:</strong> {new Date(selectedOrder.actualStartTime).toLocaleString()}</div>
              )}
              {selectedOrder.actualCompletionTime && (
                <div><strong>Actual Completion:</strong> {new Date(selectedOrder.actualCompletionTime).toLocaleString()}</div>
              )}
              {selectedOrder.productionInstructions && (
                <div style={{gridColumn: '1 / -1'}}><strong>Production Instructions:</strong><p>{selectedOrder.productionInstructions}</p></div>
              )}
              {selectedOrder.qualityCheckpoints && (
                <div style={{gridColumn: '1 / -1'}}><strong>Quality Checkpoints:</strong><p>{selectedOrder.qualityCheckpoints}</p></div>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  };

  // Supply order creation modal
  const renderSupplyOrderModal = () => {
    if (!showSupplyOrderModal) return null;

    return (
      <div className="modal">
        <div className="modal-overlay" onClick={() => !submitting && setShowSupplyOrderModal(false)} />
        <div className="modal-content">
          <div className="modal-header">
            <h2>Request Parts Supply</h2>
            <button onClick={() => !submitting && setShowSupplyOrderModal(false)} className="modal-close">×</button>
          </div>
          <div className="modal-body">
            {/* Error display inside modal */}
            {modalError && (
              <div style={{ 
                padding: "0.75rem", 
                marginBottom: "1rem", 
                backgroundColor: "#fee2e2", 
                border: "1px solid #fecaca", 
                borderRadius: "0.375rem",
                color: "#dc2626"
              }}>
                {modalError}
              </div>
            )}
            
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>Priority:</label>
              <select
                value={supplyOrderForm.priority}
                onChange={(e) => setSupplyOrderForm(prev => ({ ...prev, priority: e.target.value }))}
                style={{ width: "100%", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                disabled={submitting}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>Parts Required:</label>
              {supplyOrderForm.parts.map((part, index) => (
                <div key={index} style={{ display: "flex", gap: "0.5rem", marginBottom: "0.5rem", alignItems: "center" }}>
                  <select
                    value={part.partId}
                    onChange={(e) => handlePartChange(index, 'partId', e.target.value)}
                    style={{ flex: 2, padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                    disabled={submitting}
                  >
                    <option value="">Select Part...</option>
                    {availableParts.map(p => (
                      <option key={p.id} value={p.id}>{p.name} (ID: {p.id})</option>
                    ))}
                  </select>
                  <input
                    type="number"
                    min="1"
                    placeholder="Qty"
                    value={part.quantityRequested}
                    onChange={(e) => handlePartChange(index, 'quantityRequested', e.target.value)}
                    style={{ width: "80px", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                    disabled={submitting}
                  />
                  {supplyOrderForm.parts.length > 1 && (
                    <Button variant="danger" size="small" onClick={() => handleRemovePart(index)} disabled={submitting}>
                      Remove
                    </Button>
                  )}
                </div>
              ))}
              <Button variant="secondary" size="small" onClick={handleAddPart} style={{marginTop: '0.5rem'}} disabled={submitting}>
                Add Part
              </Button>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>Notes:</label>
              <textarea
                value={supplyOrderForm.notes}
                onChange={(e) => setSupplyOrderForm(prev => ({ ...prev, notes: e.target.value }))}
                style={{ width: "100%", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db", minHeight: "80px" }}
                rows="3"
                disabled={submitting}
              />
            </div>

            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <Button variant="secondary" onClick={() => setShowSupplyOrderModal(false)} disabled={submitting}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleSubmitSupplyOrder} loading={submitting} disabled={submitting}>
                {submitting ? 'Creating...' : 'Create Supply Order'}
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // Activity content
  const activityContent = (
    <>
      <ActivityPanel
        title="Activity"
        notifications={notifications}
        onClear={clearNotifications}
        maxItems={5}
        compact={true}
      />
      {renderSupplyOrders()}
    </>
  );

  return (
    <>
      <WorkstationDashboard
        title="Production Control"
        subtitle="Manufacturing Operations • WS-1,2,3"
        icon="🏭"
        theme="production-control-theme"
        panelRowContent={panelRowContent}
        ordersContent={ordersContent}
        activityContent={activityContent}
        loading={loading}
        error={error}
        onRefresh={fetchControlOrders}
        onDismissError={() => setError(null)}
      />
      {renderDetailsModal()}
      {renderSupplyOrderModal()}
    </>
  );
}

export default ProductionControlDashboard;
