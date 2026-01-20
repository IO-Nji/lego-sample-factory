import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { DashboardLayout, StatCard, Button, Card, Badge } from "../../components";
import "../../styles/DashboardLayout.css";

function PartsSupplyWarehouseDashboard() {
  const { session } = useAuth();
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [partsStock, setPartsStock] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterType, setFilterType] = useState("ALL"); // PRODUCTION, ASSEMBLY
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Modal states
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [showStockAdjustModal, setShowStockAdjustModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [selectedPart, setSelectedPart] = useState(null);
  const [stockAdjustForm, setStockAdjustForm] = useState({
    quantity: 0,
    reason: ""
  });

  // Fetch supply orders for Parts Supply Warehouse (workstation 9)
  const fetchSupplyOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/supply-orders/warehouse");
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setSupplyOrders(ordersList);
      applyFilters(ordersList, filterStatus, filterType);
    } catch (err) {
      setError("Failed to fetch supply orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  // Fetch parts stock for Parts Supply Warehouse (workstation 9)
  const fetchPartsStock = async () => {
    try {
      const response = await api.get("/stock/workstation/9");
      const stockList = Array.isArray(response.data) ? response.data : [];
      // Filter for parts only (itemType = PART)
      const partsOnly = stockList.filter(item => item.itemType === "PART");
      setPartsStock(partsOnly);
    } catch (err) {
      console.error("Failed to fetch parts stock:", err);
    }
  };

  useEffect(() => {
    fetchSupplyOrders();
    fetchPartsStock();
    const interval = setInterval(() => {
      fetchSupplyOrders();
      fetchPartsStock();
    }, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    applyFilters(supplyOrders, filterStatus, filterType);
  }, [filterStatus, filterType, supplyOrders]);

  const applyFilters = (ordersList, status, type) => {
    let filtered = ordersList;
    
    if (status !== "ALL") {
      filtered = filtered.filter(order => order.status === status);
    }
    
    if (type !== "ALL") {
      filtered = filtered.filter(order => order.sourceControlOrderType === type);
    }
    
    setFilteredOrders(filtered);
  };

  const handleFulfillOrder = async (orderId) => {
    try {
      await api.put(`/supply-orders/${orderId}/fulfill`);
      setSuccess(`Supply order #${orderId} fulfilled successfully`);
      fetchSupplyOrders();
      fetchPartsStock(); // Refresh stock after fulfillment
    } catch (err) {
      setError("Failed to fulfill supply order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleRejectOrder = async (orderId) => {
    const reason = prompt("Enter rejection reason:");
    if (!reason) return;
    
    try {
      await api.put(`/supply-orders/${orderId}/reject`, { reason });
      setSuccess(`Supply order #${orderId} rejected`);
      fetchSupplyOrders();
    } catch (err) {
      setError("Failed to reject supply order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  const handleStockAdjustment = (part) => {
    setSelectedPart(part);
    setStockAdjustForm({ quantity: 0, reason: "" });
    setShowStockAdjustModal(true);
  };

  const handleSubmitStockAdjustment = async () => {
    if (!selectedPart || !stockAdjustForm.reason) {
      setError("Please provide adjustment quantity and reason");
      return;
    }

    try {
      const newQuantity = selectedPart.quantity + stockAdjustForm.quantity;
      await api.post("/stock/update", null, {
        params: {
          workstationId: 9,
          itemType: "PART",
          itemId: selectedPart.itemId,
          quantity: newQuantity
        }
      });
      
      setSuccess(`Stock adjusted for ${selectedPart.itemName || `Part #${selectedPart.itemId}`}`);
      setShowStockAdjustModal(false);
      fetchPartsStock();
    } catch (err) {
      setError("Failed to adjust stock: " + (err.response?.data?.message || err.message));
    }
  };

  // Render Stats Cards
  const renderStatsCards = () => {
    const totalOrders = supplyOrders.length;
    const pendingOrders = supplyOrders.filter(o => o.status === "PENDING").length;
    const inProgressOrders = supplyOrders.filter(o => o.status === "IN_PROGRESS").length;
    const fulfilledOrders = supplyOrders.filter(o => o.status === "FULFILLED").length;
    const lowStockParts = partsStock.filter(p => p.quantity < 50).length;

    return (
      <>
        <StatCard value={totalOrders} label="Total Orders" icon="📦" variant="primary" />
        <StatCard value={pendingOrders} label="Pending" icon="⏳" variant="warning" />
        <StatCard value={inProgressOrders} label="In Progress" icon="🔄" variant="info" />
        <StatCard value={fulfilledOrders} label="Fulfilled" icon="✓" variant="success" />
        <StatCard value={lowStockParts} label="Low Stock Parts" icon="⚠️" variant="danger" threshold={5} thresholdType="high" />
      </>
    );
  };

  // Render Supply Orders Section
  const renderSupplyOrdersSection = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-orange">
        <h2 className="dashboard-box-header-title">📦 Supply Orders</h2>
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
            <option value="ALL">All Status</option>
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="FULFILLED">Fulfilled</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <select 
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            style={{ 
              padding: "0.5rem", 
              borderRadius: "0.375rem", 
              border: "1px solid #d1d5db",
              fontSize: "0.875rem"
            }}
          >
            <option value="ALL">All Types</option>
            <option value="PRODUCTION">Production Supply</option>
            <option value="ASSEMBLY">Assembly Supply</option>
          </select>
          <button 
            onClick={() => { fetchSupplyOrders(); fetchPartsStock(); }} 
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
            <p className="dashboard-empty-state-text">Loading supply orders...</p>
          </div>
        ) : filteredOrders.length === 0 ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No supply orders found</p>
            <p className="dashboard-empty-state-text">
              {filterStatus !== "ALL" || filterType !== "ALL"
                ? `No orders matching the selected filters` 
                : "Orders will appear here when production/assembly control requests parts"}
            </p>
          </div>
        ) : (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <Card key={order.id} variant="default">
                <div style={{ padding: "1rem" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "0.75rem" }}>
                    <div>
                      <h3 style={{ fontWeight: "600", fontSize: "1rem", marginBottom: "0.25rem" }}>
                        {order.supplyOrderNumber}
                      </h3>
                      <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
                        <Badge variant={
                          order.status === "FULFILLED" ? "success" : 
                          order.status === "REJECTED" ? "danger" : 
                          order.status === "CANCELLED" ? "secondary" :
                          order.status === "IN_PROGRESS" ? "info" : "warning"
                        }>
                          {order.status}
                        </Badge>
                        <Badge variant={order.sourceControlOrderType === "PRODUCTION" ? "primary" : "info"}>
                          {order.sourceControlOrderType}
                        </Badge>
                        {order.priority && (
                          <Badge variant={
                            order.priority === "URGENT" ? "danger" :
                            order.priority === "HIGH" ? "warning" : "secondary"
                          }>
                            {order.priority}
                          </Badge>
                        )}
                      </div>
                    </div>
                  </div>

                  <div style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.75rem" }}>
                    <div><strong>Workstation:</strong> WS-{order.requestingWorkstationId}</div>
                    <div><strong>Items:</strong> {order.supplyOrderItems?.length || 0} part types</div>
                    {order.requestedByTime && (
                      <div><strong>Requested By:</strong> {new Date(order.requestedByTime).toLocaleString()}</div>
                    )}
                    {order.notes && (
                      <div style={{ marginTop: "0.5rem", fontStyle: "italic" }}>{order.notes}</div>
                    )}
                  </div>

                  <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                    <Button 
                      variant="secondary" 
                      size="small" 
                      onClick={() => handleViewDetails(order)}
                    >
                      View Details
                    </Button>
                    {order.status === "PENDING" && (
                      <>
                        <Button 
                          variant="success" 
                          size="small"
                          onClick={() => handleFulfillOrder(order.id)}
                        >
                          Fulfill
                        </Button>
                        <Button 
                          variant="danger" 
                          size="small"
                          onClick={() => handleRejectOrder(order.id)}
                        >
                          Reject
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </>
  );

  // Render Parts Stock Management Section
  const renderPartsStockSection = () => (
    <div className="dashboard-box">
      <div className="dashboard-box-header dashboard-box-header-purple">
        <h2 className="dashboard-box-header-title">🔧 Parts Inventory</h2>
      </div>
      <div className="dashboard-box-content">
        {partsStock.length === 0 ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">No parts in inventory</p>
          </div>
        ) : (
          <div style={{ maxHeight: "400px", overflowY: "auto" }}>
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>Part Name</th>
                  <th>Part ID</th>
                  <th>Quantity</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {partsStock.map((part) => (
                  <tr key={part.id}>
                    <td>{part.itemName || `Part #${part.itemId}`}</td>
                    <td>#{part.itemId}</td>
                    <td>{part.quantity}</td>
                    <td>
                      <Badge variant={
                        part.quantity < 20 ? "danger" :
                        part.quantity < 50 ? "warning" : "success"
                      }>
                        {part.quantity < 20 ? "Critical" :
                         part.quantity < 50 ? "Low" : "OK"}
                      </Badge>
                    </td>
                    <td>
                      <Button 
                        variant="secondary" 
                        size="small"
                        onClick={() => handleStockAdjustment(part)}
                      >
                        Adjust Stock
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );

  // Render Details Modal
  const renderDetailsModal = () => {
    if (!showDetailsModal || !selectedOrder) return null;

    return (
      <div className="modal-overlay-custom" onClick={() => setShowDetailsModal(false)}>
        <div className="modal-content-custom" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header-custom">
            <h2>Supply Order Details</h2>
            <button onClick={() => setShowDetailsModal(false)} className="modal-close-custom">×</button>
          </div>
          <div className="modal-body-custom">
            <div style={{ marginBottom: "1rem" }}>
              <strong>Order Number:</strong> {selectedOrder.supplyOrderNumber}
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>Status:</strong> <Badge variant={selectedOrder.status === "FULFILLED" ? "success" : "warning"}>{selectedOrder.status}</Badge>
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>Type:</strong> <Badge>{selectedOrder.sourceControlOrderType}</Badge>
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>Requesting Workstation:</strong> WS-{selectedOrder.requestingWorkstationId}
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>Priority:</strong> {selectedOrder.priority || "MEDIUM"}
            </div>
            {selectedOrder.requestedByTime && (
              <div style={{ marginBottom: "1rem" }}>
                <strong>Requested By:</strong> {new Date(selectedOrder.requestedByTime).toLocaleString()}
              </div>
            )}
            {selectedOrder.notes && (
              <div style={{ marginBottom: "1rem" }}>
                <strong>Notes:</strong> {selectedOrder.notes}
              </div>
            )}
            
            <h3 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>Required Parts:</h3>
            {selectedOrder.supplyOrderItems && selectedOrder.supplyOrderItems.length > 0 ? (
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Part ID</th>
                    <th>Quantity</th>
                    <th>Unit</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.supplyOrderItems.map((item, idx) => (
                    <tr key={idx}>
                      <td>#{item.partId}</td>
                      <td>{item.quantityRequested}</td>
                      <td>{item.unit || "piece"}</td>
                      <td>{item.notes || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No items listed</p>
            )}
          </div>
          <div className="modal-footer-custom">
            <Button variant="secondary" onClick={() => setShowDetailsModal(false)}>
              Close
            </Button>
          </div>
        </div>
      </div>
    );
  };

  // Render Stock Adjustment Modal
  const renderStockAdjustModal = () => {
    if (!showStockAdjustModal || !selectedPart) return null;

    return (
      <div className="modal-overlay-custom" onClick={() => setShowStockAdjustModal(false)}>
        <div className="modal-content-custom" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header-custom">
            <h2>Adjust Parts Stock</h2>
            <button onClick={() => setShowStockAdjustModal(false)} className="modal-close-custom">×</button>
          </div>
          <div className="modal-body-custom">
            <div style={{ marginBottom: "1rem" }}>
              <strong>Part:</strong> {selectedPart.itemName || `Part #${selectedPart.itemId}`}
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>Current Quantity:</strong> {selectedPart.quantity}
            </div>
            
            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Adjustment (+ to add, - to subtract):
              </label>
              <input
                type="number"
                value={stockAdjustForm.quantity}
                onChange={(e) => setStockAdjustForm(prev => ({ ...prev, quantity: parseInt(e.target.value) || 0 }))}
                style={{ 
                  width: "100%", 
                  padding: "0.5rem", 
                  borderRadius: "0.375rem", 
                  border: "1px solid #d1d5db" 
                }}
              />
              <p style={{ fontSize: "0.875rem", color: "#6b7280", marginTop: "0.25rem" }}>
                New quantity will be: {selectedPart.quantity + stockAdjustForm.quantity}
              </p>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: "500" }}>
                Reason for Adjustment:
              </label>
              <textarea
                value={stockAdjustForm.reason}
                onChange={(e) => setStockAdjustForm(prev => ({ ...prev, reason: e.target.value }))}
                rows={3}
                placeholder="e.g., Manual restock, Inventory correction, Damaged goods removal"
                style={{ 
                  width: "100%", 
                  padding: "0.5rem", 
                  borderRadius: "0.375rem", 
                  border: "1px solid #d1d5db" 
                }}
              />
            </div>
          </div>
          <div className="modal-footer-custom">
            <Button variant="secondary" onClick={() => setShowStockAdjustModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={handleSubmitStockAdjustment}>
              Apply Adjustment
            </Button>
          </div>
        </div>
      </div>
    );
  };

  // Render Info Box
  const renderInfoBox = () => (
    <div className="dashboard-info-box">
      <h3 style={{ fontWeight: 'bold', fontSize: '1rem', marginBottom: '1rem', color: '#9a3412' }}>
        Parts Supply Warehouse Operations
      </h3>
      <ul style={{ marginLeft: '1.5rem', fontSize: '0.875rem', lineHeight: '1.75', color: '#9a3412' }}>
        <li><strong>Supply Orders:</strong> Fulfill parts requests from Production & Assembly Control</li>
        <li><strong>Production Supply:</strong> Parts for manufacturing workstations (WS-1, WS-2, WS-3)</li>
        <li><strong>Assembly Supply:</strong> Parts for assembly workstations (WS-4, WS-5, WS-6)</li>
        <li><strong>Stock Management:</strong> Monitor parts inventory and adjust stock levels</li>
        <li><strong>Low Stock Alerts:</strong> Critical (&lt;20), Low (&lt;50), OK (≥50)</li>
        <li>Auto-refresh every 30 seconds for real-time updates</li>
      </ul>
    </div>
  );

  return (
    <>
      <DashboardLayout
        title="Parts Supply Warehouse"
        subtitle={`Manage parts supply orders and inventory${session?.user?.workstation ? ` | ${session.user.workstation.name}` : ''}`}
        icon="📦"
        layout="default"
        statsCards={renderStatsCards()}
        primaryContent={renderPartsStockSection()}
        ordersSection={renderSupplyOrdersSection()}
        infoBox={renderInfoBox()}
        messages={{ error, success }}
        onDismissError={() => setError(null)}
        onDismissSuccess={() => setSuccess(null)}
      />
      {renderDetailsModal()}
      {renderStockAdjustModal()}
      <style jsx>{`
        .modal-overlay-custom {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }
        .modal-content-custom {
          background: white;
          border-radius: 0.5rem;
          width: 90%;
          max-width: 600px;
          max-height: 90vh;
          overflow-y: auto;
          box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
        }
        .modal-header-custom {
          padding: 1.5rem;
          border-bottom: 1px solid #e5e7eb;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .modal-header-custom h2 {
          margin: 0;
          font-size: 1.25rem;
          font-weight: 600;
        }
        .modal-close-custom {
          background: none;
          border: none;
          font-size: 1.5rem;
          cursor: pointer;
          color: #6b7280;
        }
        .modal-body-custom {
          padding: 1.5rem;
        }
        .modal-footer-custom {
          padding: 1rem 1.5rem;
          border-top: 1px solid #e5e7eb;
          display: flex;
          justify-content: flex-end;
          gap: 0.5rem;
        }
      `}</style>
    </>
  );
}

export default PartsSupplyWarehouseDashboard;
