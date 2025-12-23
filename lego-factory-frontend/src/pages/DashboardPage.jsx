import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import axios from "axios";
import PageHeader from "../components/PageHeader";
import CustomerOrderCard from "../components/CustomerOrderCard";
import "../styles/StandardPage.css";
import "../styles/DashboardStandard.css";
import "../styles/ControlPages.css";
import "../styles/AdminDashboard.css";

import { useDashboardRefresh } from "../context/DashboardRefreshContext";
/**
 * Product name mapping with acronyms
 * Maps itemId/productId to display name
 * Must match IDs from masterdata-service DataInitializer
 */
const PRODUCT_NAMES = {
  1: { name: "Technic Truck Yellow" },
  2: { name: "Technic Truck Red" },
  3: { name: "Creator House" },
  4: { name: "Friends Cafe" },
};

/**
 * Generate acronym from product name
 * Takes first letter of each word (e.g., "Drill A" -> "DA", "Drill Variant B" -> "DVB")
 */
const generateAcronym = (productName) => {
  if (!productName) return "?";
  return productName
    .split(" ")
    .map((word) => word.charAt(0).toUpperCase())
    .join("");
};

/**
 * Get product display name with dynamically generated acronym
 */
const getProductDisplayName = (itemId, itemType) => {
  if (itemType && itemType !== "PRODUCT") {
    return generateAcronym(itemType);
  }
  const product = PRODUCT_NAMES[itemId];
  if (product) {
    return generateAcronym(product.name);
  }
  return `#${itemId}`;
};

/**
 * Get inventory status color based on quantity
 * Green: > 20, Yellow: 6-20, Red: 1-5, Dark Red: 0
 */
const getInventoryStatusColor = (quantity) => {
  if (quantity > 20) return "#10b981"; // Green - In stock
  if (quantity > 5) return "#f59e0b"; // Yellow - Medium stock
  if (quantity > 0) return "#ef4444"; // Red - Low stock
  return "#991b1b"; // Dark red - Out of stock
};

/**
 * DashboardPage - Role-aware dashboard that displays different content based on user role
 * Consolidates all workstation dashboards into a single page component
 */
function DashboardPage() {
  const { session, isAdmin } = useAuth();
  const [isLoading, setIsLoading] = useState(true);
  const { subscribe } = useDashboardRefresh();

  useEffect(() => {
    setIsLoading(false);
  }, []);

  const userRole = session?.user?.role;

  if (isLoading) {
    return (
      <section>
        <h2>Loading Dashboard...</h2>
        <p>Initializing your personalized dashboard...</p>
      </section>
    );
  }

  // Render Admin Dashboard
  if (isAdmin) {
    return <AdminDashboardContent />;
  }

  // Render Plant Warehouse Dashboard
  if (userRole === "PLANT_WAREHOUSE") {
    return <PlantWarehouseDashboardContent />;
  }

  // Render Modules Supermarket Dashboard
  if (userRole === "MODULES_SUPERMARKET") {
    return <ModulesSupermarketDashboardContent />;
  }

  // Render Production Planning Dashboard
  if (userRole === "PRODUCTION_PLANNING") {
    return <ProductionPlanningDashboardContent />;
  }

  // Render Production Control Dashboard
  if (userRole === "PRODUCTION_CONTROL") {
    return <ProductionControlDashboardContent />;
  }

  // Render Assembly Control Dashboard
  if (userRole === "ASSEMBLY_CONTROL") {
    return <AssemblyControlDashboardContent />;
  }

  // Render Manufacturing Workstation Dashboard
  if (userRole === "MANUFACTURING_WORKSTATION") {
    return <ManufacturingDashboardContent />;
  }

  // Render Assembly Workstation Dashboard
  if (userRole === "ASSEMBLY_WORKSTATION") {
    return <AssemblyWorkstationDashboardContent />;
  }

  // Render Parts Supply Warehouse Dashboard
  if (userRole === "PARTS_SUPPLY_WAREHOUSE") {
    return <PartsSupplyWarehouseDashboardContent />;
  }

  // Default fallback
  return (
    <section>
      <h2>Factory</h2>
      <p>Welcome to the LEGO Factory Control System</p>
      <p>Your role ({userRole || "Unknown"}) is currently being set up. Contact your administrator if you believe this is an error.</p>
    </section>
  );
}

