import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { DashboardLayout, StatCard, Button, Card, Badge } from "../../components";
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
  
  // Modal states
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showSupplyOrderModal, setShowSupplyOrderModal] = useState(false);
  const [supplyOrderForm, setSupplyOrderForm] = useState({
    parts: [{ partId: "", quantityRequested: 1, unit: "piece", notes: "" }],
    priority: "MEDIUM",
    notes: ""
  });

  // Fetch control orders for the production control workstation
  const fetchControlOrders = async () => {
    const workstationId = session?.user?.workstation?.id;
    if (!workstationId) {
      setControlOrders([]);
      setFilteredOrders([]);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await api.get(`/api/production-control-orders/workstation/${workstationId}`);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setControlOrders(ordersList);
      applyFilter(ordersList, filterStatus);
    } catch (err) {
      setError("Failed to load control orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Fetch supply orders for this workstation
  const fetchSupplyOrders = async () => {
    const workstationId = session?.user?.workstation?.id;
    if (!workstationId) {
      setSupplyOrders([]);
      return;
    }

    try {
      const response = await api.get(`/api/supply-orders/workstation/${workstationId}`);
      setSupplyOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to load supply orders:", err);
    }
  };

  useEffect(() => {
    if (session?.user?.workstation?.id) {
      fetchControlOrders();
      fetchSupplyOrders();
      const interval = setInterval(() => {
        fetchControlOrders();
        fetchSupplyOrders();
      }, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstation?.id]);

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

  const handleStartProduction = async (orderId) => {
    try {
      await api.post(`/api/production-control-orders/${orderId}/start`);
      setSuccess("Production started successfully");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to start production: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCompleteProduction = async (orderId) => {
    try {
      await api.post(`/api/production-control-orders/${orderId}/complete`);
      setSuccess("Production completed successfully");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to complete production: " + (err.response?.data?.message || err.message));
    }
  };

  const handleHaltProduction = async (orderId, reason) => {
    try {
      await api.post(`/api/production-control-orders/${orderId}/halt`, { reason });
      setSuccess("Production halted");
      fetchControlOrders();
    } catch (err) {
      setError("Failed to halt production: " + (err.response?.data?.message || err.message));
    }
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  const handleCreateSupplyOrder = (order) => {
    setSelectedOrder(order);
    setSupplyOrderForm({
      parts: [{ partId: "1", quantityRequested: 10, unit: "piece", notes: "Plastic parts" }],
      priority: order.priority || "MEDIUM",
      notes: `Parts for Control Order ${order.controlOrderNumber}`
    });
    setShowSupplyOrderModal(true);
  };

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
    if (!selectedOrder || supplyOrderForm.parts.some(p => !p.partId || p.quantityRequested <= 0)) {
      setError("Please fill in all required fields");
      return;
    }

    try {
      const requestBody = {
        sourceControlOrderId: selectedOrder.id,
        sourceControlOrderType: "PRODUCTION",
        requestingWorkstationId: session?.user?.workstation?.id,
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

      await api.post('/api/supply-orders', requestBody);
      
      setSuccess("Supply order created successfully");
      setShowSupplyOrderModal(false);
      fetchSupplyOrders();
    } catch (err) {
      setError("Failed to create supply order: " + (err.response?.data?.message || err.message));
    }
  };

  // Stats cards
  const renderStatsCards = () => {
    const total = controlOrders.length;
    const assigned = controlOrders.filter(o => o.status === "ASSIGNED").length;
    const inProgress = controlOrders.filter(o => o.status === "IN_PROGRESS").length;
    const completed = controlOrders.filter(o => o.status === "COMPLETED").length;

    return (
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "1rem", marginBottom: "1.5rem" }}>
        <StatCard value={total} label="Total Orders" variant="primary" />
        <StatCard value={assigned} label="Assigned" variant="info" />
        <StatCard value={inProgress} label="In Progress" variant="warning" />
        <StatCard value={completed} label="Completed" variant="success" />
      </div>
    );
  };

  // Filter controls
  const renderFilterControls = () => (
    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
      <select 
        value={filterStatus}
        onChange={(e) => setFilterStatus(e.target.value)}
        style={{ padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db", fontSize: "0.875rem" }}
      >
        <option value="ALL">All Orders</option>
        <option value="ASSIGNED">Assigned</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="COMPLETED">Completed</option>
        <option value="HALTED">Halted</option>
        <option value="ABANDONED">Abandoned</option>
      </select>
      <Button 
        variant="secondary"
        size="small"
        onClick={fetchControlOrders} 
        disabled={loading}
      >
        {loading ? "Refreshing..." : "Refresh"}
      </Button>
    </div>
  );

  // Control orders display
  const renderControlOrders = () => {
    if (loading && controlOrders.length === 0) {
      return <div className="dashboard-empty-state">Loading control orders...</div>;
    }

    if (filteredOrders.length === 0) {
      return (
        <div className="dashboard-empty-state">
          <p className="dashboard-empty-state-title">No control orders</p>
          <p className="dashboard-empty-state-text">
            {filterStatus !== "ALL" 
              ? `No orders with status: ${filterStatus}` 
              : "Control orders will appear here when assigned by Production Planning"}
          </p>
        </div>
      );
    }

    return (
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
        {filteredOrders.map((order) => (
          <Card key={order.id} style={{ padding: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "0.75rem" }}>
              <div>
                <h3 style={{ fontSize: "1.125rem", fontWeight: "600", margin: "0 0 0.25rem 0" }}>
                  {order.controlOrderNumber}
                </h3>
                <p style={{ fontSize: "0.875rem", color: "#6b7280", margin: 0 }}>
                  From Production Order #{order.sourceProductionOrderId}
                </p>
              </div>
              <Badge 
                variant={
                  order.status === "COMPLETED" ? "success" :
                  order.status === "IN_PROGRESS" ? "warning" :
                  order.status === "HALTED" ? "danger" :
                  "default"
                }
              >
                {order.status}
              </Badge>
            </div>
            
            <div style={{ marginBottom: "0.75rem", fontSize: "0.875rem" }}>
              <div style={{ marginBottom: "0.25rem" }}>
                <span style={{ fontWeight: "500" }}>Priority:</span> {order.priority || "MEDIUM"}
              </div>
              <div style={{ marginBottom: "0.25rem" }}>
                <span style={{ fontWeight: "500" }}>Target Start:</span> {new Date(order.targetStartTime).toLocaleString()}
              </div>
              <div style={{ marginBottom: "0.25rem" }}>
                <span style={{ fontWeight: "500" }}>Target Completion:</span> {new Date(order.targetCompletionTime).toLocaleString()}
              </div>
            </div>
            
            <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
              {order.status === "ASSIGNED" && (
                <>
                  <Button variant="success" size="small" onClick={() => handleStartProduction(order.id)}>
                    Start Production
                  </Button>
                  <Button variant="primary" size="small" onClick={() => handleCreateSupplyOrder(order)}>
                    Request Parts
                  </Button>
                </>
              )}
              {order.status === "IN_PROGRESS" && (
                <>
                  <Button variant="success" size="small" onClick={() => handleCompleteProduction(order.id)}>
                    Complete
                  </Button>
                  <Button variant="warning" size="small" onClick={() => handleHaltProduction(order.id, "Operator initiated halt")}>
                    Halt
                  </Button>
                </>
              )}
              <Button variant="outline" size="small" onClick={() => handleViewDetails(order)}>
                View Details
              </Button>
            </div>
          </Card>
        ))}
      </div>
    );
  };

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
      <div className="modal">
        <div className="modal-overlay" onClick={() => setShowDetailsModal(false)} />
        <div className="modal-content">
          <div className="modal-header">
            <h2>Control Order: {selectedOrder.controlOrderNumber}</h2>
            <button onClick={() => setShowDetailsModal(false)} className="modal-close">×</button>
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
        <div className="modal-overlay" onClick={() => setShowSupplyOrderModal(false)} />
        <div className="modal-content">
          <div className="modal-header">
            <h2>Request Parts Supply</h2>
            <button onClick={() => setShowSupplyOrderModal(false)} className="modal-close">×</button>
          </div>
          <div className="modal-body">
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>Priority:</label>
              <select
                value={supplyOrderForm.priority}
                onChange={(e) => setSupplyOrderForm(prev => ({ ...prev, priority: e.target.value }))}
                style={{ width: "100%", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
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
                <div key={index} style={{ display: "flex", gap: "0.5rem", marginBottom: "0.5rem" }}>
                  <input
                    type="text"
                    placeholder="Part ID"
                    value={part.partId}
                    onChange={(e) => handlePartChange(index, 'partId', e.target.value)}
                    style={{ flex: 1, padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                  />
                  <input
                    type="number"
                    min="1"
                    placeholder="Quantity"
                    value={part.quantityRequested}
                    onChange={(e) => handlePartChange(index, 'quantityRequested', e.target.value)}
                    style={{ width: "100px", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                  />
                  <input
                    type="text"
                    placeholder="Unit"
                    value={part.unit}
                    onChange={(e) => handlePartChange(index, 'unit', e.target.value)}
                    style={{ width: "80px", padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db" }}
                  />
                  {supplyOrderForm.parts.length > 1 && (
                    <Button variant="danger" size="small" onClick={() => handleRemovePart(index)}>
                      Remove
                    </Button>
                  )}
                </div>
              ))}
              <Button variant="secondary" size="small" onClick={handleAddPart} style={{marginTop: '0.5rem'}}>
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
              />
            </div>

            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <Button variant="secondary" onClick={() => setShowSupplyOrderModal(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleSubmitSupplyOrder}>
                Create Supply Order
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <>
      <DashboardLayout
        title="Production Control"
        subtitle={`Manage manufacturing operations for ${session?.user?.workstation?.name || 'Production Control'}`}
        icon="🏭"
        layout="default"
        statsCards={renderStatsCards()}
        primaryContent={
          <div className="dashboard-box">
            <div className="dashboard-box-header dashboard-box-header-blue">
              <h2 className="dashboard-box-header-title">Control Orders</h2>
              {renderFilterControls()}
            </div>
            <div className="dashboard-box-content">
              {renderControlOrders()}
            </div>
          </div>
        }
        secondaryContent={
          <div className="dashboard-box">
            <div className="dashboard-box-header dashboard-box-header-orange">
              <h2 className="dashboard-box-header-title">Supply Orders</h2>
            </div>
            <div className="dashboard-box-content">
              {renderSupplyOrders()}
            </div>
          </div>
        }
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
