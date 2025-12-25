import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { useDashboardRefresh } from "../../context/DashboardRefreshContext";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import CustomerOrderCard from "../../components/CustomerOrderCard";
import AddNewUserForm from "../../components/AddNewUserForm";
import { getProductDisplayName, getInventoryStatusColor } from "../../utils/dashboardHelpers";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function PlantWarehouseDashboard() {
  const { session } = useAuth();
  const [products, setProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState({});
  const [orders, setOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [workstations, setWorkstations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fulfillingOrderId, setFulfillingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

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
      // Backend auto-determines scenario: Scenario 1 (COMPLETED) or Scenario 2/3 (PROCESSING)
      const isCompleted = response.data.status === 'COMPLETED';
      const message = isCompleted 
        ? `Order ${response.data.orderNumber} fulfilled successfully!`
        : `Order ${response.data.orderNumber} processing - Warehouse order created`;
      setSuccessMessage(message);
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
      setSuccessMessage("Order confirmed - Check stock and click Fulfill/Process");
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

      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
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
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">In Stock</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Order QTY</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {products.length > 0 ? (
                products.map((product) => {
                  const inventoryItem = inventory.find(item => item.itemId === product.id);
                  const stockQuantity = inventoryItem?.quantity || 0;
                  const statusColor = getInventoryStatusColor(stockQuantity);
                  
                  return (
                    <tr key={product.id} className="hover:bg-gray-50">
                      <td className="px-6 py-2 whitespace-nowrap text-sm font-medium text-gray-900">{product.name || "Unknown"}</td>
                      <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">${(product.price || 0).toFixed(2)}</td>
                      <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">{product.estimatedTimeMinutes || 0} min</td>
                      <td className="px-6 py-2 whitespace-nowrap text-sm font-semibold" style={{ color: statusColor }}>
                        {stockQuantity > 0 ? stockQuantity : (
                          <span className="text-red-600">Out of Stock</span>
                        )}
                      </td>
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
                  );
                })
              ) : (
                <tr>
                  <td colSpan="5" className="px-6 py-4 text-center text-gray-500">
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
            <div className="text-center text-gray-500">
              <p className="text-sm">No orders found for this workstation</p>
            </div>
          )}
        </div>
      </div>

      {session?.user?.role === 'ADMIN' && (
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">👤 User Management</h2>
          </div>
          <div className="p-6 flex justify-center">
            <AddNewUserForm 
              workstations={workstations}
              onSuccess={(user) => {
                setSuccessMessage(`User "${user.username}" created successfully!`);
                setTimeout(() => setSuccessMessage(null), 5000);
              }}
              onError={(error) => {
                setError(error);
                setTimeout(() => setError(null), 5000);
              }}
            />
          </div>
        </div>
      )}

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

export default PlantWarehouseDashboard;
