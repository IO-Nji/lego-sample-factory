import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { useDashboardRefresh } from "../../context/DashboardRefreshContext";
import api from "../../api/api";
import { 
  StandardDashboardLayout, 
  StatisticsGrid, 
  OrdersSection,
  InventoryTable,
  FormCard,
  ActivityLog,
  Card
} from "../../components";
import CustomerOrderCard from "../../components/CustomerOrderCard";
import { getProductDisplayName, getInventoryStatusColor } from "../../utils/dashboardHelpers";
import { useInventoryDisplay } from "../../hooks/useInventoryDisplay";
import { useActivityLog } from "../../hooks/useActivityLog";
import styles from "../../components/StandardDashboardLayout.module.css";

function PlantWarehouseDashboard() {
  const { session } = useAuth();
  const [selectedProducts, setSelectedProducts] = useState({});
  const [orders, setOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [sortBy, setSortBy] = useState("orderDate");
  const [orderCanComplete, setOrderCanComplete] = useState({}); // Track which PROCESSING orders can be completed
  
  // Use centralized inventory hook for products management
  const { 
    inventory, 
    masterdata: products,
    getItemName: getProductName,
    fetchInventory,
    error: inventoryError
  } = useInventoryDisplay('PRODUCT', 7);
  
  // Use enhanced activity log hook with auto-login tracking
  const { notifications, addNotification, clearNotifications } = useActivityLog(session, 'PLANT-WH');
  
  const [workstations, setWorkstations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fulfillingOrderId, setFulfillingOrderId] = useState(null);
  const [error, setError] = useState(null);
  
  // Log and notify when products fail to load
  useEffect(() => {
    if (inventoryError) {
      console.error('[PlantWarehouse] Product loading error:', inventoryError);
      addNotification(`⚠️ Failed to load products: ${inventoryError}`, 'error');
    }
    if (products.length === 0 && !inventoryError) {
      console.warn('[PlantWarehouse] No products available - database may not be seeded');
    }
    if (products.length > 0) {
      console.log(`[PlantWarehouse] Successfully loaded ${products.length} products`);
    }
  }, [inventoryError, products, addNotification]);

  // Check if PROCESSING orders can be completed (all FA orders submitted)
  const checkProcessingOrdersCompletion = async (ordersList) => {
    const processingOrders = ordersList.filter(o => o.status === 'PROCESSING');
    const canCompleteMap = {};
    
    for (const order of processingOrders) {
      try {
        const response = await api.get(`/customer-orders/${order.id}/can-complete`);
        canCompleteMap[order.id] = response.data === true;
      } catch (err) {
        console.error(`Failed to check completion status for order ${order.id}:`, err);
        canCompleteMap[order.id] = false;
      }
    }
    
    setOrderCanComplete(canCompleteMap);
  };

  useEffect(() => {
    const workstationId = session?.user?.workstationId;
    
    fetchWorkstations();
    if (workstationId) {
      fetchOrders();
      // fetchInventory is handled by useInventoryDisplay hook
    } else {
      console.warn('[PlantWarehouse] No workstationId in session, skipping orders/inventory fetch');
    }

    const inventoryInterval = setInterval(() => {
      const wsId = session?.user?.workstationId;
      if (wsId) {
        fetchInventory(); // Provided by useInventoryDisplay hook
      }
    }, 30000); // Increased to 30s to reduce page jump

    return () => clearInterval(inventoryInterval);
  }, [session?.user?.workstationId, fetchInventory]);

  const applyFilter = (ordersList, status) => {
    // Kept for backward compatibility - OrdersSection handles filtering now
    return ordersList;
  };

  // fetchProducts is now handled by useInventoryDisplay hook (products = masterdata)

  const fetchWorkstations = async () => {
    try {
      const response = await api.get("/masterdata/workstations");
      setWorkstations(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to load workstations:", err);
    }
  };

  const fetchOrders = async () => {
    const workstationId = session?.user?.workstationId || 7;
    if (!workstationId) {
      setOrders([]);
      return;
    }
    try {
      const response = await api.get("/customer-orders/workstation/" + workstationId);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      
      // Dynamically check current trigger scenario for CONFIRMED orders
      const ordersWithCurrentScenario = await Promise.all(
        ordersList.map(async (order) => {
          if (order.status === 'CONFIRMED') {
            try {
              const scenarioResponse = await api.get(`/customer-orders/${order.id}/current-scenario`);
              return {
                ...order,
                triggerScenario: scenarioResponse.data.triggerScenario
              };
            } catch (err) {
              console.error(`Failed to fetch current scenario for order ${order.id}:`, err);
              return order; // Keep original if check fails
            }
          }
          return order;
        })
      );
      
      setOrders(ordersWithCurrentScenario);
      applyFilter(ordersWithCurrentScenario, filterStatus);
      
      // Check completion status for PROCESSING orders
      await checkProcessingOrdersCompletion(ordersWithCurrentScenario);
    } catch (err) {
      if (err.response?.status !== 404) {
        setError("Failed to load orders: " + (err.response?.data?.message || err.message));
      }
    }
  };

  // fetchInventory is provided by useInventoryDisplay hook

  const handleQuantityChange = (productId, quantity) => {
    setSelectedProducts({
      ...selectedProducts,
      [productId]: Number.parseInt(quantity) || 0,
    });
  };

  const { refresh } = useDashboardRefresh();
  const handleCreateOrder = async () => {
    const workstationId = session?.user?.workstationId || 7;
    if (!workstationId) {
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
      const response = await api.post("/customer-orders", {
        orderItems,
        workstationId: workstationId,
        notes: "Plant warehouse order",
      });

      const orderNum = response.data.orderNumber;
      addNotification(`Created ${orderNum}`, 'success', { orderNumber: orderNum });
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
      const response = await api.put(`/customer-orders/${orderId}/fulfill`);
      // Backend auto-determines scenario: Scenario 1 (COMPLETED) or Scenario 2/3 (PROCESSING)
      const isCompleted = response.data.status === 'COMPLETED';
      const orderNum = response.data.orderNumber;
      addNotification(
        isCompleted 
          ? `${orderNum} fulfilled` 
          : `${orderNum} processing`, 
        'success',
        { orderNumber: orderNum }
      );
      await fetchOrders();
      await fetchInventory();
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
      await api.put(`/customer-orders/${orderId}/confirm`);
      fetchOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
    }
  };

  // Process button calls same fulfill endpoint - backend handles scenario routing
  const handleProcessing = async (orderId) => {
    return handleFulfillOrder(orderId);
  };

  // Scenario 4: Order Production Directly (large orders bypass warehouse)
  const handleOrderProduction = async (orderId) => {
    setError(null);
    try {
      // Create production order directly from customer order
      await api.post('/production-orders/from-customer-order', {
        customerOrderId: orderId,
        priority: 'NORMAL',
        notes: 'Scenario 4: Direct production (large order bypasses warehouse)',
        createdByWorkstationId: session?.user?.workstationId || 7
      });
      
      const order = orders.find(o => o.id === orderId);
      const orderNum = order?.orderNumber || `Order #${orderId}`;
      addNotification(`${orderNum} sent to production`, 'success', { orderNumber: orderNum });
      
      await fetchOrders();
    } catch (err) {
      setError("Failed to create production order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to create production order", 'error');
    }
  };

  const handleComplete = async (orderId) => {
    setError(null);
    try {
      await api.post(`/customer-orders/${orderId}/complete`);
      addNotification("Order completed successfully", 'success');
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to complete order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to complete order", 'error');
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this order?")) return;
    setError(null);
    try {
      await api.post(`/customer-orders/${orderId}/cancel`);
      addNotification("Order cancelled successfully", 'warning');
      fetchOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to cancel order", 'error');
    }
  };

  // Stats data for StatisticsGrid
  const statsData = [
    { value: orders.length, label: 'Total Orders', variant: 'default', icon: '📦' },
    { value: orders.filter(o => o.status === "PENDING").length, label: 'Pending', variant: 'pending', icon: '⏳' },
    { value: orders.filter(o => o.status === "PROCESSING").length, label: 'Processing', variant: 'processing', icon: '⚙️' },
    { value: orders.filter(o => o.status === "COMPLETED").length, label: 'Completed', variant: 'success', icon: '✅' },
    { value: inventory.filter(item => item.itemType === "PRODUCT").length, label: 'Product Types', variant: 'info', icon: '🏷️' },
    { value: inventory.filter(item => item.itemType === "PRODUCT").reduce((sum, item) => sum + (item.quantity || 0), 0), label: 'Total Stock', variant: 'default', icon: '📊' },
    { value: inventory.filter(item => item.quantity < 5).length, label: 'Low Stock', variant: 'warning', icon: '⚠️' },
    { value: inventory.filter(item => item.quantity === 0).length, label: 'Out of Stock', variant: 'danger', icon: '🚫' },
  ];

  // Render Activity/Notifications using ActivityLog component
  const renderActivity = () => (
    <Card variant="framed" title="WAREHOUSE ACTIVITY" style={{ height: '100%' }}>
      <ActivityLog
        notifications={notifications}
        onClear={clearNotifications}
        showTitle={false}
      />
    </Card>
  );

  // Render Statistics Grid for new layout
  const renderStats = () => (
    <StatisticsGrid stats={statsData} />
  );

  // Render Create Order Form using FormCard component
  const renderFormCompact = () => {
    // Determine appropriate empty message based on loading state
    let emptyMessage = "No products";
    if (inventoryError) {
      emptyMessage = "⚠️ Failed to load products - Check backend connection";
    } else if (products.length === 0) {
      emptyMessage = "⏳ Loading products... (or database not seeded)";
    }
    
    return (
      <FormCard
        title="New Order"
        icon="➕"
        items={products}
        selectedItems={selectedProducts}
        onItemChange={(itemId, quantity) => {
          setSelectedProducts({
            ...selectedProducts,
            [itemId]: quantity
          });
        }}
        onSubmit={handleCreateOrder}
        loading={loading}
        buttonText={loading ? "Creating..." : "Create Order"}
        getItemDisplayName={(item) => item.name}
        emptyMessage={emptyMessage}
        columnHeaders={{ item: 'Product', quantity: 'Qty' }}
      />
    );
  };

  // Render Inventory Table using InventoryTable component
  const renderInventory = () => (
    <InventoryTable
      title="Product Inventory"
      icon="📦"
      inventory={inventory}
      getItemName={getProductName}
      getStatusColor={getInventoryStatusColor}
      emptyMessage="No inventory items"
      columnHeaders={{ item: 'Product', quantity: 'Qty' }}
    />
  );

  // Render Orders Grid using OrdersSection component
  const renderOrdersGrid = () => (
    <OrdersSection
      title="Customer Orders"
      icon="📋"
      orders={orders}
      filterOptions={[
        { value: 'ALL', label: 'All Orders' },
        { value: 'PENDING', label: 'Pending' },
        { value: 'CONFIRMED', label: 'Confirmed' },
        { value: 'PROCESSING', label: 'Processing' },
        { value: 'COMPLETED', label: 'Completed' },
        { value: 'DELIVERED', label: 'Delivered' },
        { value: 'CANCELLED', label: 'Cancelled' }
      ]}
      sortOptions={[
        { value: 'orderDate', label: 'Order Date' },
        { value: 'orderNumber', label: 'Order Number' },
        { value: 'status', label: 'Status' }
      ]}
      renderCard={(order) => (
        <CustomerOrderCard
          key={order.id}
          order={order}
          inventory={inventory}
          onConfirm={handleConfirm}
          onFulfill={handleFulfillOrder}
          onProcess={handleProcessing}
          onOrderProduction={handleOrderProduction}
          onComplete={handleComplete}
          onCancel={handleCancel}
          isProcessing={fulfillingOrderId === order.id}
          canComplete={orderCanComplete[order.id] === true}
          getProductDisplayName={getProductDisplayName}
          getInventoryStatusColor={getInventoryStatusColor}
        />
      )}
      searchPlaceholder="CUST-XXX"
      emptyMessage="No customer orders found"
    />
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
                        aria-label={`Order quantity for ${product.name}`}
                        title={`Order quantity for ${product.name}`}
                        placeholder="0"
                        autoComplete="off"
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

  return (
    <StandardDashboardLayout
      title="Plant Warehouse"
      subtitle={`Customer Order Management${session?.user?.workstationId ? ` | Workstation ${session.user.workstationId}` : ''}`}
      icon="🏢"
      activityContent={renderActivity()}
      statsContent={renderStats()}
      formContent={renderFormCompact()}
      contentGrid={renderOrdersGrid()}
      inventoryContent={renderInventory()}
    />
  );
}

export default PlantWarehouseDashboard;