// ============================================
// ADMIN DASHBOARD
// ============================================
function AdminDashboardContent() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dashboardData, setDashboardData] = useState({
    totalOrders: 0,
    pendingOrders: 0,
    completedOrders: 0,
    activeWorkstations: 0,
    lowStockItems: 0,
    productionOrders: [],
    orderStats: {},
  });
  const [lowAlerts, setLowAlerts] = useState([]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch from correct working endpoints
      const [wsResponse, prodResponse, asmResponse, supResponse, lowAlertsRes] = await Promise.all([
        axios.get("/api/masterdata/workstations"),
        axios.get("/api/production-control-orders"),
        axios.get("/api/assembly-control-orders"),
        axios.get("/api/supply-orders/warehouse"),
        axios.get("/api/stock/alerts/low"),
      ]).catch((err) => {
        setError("Dashboard API error: " + (err.message || "Unknown error"));
        console.error("Dashboard API error:", err);
        return [null, null, null, null, null];
      });

      // Log API responses for debugging
      console.log("Workstations:", wsResponse?.data);
      console.log("Production Orders:", prodResponse?.data);
      console.log("Assembly Orders:", asmResponse?.data);
      console.log("Supply Orders:", supResponse?.data);
      console.log("Low Stock Alerts:", lowAlertsRes?.data);

      const wsData = Array.isArray(wsResponse?.data) ? wsResponse.data : [];
      const prodData = Array.isArray(prodResponse?.data) ? prodResponse.data : [];
      const asmData = Array.isArray(asmResponse?.data) ? asmResponse.data : [];
      const supData = Array.isArray(supResponse?.data) ? supResponse.data : [];

      // Combine all orders
      const allOrders = [...prodData, ...asmData, ...supData];
      const pendingOrders = allOrders.filter((o) => o.status === "PENDING").length;
      const completedOrders = allOrders.filter((o) => o.status === "COMPLETED").length;
      const processingOrders = allOrders.filter((o) => o.status === "PROCESSING").length;

      setDashboardData({
        totalOrders: allOrders.length,
        pendingOrders: pendingOrders,
        completedOrders: completedOrders,
        activeWorkstations: wsData.length,
        lowStockItems: Array.isArray(lowAlertsRes?.data) ? lowAlertsRes.data.length : 0,
        productionOrders: prodData.slice(0, 5),
        orderStats: {
          pending: pendingOrders,
          processing: processingOrders,
          completed: completedOrders,
        },
      });
      setLowAlerts(Array.isArray(lowAlertsRes?.data) ? lowAlertsRes.data : []);
    } catch (err) {
      setError("Failed to load dashboard data: " + (err.message || "Unknown error"));
      console.error("Dashboard fetch error:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <section className="admin-dashboard">
        <h2>🏭 Admin</h2>
        <div className="loading-state">Loading dashboard data...</div>
      </section>
    );
  }

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Admin Overview"
        subtitle="Real-time monitoring and control of factory operations"
        icon="🏭"
        actions={[
          {
            label: loading ? "Refreshing..." : "Refresh",
            onClick: fetchDashboardData,
            disabled: loading,
            icon: "⟳",
          },
        ]}
      />
      <section className="admin-dashboard">

      {error && <div className="error-alert">{error}</div>}

      {/* KPI Cards */}
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-value">{dashboardData.totalOrders}</div>
          <div className="kpi-label">Total Orders</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-value" style={{ color: "#f59e0b" }}>{dashboardData.pendingOrders}</div>
          <div className="kpi-label">Pending Orders</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-value" style={{ color: "#10b981" }}>{dashboardData.completedOrders}</div>
          <div className="kpi-label">Completed Orders</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-value">{dashboardData.activeWorkstations}</div>
          <div className="kpi-label">Active Workstations</div>
        </div>
      </div>

      {/* Low Stock Alerts + Recent Production Orders */}
      <div className="dashboard-section" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        <div className="dashboard-box">
          <h3>Low Stock Alerts ({dashboardData.lowStockItems})</h3>
          <div className="dashboard-table">
            {lowAlerts.length > 0 ? (
              <table className="products-table">
                <thead>
                  <tr>
                    <th>Scope</th>
                    <th>Item</th>
                    <th>Qty</th>
                    <th>Threshold</th>
                    <th>Deficit</th>
                  </tr>
                </thead>
                <tbody>
                  {lowAlerts.slice(0, 8).map((a, idx) => (
                    <tr key={idx}>
                      <td>{a.workstationId ? `WS#${a.workstationId}` : "Global"}</td>
                      <td>{(a.itemType || "").toUpperCase()} #{a.itemId}</td>
                      <td>{a.quantity}</td>
                      <td>{a.threshold}</td>
                      <td style={{ color: "#b91c1c", fontWeight: 700 }}>{a.deficit}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No low stock alerts</p>
            )}
          </div>
        </div>

        <div className="dashboard-box">
          <h3>Recent Production Orders</h3>
          {dashboardData.productionOrders.length > 0 ? (
            <div className="dashboard-table">
              <table className="products-table">
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Items</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboardData.productionOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span className={`status-badge status-${order.status?.toLowerCase()}`}>
                          {order.status || "UNKNOWN"}
                        </span>
                      </td>
                      <td>{order.quantity || 0}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p>No production orders at this time</p>
          )}
        </div>
      </div>
    </section>
    </div>
  );
}

// ============================================
// PLANT WAREHOUSE DASHBOARD
// ============================================
function PlantWarehouseDashboardContent() {
  const { session } = useAuth();
  const [products, setProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState({});
  const [orders, setOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fulfillingOrderId, setFulfillingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    fetchProducts();
    if (session?.user?.workstationId) {
      fetchOrders();
      fetchInventory();
    }

    const inventoryInterval = setInterval(() => {
      if (session?.user?.workstationId) {
        fetchInventory();
      }
    }, 10000);

    return () => clearInterval(inventoryInterval);
  }, [session?.user?.workstationId]);

  const fetchProducts = async () => {
    try {
      const response = await axios.get("/api/masterdata/product-variants");
      setProducts(response.data);
    } catch (err) {
      setError("Failed to load products: " + (err.response?.data?.message || err.message));
    }
  };

  const fetchOrders = async () => {
    if (!session?.user?.workstationId) {
      setOrders([]);
      return;
    }
    try {
      const response = await axios.get("/api/customer-orders/workstation/" + session.user.workstationId);
      setOrders(Array.isArray(response.data) ? response.data : []);
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
    setSuccessMessage(null);

    try {
      const response = await axios.post("/api/customer-orders", {
        orderItems,
        workstationId: session.user.workstationId,
        notes: "Plant warehouse order",
      });

      setSuccessMessage(`Order created: ${response.data.orderNumber} - Click "Fulfill" to process`);
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
    setSuccessMessage(null);

    try {
      const response = await axios.put(`/api/customer-orders/${orderId}/fulfill`);
      setSuccessMessage(`Order fulfilled successfully! Status: ${response.data.status}`);
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
    } finally {
      setFulfillingOrderId(null);
    }
  };

  const handleConfirm = async (orderId) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/confirm`);
      setSuccessMessage("Order confirmed");
      fetchOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleProcessing = async (orderId) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/processing`);
      setSuccessMessage("Order moved to PROCESSING");
      fetchOrders();
    } catch (err) {
      setError("Failed to mark processing: " + (err.response?.data?.message || err.message));
    }
  };

  const handleComplete = async (orderId) => {
    setError(null);
    setSuccessMessage(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/complete`);
      setSuccessMessage("Order completed");
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to complete order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this order?")) return;
    setError(null);
    setSuccessMessage(null);
    try {
      await axios.put(`/api/customer-orders/${orderId}/cancel`);
      setSuccessMessage("Order cancelled");
      fetchOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Plant Warehouse"
        subtitle={`Manage inventory and customer orders${session?.user?.workstationId ? ` | Workstation ID: ${session.user.workstationId}` : ''}`}
        icon="🏢"
      />
      <section className="plant-warehouse-page">

      {error && (
        <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
          <p className="font-semibold text-red-900">Error</p>
          <p className="text-red-800 text-sm mt-1">{error}</p>
          <button onClick={() => setError(null)} className="text-red-600 hover:text-red-800 font-semibold text-sm mt-2">
            Dismiss
          </button>
        </div>
      )}

      {successMessage && (
        <div className="form-success-details mb-6">
          <p className="font-semibold text-green-900">Success</p>
          <p className="text-green-800 text-sm mt-1">{successMessage}</p>
          <button onClick={() => setSuccessMessage(null)} className="text-green-700 hover:text-green-900 font-semibold text-sm mt-2">
            Dismiss
          </button>
        </div>
      )}

      {!session?.user?.workstationId && (
        <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
          <p className="font-semibold text-red-900">⚠️ No workstation assigned</p>
          <p className="text-red-800 text-sm mt-1">Contact administrator to assign a workstation to your account.</p>
        </div>
      )}

      <div className="two-column-section">
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 create-order-box">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">➕ Create Customer Order</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="products-table w-full">
              <thead>
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                    Product Variant
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Price</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Est. Time</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">QTY</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {products.length > 0 ? (
                  products.map((product) => (
                    <tr key={product.id} className="hover:bg-gray-50">
                      <td className="px-6 py-2 whitespace-nowrap text-sm font-medium text-gray-900">{product.name || "Unknown"}</td>
                      <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">${(product.price || 0).toFixed(2)}</td>
                      <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">{product.estimatedTimeMinutes || 0} min</td>
                      <td className="px-6 py-1 whitespace-nowrap text-sm">
                        <input
                          type="number"
                          min="0"
                          value={selectedProducts[product.id] || 0}
                          onChange={(e) => handleQuantityChange(product.id, e.target.value)}
                          className="px-1 py-1 border border-gray-300 rounded text-center focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs"
                          style={{ width: "3rem" }}
                        />
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" className="px-6 py-4 text-center text-gray-500">
                      No products available
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
            <button onClick={handleCreateOrder} disabled={loading} className="primary-link">
              {loading ? "Creating Order..." : "Create Order"}
            </button>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 inventory-box">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">📦 Current Inventory</h2>
          </div>
          <div className="overflow-x-auto flex-grow">
            {inventory.length > 0 ? (
              <table className="products-table w-full">
                <thead>
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Product</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Item ID</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Qty</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {inventory.map((item, idx) => (
                    <tr key={idx} className="hover:bg-gray-50">
                      <td className="px-4 py-3 whitespace-nowrap text-xs font-medium text-gray-900">{PRODUCT_NAMES[item.itemId]?.name || `Product #${item.itemId}`}</td>
                      <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-600">#{item.itemId}</td>
                      <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-900 font-semibold">{item.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="p-4 text-center text-gray-500">
                <p className="text-sm">No inventory items</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
        <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
          <h2 className="text-lg font-semibold text-blue-900">📋 Recent Orders</h2>
        </div>
        <div className="p-6">
          {Array.isArray(orders) && orders.length > 0 ? (
            <div className="orders-grid">
              {orders.map((order) => (
                <CustomerOrderCard
                  key={order.id}
                  order={order}
                  inventory={inventory}
                  onConfirm={handleConfirm}
                  onFulfill={handleFulfillOrder}
                  onComplete={handleComplete}
                  onCancel={handleCancel}
                  isProcessing={fulfillingOrderId === order.id}
                  getProductDisplayName={getProductDisplayName}
                  getInventoryStatusColor={getInventoryStatusColor}
                />
              ))}
            </div>
          ) : (
            <div className="text-center text-gray-500">
              <p className="text-sm">No orders found for this workstation</p>
            </div>
          )}
        </div>
      </div>

      <style>{`
        .plant-warehouse-page {
          padding: 2rem 1rem;
          max-width: 1400px;
          margin: 0 auto;
        }
        .page-header {
          margin-bottom: 2rem;
        }
        .page-title {
          font-size: 2rem;
          font-weight: 700;
          color: #0b5394;
          margin: 0 0 0.5rem 0;
        }
        .page-subtitle {
          font-size: 1rem;
          color: #666;
          margin: 0;
        }
        .two-column-section {
          display: grid;
          grid-template-columns: 60% 40%;
          gap: 2rem;
          margin-bottom: 2rem;
        }
        .create-order-box {
          display: flex;
          flex-direction: column;
        }
        .create-order-box .overflow-x-auto {
          flex-grow: 1;
          overflow-y: auto;
          max-height: 400px;
        }
        .inventory-box {
          display: flex;
          flex-direction: column;
        }
        .inventory-box .overflow-x-auto {
          flex-grow: 1;
          overflow-y: auto;
          max-height: 400px;
        }
        .form-error {
          background: #fee;
          color: #c33;
          border-left: 4px solid #c33;
        }
        .form-success-details {
          background: #efe;
          color: #3c3;
          border-left: 4px solid #3c3;
          padding: 1rem;
          border-radius: 0.5rem;
          margin-bottom: 1.5rem;
        }
        .mb-6 {
          margin-bottom: 1.5rem;
        }
      `}</style>
      </section>
    </div>
  );
}

// ============================================
// MODULES SUPERMARKET DASHBOARD
// ============================================
function ModulesSupermarketDashboardContent() {
  const { session } = useAuth();
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [fulfillmentInProgress, setFulfillmentInProgress] = useState({});
  const [selectedModule, setSelectedModule] = useState(null);

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchWarehouseOrders();
      fetchInventory();
    }
    const interval = setInterval(() => {
      fetchWarehouseOrders();
      fetchInventory();
    }, 15000);
    return () => clearInterval(interval);
  }, [session?.user?.workstationId]);

  useEffect(() => {
    if (statusFilter === "ALL") {
      setFilteredOrders(warehouseOrders);
    } else {
      setFilteredOrders(warehouseOrders.filter((order) => order.status === statusFilter));
    }
  }, [warehouseOrders, statusFilter]);

  const fetchWarehouseOrders = async () => {
    try {
      const workstationId = session?.user?.workstationId || 8;
      const response = await axios.get(`/api/warehouse-orders/workstation/${workstationId}`);
      const data = response.data;
      setWarehouseOrders(Array.isArray(data) ? data : []);
    } catch (err) {
      setError("Failed to fetch warehouse orders: " + err.message);
    }
  };

  const fetchInventory = async () => {
    try {
      const workstationId = session?.user?.workstationId || 8;
      const response = await axios.get(`/api/stock/workstation/${workstationId}`);
      setInventory(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to fetch inventory:", err);
      setInventory([]);
    }
  };

  const fulfillWarehouseOrder = async (orderId) => {
    setFulfillmentInProgress({ ...fulfillmentInProgress, [orderId]: true });
    setError(null);
    setSuccessMessage(null);

    try {
      const response = await axios.put(`/api/warehouse-orders/${orderId}/fulfill-modules`);
      setSuccessMessage(`Order ${response.data.orderNumber} fulfilled successfully`);
      setTimeout(() => setSuccessMessage(null), 3000);
      fetchWarehouseOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
    } finally {
      setFulfillmentInProgress({ ...fulfillmentInProgress, [orderId]: false });
    }
  };

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Modules Supermarket Dashboard"
        subtitle="Manage modules and fulfill warehouse orders"
        icon="🏢"
      />
      <section className="modules-supermarket-page" style={{ padding: "2rem 1rem", maxWidth: "1400px", margin: "0 auto" }}>

      {error && <div className="error-alert">{error}</div>}
      {successMessage && <div className="form-success-details">{successMessage}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "2rem", marginBottom: "2rem" }}>
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">📋 Warehouse Orders</h2>
            <div style={{ marginTop: "1rem", display: "flex", gap: "0.5rem" }}>
              {["PENDING", "IN_PROGRESS", "COMPLETED", "ALL"].map((status) => (
                <button
                  key={status}
                  onClick={() => setStatusFilter(status)}
                  style={{
                    padding: "0.5rem 1rem",
                    background: statusFilter === status ? "#0b5394" : "#e0e0e0",
                    color: statusFilter === status ? "white" : "#333",
                    border: "none",
                    borderRadius: "0.375rem",
                    cursor: "pointer",
                    fontSize: "0.875rem",
                    fontWeight: "500",
                  }}
                >
                  {status}
                </button>
              ))}
            </div>
          </div>
          <div style={{ overflowX: "auto", maxHeight: "600px" }}>
            {filteredOrders.length > 0 ? (
              <table className="products-table" style={{ width: "100%" }}>
                <thead>
                  <tr>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Order #</th>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Items</th>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Status</th>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((order) => (
                    <tr key={order.id} style={{ borderBottom: "1px solid #e0e0e0" }}>
                      <td style={{ padding: "0.75rem", fontSize: "0.875rem" }}>#{order.orderNumber}</td>
                      <td style={{ padding: "0.75rem", fontSize: "0.875rem" }}>{order.quantity || 0}</td>
                      <td style={{ padding: "0.75rem" }}>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td style={{ padding: "0.75rem" }}>
                        {order.status === "PENDING" && (
                          <button
                            onClick={() => fulfillWarehouseOrder(order.id)}
                            disabled={fulfillmentInProgress[order.id]}
                            style={{
                              padding: "0.5rem 1rem",
                              background: "#27ae60",
                              color: "white",
                              border: "none",
                              borderRadius: "0.375rem",
                              cursor: fulfillmentInProgress[order.id] ? "not-allowed" : "pointer",
                              fontSize: "0.875rem",
                              fontWeight: "600",
                            }}
                          >
                            {fulfillmentInProgress[order.id] ? "Processing..." : "Fulfill"}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ padding: "2rem", textAlign: "center", color: "#999" }}>No orders found</div>
            )}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">📦 Current Inventory</h2>
          </div>
          <div style={{ overflowX: "auto", maxHeight: "600px" }}>
            {inventory.length > 0 ? (
              <table className="products-table" style={{ width: "100%" }}>
                <thead>
                  <tr>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Item Type</th>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Item ID</th>
                    <th style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>Quantity</th>
                  </tr>
                </thead>
                <tbody>
                  {inventory.map((item, idx) => (
                    <tr key={idx} style={{ borderBottom: "1px solid #e0e0e0" }}>
                      <td style={{ padding: "0.75rem", fontSize: "0.875rem" }}>{item.itemType || "MODULE"}</td>
                      <td style={{ padding: "0.75rem", fontSize: "0.875rem" }}>#{item.itemId}</td>
                      <td style={{ padding: "0.75rem", fontSize: "0.875rem", fontWeight: "600" }}>{item.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div style={{ padding: "2rem", textAlign: "center", color: "#999" }}>No inventory items</div>
            )}
          </div>
        </div>
      </div>
    </section>
    </div>
  );
}

// ============================================
// PRODUCTION PLANNING DASHBOARD
// ============================================
function ProductionPlanningDashboardContent() {
  const [productionOrders, setProductionOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchProductionOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get("/api/production-control-orders");
      setProductionOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError("Failed to load production orders: " + (err.message || "Unknown error"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProductionOrders();
    const interval = setInterval(fetchProductionOrders, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Production Planning Dashboard"
        subtitle="Manage production orders and scheduling"
        icon="📋"
        actions={[
          <button
            key="refresh"
            onClick={fetchProductionOrders}
            disabled={loading}
            className="standard-btn standard-btn-primary"
            style={{
              cursor: loading ? "not-allowed" : "pointer",
              opacity: loading ? 0.6 : 1,
            }}
          >
            {loading ? "⟳ Refreshing..." : "⟳ Refresh"}
          </button>
        ]}
      />

      {error && <div className="error-alert">{error}</div>}

      {loading ? (
        <div>Loading production orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {productionOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Items</th>
                    <th>Workstation</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {productionOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status || "UNKNOWN"}
                        </span>
                      </td>
                      <td>{order.quantity || 0}</td>
                      <td>{order.workstationName || "N/A"}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt || new Date()).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No production orders at this time</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

// ============================================
// PRODUCTION CONTROL DASHBOARD
// ============================================
function ProductionControlDashboardContent() {
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchControlOrders = async () => {
    const workstationId = session?.user?.workstationId;
    if (!workstationId) return;

    setLoading(true);
    setError(null);

    try {
      const response = await axios.get(`/api/production-control-orders/workstation/${workstationId}`);
      setControlOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError("Failed to load control orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchControlOrders();
      const interval = setInterval(fetchControlOrders, 5000);
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Production Control Dashboard"
        subtitle="Manage production control orders"
        icon="🏭"
        actions={[
          <button
            key="refresh"
            onClick={fetchControlOrders}
            disabled={loading}
            className="standard-btn standard-btn-primary"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        ]}
      />

      {error && <div className="error-alert">{error}</div>}

      {loading ? (
        <div>Loading control orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {controlOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Quantity</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {controlOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td>{order.quantity}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No production control orders assigned yet</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

// ============================================
// ASSEMBLY CONTROL DASHBOARD
// ============================================
function AssemblyControlDashboardContent() {
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchControlOrders = async () => {
    const workstationId = session?.user?.workstationId;
    if (!workstationId) return;

    setLoading(true);
    setError(null);

    try {
      const response = await axios.get(`/api/assembly-control-orders/workstation/${workstationId}`);
      setControlOrders(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      setError("Failed to load assembly orders: " + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchControlOrders();
      const interval = setInterval(fetchControlOrders, 10000);
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Assembly Control Dashboard"
        subtitle={`Workstation ${session?.user?.workstationId || 'N/A'} - Manage assembly orders`}
        icon="⚙️"
      />

      {error && <div className="error-alert">{error}</div>}

      {loading ? (
        <div>Loading assembly orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {controlOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Quantity</th>
                    <th>Priority</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {controlOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td>{order.quantity}</td>
                      <td>{order.priority || "NORMAL"}</td>
                      <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No assembly control orders assigned yet</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

// ============================================
// MANUFACTURING WORKSTATION DASHBOARD
// ============================================
function ManufacturingDashboardContent() {
  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Manufacturing Workstation Dashboard"
        subtitle="Configure your workstation-specific controls here"
        icon="🔧"
      />
      <p>Manufacturing workstation interface</p>
    </section>
    </div>
  );
}

// ============================================
// ASSEMBLY WORKSTATION DASHBOARD
// ============================================
function AssemblyWorkstationDashboardContent() {
  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Assembly Workstation Dashboard"
        subtitle="Configure your workstation-specific controls here"
        icon="🔩"
      />
      <p>Assembly workstation interface</p>
    </section>
    </div>
  );
}

// ============================================
// PARTS SUPPLY WAREHOUSE DASHBOARD
// ============================================
function PartsSupplyWarehouseDashboardContent() {
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("PENDING");

  const fetchSupplyOrders = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await axios.get("/api/supply-orders/warehouse", {
        params: { status: filter || undefined },
      });
      setSupplyOrders(response.data);
    } catch (err) {
      setError("Failed to fetch supply orders: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSupplyOrders();
    const interval = setInterval(fetchSupplyOrders, 10000);
    return () => clearInterval(interval);
  }, [filter]);

  const fulfillSupplyOrder = async (orderId) => {
    try {
      const response = await axios.put(`/api/supply-orders/${orderId}/fulfill`);
      fetchSupplyOrders();
    } catch (err) {
      setError("Failed to fulfill supply order: " + err.message);
    }
  };

  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Parts Supply Warehouse Dashboard"
        subtitle="Manage parts supply orders"
        icon="📦"
      />

      {error && <div className="error-alert">{error}</div>}

      <div style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}>
        {["PENDING", "IN_PROGRESS", "COMPLETED"].map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            style={{
              padding: "0.5rem 1rem",
              background: filter === status ? "#0b5394" : "#e0e0e0",
              color: filter === status ? "white" : "#333",
              border: "none",
              borderRadius: "0.375rem",
              cursor: "pointer",
              fontWeight: "600",
            }}
          >
            {status}
          </button>
        ))}
      </div>

      {loading ? (
        <div>Loading supply orders...</div>
      ) : (
        <div className="dashboard-box">
          <div className="dashboard-table">
            {supplyOrders.length > 0 ? (
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Status</th>
                    <th>Items Needed</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {supplyOrders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.supplyOrderNumber}</td>
                      <td>
                        <span style={{ padding: "0.25rem 0.75rem", background: "#cfe2ff", color: "#084298", borderRadius: "0.375rem", fontSize: "0.75rem", fontWeight: "700" }}>
                          {order.status}
                        </span>
                      </td>
                      <td>{order.quantity || 0}</td>
                      <td>
                        {order.status === "PENDING" && (
                          <button onClick={() => fulfillSupplyOrder(order.id)} style={{ padding: "0.5rem 1rem", background: "#27ae60", color: "white", border: "none", borderRadius: "0.375rem", cursor: "pointer", fontWeight: "600", fontSize: "0.875rem" }}>
                            Fulfill
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No supply orders found</p>
            )}
          </div>
        </div>
      )}
    </section>
    </div>
  );
}

export default DashboardPage;
