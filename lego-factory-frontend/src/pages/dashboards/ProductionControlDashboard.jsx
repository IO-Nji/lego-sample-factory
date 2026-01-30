import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid, 
  Button, 
  Card, 
  Badge,
  ProductionControlOrderCard 
} from "../../components";
import "../../styles/DashboardLayout.css";

function ProductionControlDashboard() {
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
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
  const fetchControlOrders = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.get('/production-control-orders');
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setControlOrders(ordersList);
      applyFilter(ordersList, filterStatus);
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

  useEffect(() => {
    applyFilter(controlOrders, filterStatus);
  }, [filterStatus, controlOrders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const handleConfirmOrder = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/confirm`);
      setSuccess("Order confirmed successfully");
      addNotification("Order confirmed - ready to request parts", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to confirm order", "error");
    }
  };

  const handleStartProduction = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/start`);
      setSuccess("Production started successfully");
      addNotification("Production started", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to start production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to start production", "error");
    }
  };

  const handleCompleteProduction = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/complete`);
      setSuccess("Production completed successfully");
      addNotification("Production completed", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to complete production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to complete production", "error");
    }
  };

  const handleHaltProduction = async (orderId, reason) => {
    try {
      await api.post(`/production-control-orders/${orderId}/halt`, { reason });
      setSuccess("Production halted");
      addNotification("Production halted", "warning");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to halt production: " + (err.response?.data?.message || err.message));
      addNotification("Failed to halt production", "error");
    }
  };

  // Dispatch order to workstation (CONFIRMED → ASSIGNED)
  const handleDispatchToWorkstation = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/dispatch`);
      setSuccess("Order dispatched to workstation");
      addNotification("Order dispatched to workstation", "success");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to dispatch order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to dispatch order", "error");
    }
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  // Create supply order with automatic BOM lookup (no manual part selection needed)
  const handleCreateSupplyOrder = async (order) => {
    setLoading(true);
    setError(null);
    
    try {
      const requestBody = {
        controlOrderId: order.id,
        controlOrderType: "PRODUCTION",
        priority: order.priority || "MEDIUM"
      };

      console.log('Creating supply order from control order:', requestBody);
      await api.post('/supply-orders/from-control-order', requestBody);
      
      setSuccess("Supply order created successfully - parts determined from BOM");
      addNotification("Supply order created from BOM", "success");
      fetchSupplyOrders();
      fetchControlOrders(); // Refresh control orders to update card button states
    } catch (err) {
      console.error('Supply order creation error:', err);
      setError("Failed to create supply order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to create supply order", "error");
    } finally {
      setLoading(false);
    }
  };

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

      console.log('Creating supply order with:', requestBody);
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

  // Stats cards
  // Stats data for StatisticsGrid
  const statsData = (() => {
    const total = controlOrders.length;
    const pending = controlOrders.filter(o => o.status === "PENDING").length;
    const confirmed = controlOrders.filter(o => o.status === "CONFIRMED").length;
    const assigned = controlOrders.filter(o => o.status === "ASSIGNED").length;
    const inProgress = controlOrders.filter(o => o.status === "IN_PROGRESS").length;
    const completed = controlOrders.filter(o => o.status === "COMPLETED").length;
    const halted = controlOrders.filter(o => o.status === "HALTED").length;

    return [
      { value: total, label: 'Total Orders', variant: 'default', icon: '📦' },
      { value: pending, label: 'Pending', variant: 'pending', icon: '⏳' },
      { value: confirmed, label: 'Confirmed', variant: 'info', icon: '✓' },
      { value: assigned, label: 'Assigned', variant: 'info', icon: '📝' },
      { value: inProgress, label: 'In Progress', variant: 'warning', icon: '⚙️' },
      { value: completed, label: 'Completed', variant: 'success', icon: '✅' },
      { value: halted, label: 'Halted', variant: 'warning', icon: '⏸️' },
      { value: supplyOrders.length, label: 'Supply Orders', variant: 'info', icon: '🚚' },
    ];
  })();

  // Render activity log using ActivityLog component
  const renderActivity = () => (
    <Card variant="framed" title="CONTROL ACTIVITY" style={{ height: '100%' }}>
      <ActivityLog
        notifications={notifications}
        onClear={clearNotifications}
        showTitle={false}
      />
    </Card>
  );

  // Render control orders using OrdersSection
  const renderControlOrders = () => (
    <OrdersSection
      title="Production Control Orders"
      icon="🏭"
      orders={controlOrders}
      filterOptions={[
        { value: 'ALL', label: 'All Orders' },
        { value: 'PENDING', label: 'Pending' },
        { value: 'CONFIRMED', label: 'Confirmed' },
        { value: 'ASSIGNED', label: 'Assigned' },
        { value: 'IN_PROGRESS', label: 'In Progress' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'HALTED', label: 'Halted' }
      ]}
      sortOptions={[
        { value: 'orderNumber', label: 'Order Number' },
        { value: 'priority', label: 'Priority' },
        { value: 'status', label: 'Status' }
      ]}
      searchKeys={['controlOrderNumber', 'assemblyInstructions']}
      sortKey="controlOrderNumber"
      renderCard={(order) => (
        <ProductionControlOrderCard
          key={order.id}
          order={order}
          onConfirm={handleConfirmOrder}
          onStart={handleStartProduction}
          onComplete={handleCompleteProduction}
          onHalt={(orderId) => handleHaltProduction(orderId, "Operator initiated halt")}
          onRequestParts={handleCreateSupplyOrder}
          onDispatch={handleDispatchToWorkstation}
          onViewDetails={handleViewDetails}
        />
      )}
      searchPlaceholder="Search by order number..."
      emptyMessage="Control orders will appear here when assigned by Production Planning"
    />
  );

  // Supply orders display
  const renderSupplyOrders = () => {
    if (supplyOrders.length === 0) {
      return (
        <div className="dashboard-empty-state">
          <p className="dashboard-empty-state-text">No active supply orders</p>
        </div>
      );
    }

    return (
      <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
        {supplyOrders.slice(0, 5).map((order) => (
          <Card key={order.id} style={{ padding: "0.75rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontWeight: "500", fontSize: "0.875rem" }}>{order.supplyOrderNumber}</span>
              <Badge 
                variant={
                  order.status === "FULFILLED" ? "success" :
                  order.status === "IN_PROGRESS" ? "warning" :
                  order.status === "REJECTED" ? "danger" :
                  "default"
                }
              >
                {order.status}
              </Badge>
            </div>
            <div style={{ marginTop: "0.5rem", fontSize: "0.75rem", color: "#6b7280", display: "flex", gap: "1rem" }}>
              <span>{order.supplyOrderItems?.length || 0} items</span>
              <span>Priority: {order.priority}</span>
            </div>
          </Card>
        ))}
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

  return (
    <>
      <StandardDashboardLayout
        title="Production Control"
        subtitle={`Manufacturing Operations | ${session?.user?.workstation?.name || 'Production Control'}`}
        icon="🏭"
        activityContent={renderActivity()}
        statsContent={<StatisticsGrid stats={statsData} />}
        formContent={renderSupplyOrders()}
        contentGrid={renderControlOrders()}
        inventoryContent={null}
        messages={{ error, success }}
        onDismissError={() => setError(null)}
        onDismissSuccess={() => setSuccess(null)}
      />
      {renderDetailsModal()}
      {renderSupplyOrderModal()}
    </>
  );
}

export default ProductionControlDashboard;
