import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  InventoryTable,
  StatisticsGrid, 
  Button, 
  Card, 
  Badge 
} from "../../components";
import { useInventoryDisplay } from "../../hooks/useInventoryDisplay";
import "../../styles/DashboardLayout.css";

function PartsSupplyWarehouseDashboard() {
  const { session } = useAuth();
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  
  // Use centralized inventory hook for parts management
  const { 
    inventory: partsStock, 
    getItemName,
    fetchInventory: fetchPartsStock 
  } = useInventoryDisplay('PART', 9);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterType, setFilterType] = useState("ALL"); // PRODUCTION, ASSEMBLY
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
      station: 'PARTS-WH'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };
  
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

  useEffect(() => {
    fetchSupplyOrders();
    const interval = setInterval(() => {
      fetchSupplyOrders();
      fetchPartsStock();
      // Parts masterdata doesn't change frequently, no need to refetch
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
      addNotification(`Supply order #${orderId} fulfilled successfully`, 'success');
      setSuccess(`Supply order #${orderId} fulfilled successfully`);
      fetchSupplyOrders();
      fetchPartsStock(); // Refresh stock after fulfillment
    } catch (err) {
      addNotification('Failed to fulfill supply order', 'error');
      setError("Failed to fulfill supply order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleRejectOrder = async (orderId) => {
    const reason = prompt("Enter rejection reason:");
    if (!reason) return;
    
    try {
      await api.put(`/supply-orders/${orderId}/reject`, { reason });
      addNotification(`Supply order #${orderId} rejected`, 'warning');
      setSuccess(`Supply order #${orderId} rejected`);
      fetchSupplyOrders();
    } catch (err) {
      addNotification('Failed to reject supply order', 'error');
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
  // Stats data for StatisticsGrid
  const statsData = (() => {
    const totalOrders = supplyOrders.length;
    const pendingOrders = supplyOrders.filter(o => o.status === "PENDING").length;
    const inProgressOrders = supplyOrders.filter(o => o.status === "IN_PROGRESS").length;
    const fulfilledOrders = supplyOrders.filter(o => o.status === "FULFILLED").length;
    const rejectedOrders = supplyOrders.filter(o => o.status === "REJECTED").length;
    const lowStockParts = partsStock.filter(p => p.quantity < 50).length;
    const totalParts = partsStock.reduce((sum, p) => sum + p.quantity, 0);

    return [
      { value: totalOrders, label: 'Total Orders', variant: 'default', icon: '📦' },
      { value: pendingOrders, label: 'Pending', variant: 'pending', icon: '⏳' },
      { value: inProgressOrders, label: 'In Progress', variant: 'info', icon: '🔄' },
      { value: fulfilledOrders, label: 'Fulfilled', variant: 'success', icon: '✅' },
      { value: rejectedOrders, label: 'Rejected', variant: 'danger', icon: '❌' },
      { value: lowStockParts, label: 'Low Stock Parts', variant: 'warning', icon: '⚠️' },
      { value: partsStock.length, label: 'Part Types', variant: 'info', icon: '🔧' },
      { value: totalParts, label: 'Total Parts', variant: 'default', icon: '📊' },
    ];
  })();

  // Activity log rendering
  const renderActivity = () => (
    <ActivityLog 
      notifications={notifications}
      onClear={clearNotifications}
    />
  );

  // Render Supply Orders using OrdersSection
  const renderSupplyOrders = () => (
    <OrdersSection
      title="Supply Orders"
      icon="📦"
      orders={supplyOrders}
      filterOptions={[
        { value: 'ALL', label: 'All Status' },
        { value: 'PENDING', label: 'Pending' },
        { value: 'IN_PROGRESS', label: 'In Progress' },
        { value: 'FULFILLED', label: 'Fulfilled' },
        { value: 'REJECTED', label: 'Rejected' },
        { value: 'CANCELLED', label: 'Cancelled' }
      ]}
      sortOptions={[
        { value: 'orderDate', label: 'Order Date' },
        { value: 'orderNumber', label: 'Order Number' },
        { value: 'status', label: 'Status' }
      ]}
      searchKeys={['supplyOrderNumber', 'notes']}
      sortKey="supplyOrderNumber"
      renderCard={(order) => (
        <Card key={order.id} variant="default">
          <div style={{ padding: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "0.75rem" }}>
              <div>
                <h3 style={{ fontWeight: "600", fontSize: "1rem", marginBottom: "0.25rem" }}>
                  {order.supplyOrderNumber}
                </h3>
                <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", flexWrap: "wrap" }}>
                  <Badge variant={
                    order.status === "FULFILLED" ? "success" : 
                    order.status === "REJECTED" ? "danger" : 
                    order.status === "CANCELLED" ? "secondary" :
                    order.status === "IN_PROGRESS" ? "info" : "warning"
                  }>
                    {order.status}
                  </Badge>
                  <Badge variant={order.sourceControlOrderType === "PRODUCTION" ? "primary" : "info"}>
                    {order.sourceControlOrderType || 'SUPPLY'}
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
      )}
      emptyMessage="Orders will appear here when production/assembly control requests parts"
      searchPlaceholder="Search by order number or notes..."
    />
  );

  // Render Parts Stock using InventoryTable
  const renderPartsStock = () => (
    <InventoryTable
      inventory={partsStock}
      onAdjustStock={handleStockAdjustment}
      title="Parts Inventory"
      icon="🔧"
      showAdjustButton={true}
      getItemName={getItemName}
      itemIdKey="itemId"
      emptyMessage="No parts in inventory"
    />
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
      <StandardDashboardLayout
        title="Parts Supply Warehouse"
        subtitle={`Parts Supply & Inventory Management${session?.user?.workstation ? ` | ${session.user.workstation.name}` : ''}`}
        icon="📦"
        activityContent={renderActivity()}
        statsContent={<StatisticsGrid stats={statsData} />}
        formContent={renderPartsStock()}
        contentGrid={renderSupplyOrders()}
        inventoryContent={null}
        messages={{ error, success }}
        onDismissError={() => setError(null)}
        onDismissSuccess={() => setSuccess(null)}
      />
      {renderDetailsModal()}
      {renderStockAdjustModal()}
    </>
  );
}

export default PartsSupplyWarehouseDashboard;
