import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { useDashboardRefresh } from "../../context/DashboardRefreshContext";
import axios from "axios";
import { DashboardLayout, StatsCard, Notification } from "../../components";
import CustomerOrderCard from "../../components/CustomerOrderCard";
import { getProductDisplayName, getInventoryStatusColor } from "../../utils/dashboardHelpers";

function PlantWarehouseDashboard() {
  const { session } = useAuth();
  const [products, setProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState({});
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [inventory, setInventory] = useState([]);
  const [workstations, setWorkstations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fulfillingOrderId, setFulfillingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [notifications, setNotifications] = useState([]);

  // Add notification to state
  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: session?.user?.workstation?.name || 'PLANT-WH'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  useEffect(() => {
    fetchProducts();
    fetchWorkstations();
    if (session?.user?.workstationId) {
      fetchOrders();
      fetchInventory();
    }

    const inventoryInterval = setInterval(() => {
      if (session?.user?.workstationId) {
        fetchInventory();
      }
    }, 30000); // Increased to 30s to reduce page jump

    return () => clearInterval(inventoryInterval);
  }, [session?.user?.workstationId]);

  useEffect(() => {
    applyFilter(orders, filterStatus);
  }, [filterStatus, orders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const fetchProducts = async () => {
    try {
      const response = await axios.get("/api/masterdata/product-variants");
      setProducts(response.data);
    } catch (err) {
      setError("Failed to load products: " + (err.response?.data?.message || err.message));
    }
  };

  const fetchWorkstations = async () => {
    try {
      const response = await axios.get("/api/masterdata/workstations");
      setWorkstations(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to load workstations:", err);
    }
  };

  const fetchOrders = async () => {
    if (!session?.user?.workstationId) {
      setOrders([]);
      setFilteredOrders([]);
      return;
    }
    try {
      const response = await axios.get("/api/customer-orders/workstation/" + session.user.workstationId);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setOrders(ordersList);
      applyFilter(ordersList, filterStatus);
    } catch (err) {
      if (err.response?.status !== 404) {
        setError("Failed to load orders: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const fetchInventory = async () => {
    if (!session?.user?.workstationId) return;
    try {
      const response = await axios.get(`/api/stock/workstation/${session.user.workstationId}`);
      setInventory(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to fetch inventory:", err);
      setInventory([]);
    }
  };

  const handleQuantityChange = (productId, quantity) => {
    setSelectedProducts({
      ...selectedProducts,
      [productId]: Number.parseInt(quantity) || 0,
    });
  };

  const { refresh } = useDashboardRefresh();
  const handleCreateOrder = async () => {
    if (!session?.user?.workstationId) {
      setError("Cannot create order: workstation ID not found in session");
      return;
    }

    const orderItems = Object.entries(selectedProducts)
      .filter(([_, quantity]) => quantity > 0)
      .map(([productId, quantity]) => ({
        itemType: "PRODUCT",
        itemId: Number.parseInt(productId),
        quantity,
        notes: "",
      }));

    if (orderItems.length === 0) {
      setError("Please select at least one product with quantity > 0");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.post("/api/customer-orders", {
        orderItems,
        workstationId: session.user.workstationId,
        notes: "Plant warehouse order",
      });

      const orderNum = response.data.orderNumber;
      addNotification(`Order created: ${orderNum}`, 'success');
      setSelectedProducts({});
      fetchOrders();
      refresh(); // Trigger global dashboard refresh
    } catch (err) {
      setError("Failed to create order: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleFulfillOrder = async (orderId) => {
    setFulfillingOrderId(orderId);
    setError(null);

    try {
      const response = await axios.put(`/api/customer-orders/${orderId}/fulfill`);
      // Backend auto-determines scenario: Scenario 1 (COMPLETED) or Scenario 2/3 (PROCESSING)
      const isCompleted = response.data.status === 'COMPLETED';
      addNotification(
        isCompleted 
          ? `Order ${response.data.orderNumber} fulfilled` 
          : `Order ${response.data.orderNumber} processing`, 
        'success'
      );
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to fulfill order", 'error');
    } finally {
      setFulfillingOrderId(null);
    }
  };

  const handleConfirm = async (orderId) => {
    setError(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/confirm`);
      fetchOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
    }
  };

  // Process button calls same fulfill endpoint - backend handles scenario routing
  const handleProcessing = async (orderId) => {
    return handleFulfillOrder(orderId);
  };

  const handleComplete = async (orderId) => {
    setError(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/complete`);
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to complete order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this order?")) return;
    setError(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/cancel`);
      fetchOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
    }
  };

  // Render Stats Cards
  const renderStatsCards = () => (
    <>
      <StatsCard 
        value={orders.length} 
        label="Total Orders" 
        variant="default"
      />
      <StatsCard 
        value={orders.filter(o => o.status === "PENDING").length} 
        label="Pending" 
        variant="pending"
      />
      <StatsCard 
        value={orders.filter(o => o.status === "CONFIRMED" || o.status === "PROCESSING").length} 
        label="In Progress" 
        variant="processing"
      />
      <StatsCard 
        value={orders.filter(o => o.status === "COMPLETED" || o.status === "DELIVERED").length} 
        label="Completed" 
        variant="completed"
      />
    </>
  );

  // Render Create Order Form (Primary Content)
  const renderCreateOrderForm = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-blue">
        <h2 className="dashboard-box-header-title">➕ Create Customer Order</h2>
      </div>
      <div style={{ overflowX: 'auto', maxHeight: '400px' }}>
        <table className="dashboard-table">
          <thead>
            <tr>
              <th>Product Variant</th>
              <th>Price</th>
              <th>Est. Time</th>
              <th>In Stock</th>
              <th>Order QTY</th>
            </tr>
          </thead>
          <tbody>
            {products.length > 0 ? (
              products.map((product) => {
                const inventoryItem = inventory.find(item => item.itemId === product.id);
                const stockQuantity = inventoryItem?.quantity || 0;
                const statusColor = getInventoryStatusColor(stockQuantity);
                
                return (
                  <tr key={product.id}>
                    <td>{product.name || "Unknown"}</td>
                    <td>${(product.price || 0).toFixed(2)}</td>
                    <td>{product.estimatedTimeMinutes || 0} min</td>
                    <td style={{ color: statusColor, fontWeight: 'bold' }}>
                      {stockQuantity > 0 ? stockQuantity : (
                        <span style={{ color: '#dc2626' }}>Out of Stock</span>
                      )}
                    </td>
                    <td>
                      <input
                        type="number"
                        min="0"
                        value={selectedProducts[product.id] || 0}
                        onChange={(e) => handleQuantityChange(product.id, e.target.value)}
                        style={{
                          padding: '0.5rem 0.75rem',
                          border: '1px solid #d1d5db',
                          borderRadius: '0.375rem',
                          textAlign: 'center',
                          width: '5rem',
                          fontSize: '0.875rem'
                        }}
                      />
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="5" style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>
                  No products available
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="dashboard-box-footer">
        <button onClick={handleCreateOrder} disabled={loading} className="primary-link">
          {loading ? "Creating Order..." : "Create Order"}
        </button>
      </div>
    </>
  );

  // Render Inventory Display (Secondary Content)
  const renderInventoryDisplay = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-green">
        <h2 className="dashboard-box-header-title">📦 Current Inventory</h2>
      </div>
      <div style={{ overflowX: 'auto', maxHeight: '400px' }}>
        <table className="dashboard-table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Quantity</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {inventory.length > 0 ? (
              inventory.filter(item => item.itemType === "PRODUCT_VARIANT").map((item) => {
                const product = products.find(p => p.id === item.itemId);
                const statusColor = getInventoryStatusColor(item.quantity || 0);
                
                return (
                  <tr key={item.id}>
                    <td>{product?.name || item.itemName || `Product #${item.itemId}`}</td>
                    <td style={{ fontWeight: 'bold' }}>{item.quantity || 0}</td>
                    <td style={{ color: statusColor, fontWeight: 'bold' }}>
                      {item.quantity > 10 ? 'In Stock' : item.quantity > 0 ? 'Low Stock' : 'Out of Stock'}
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="3" style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>
                  No inventory data
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );

  // Render Orders Section
  const renderOrdersSection = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-blue">
        <h2 className="dashboard-box-header-title">📋 Recent Orders</h2>
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
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PROCESSING">Processing</option>
            <option value="COMPLETED">Completed</option>
            <option value="DELIVERED">Delivered</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>
      </div>
      <div className="dashboard-box-content">
        {Array.isArray(filteredOrders) && filteredOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <CustomerOrderCard
                key={order.id}
                order={order}
                inventory={inventory}
                onConfirm={handleConfirm}
                onFulfill={handleFulfillOrder}
                onProcess={handleProcessing}
                onComplete={handleComplete}
                onCancel={handleCancel}
                isProcessing={fulfillingOrderId === order.id}
                getProductDisplayName={getProductDisplayName}
                getInventoryStatusColor={getInventoryStatusColor}
              />
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No orders found</p>
            <p className="dashboard-empty-state-text">
              {filterStatus !== "ALL" 
                ? `No orders with status: ${filterStatus}` 
                : "Orders will appear here when created"}
            </p>
          </div>
        )}
      </div>
    </>
  );

  return (
    <DashboardLayout
      title="Plant Warehouse Dashboard"
      subtitle={`Manage inventory and customer orders${session?.user?.workstationId ? ` | Workstation ID: ${session.user.workstationId}` : ''}`}
      icon="🏢"
      layout="compact"
      statsCards={renderStatsCards()}
      primaryContent={renderCreateOrderForm()}
      notifications={
        <Notification 
          notifications={notifications}
          title="Warehouse Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={renderOrdersSection()}
    />
  );
}

export default PlantWarehouseDashboard;

