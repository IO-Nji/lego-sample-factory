import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import api from "../api/api";
import "../styles/DashboardStandard.css";

/**
 * Product name mapping
 * Maps itemId to product variant name
 */
const PRODUCT_NAMES = {
  1: { name: "Technic Truck Yellow" },
  2: { name: "Technic Truck Red" },
  3: { name: "Creator House" },
  4: { name: "Friends Cafe" },
};

function PlantWarehousePage() {
  const navigate = useNavigate();
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
    
    // Auto-refresh inventory every 10 seconds for live updates
    const inventoryInterval = setInterval(() => {
      if (session?.user?.workstationId) {
        fetchInventory();
      }
    }, 10000);
    
    return () => clearInterval(inventoryInterval);
  }, [session?.user?.workstationId]);

  const fetchProducts = async () => {
    try {
      const response = await api.get("/masterdata/product-variants");
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
      const response = await api.get("/customer-orders/workstation/" + session.user.workstationId);
      if (Array.isArray(response.data)) {
        setOrders(response.data);
      } else {
        setOrders([]);
        setError("Unexpected response format from server.");
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setOrders([]);
      } else {
        setError("Failed to load orders: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const fetchInventory = async () => {
    if (!session?.user?.workstationId) return;
    try {
      const response = await api.get(`/stock/workstation/${session.user.workstationId}`);
      if (Array.isArray(response.data)) {
        setInventory(response.data);
      } else {
        setInventory([]);
      }
    } catch (err) {
      console.error("Failed to fetch inventory:", err);
      setInventory([]);
    }
  };

  const handleQuantityChange = (productId, quantity) => {
    setSelectedProducts({
      ...selectedProducts,
      [productId]: parseInt(quantity) || 0,
    });
  };

  const handleCreateOrder = async () => {
    if (!session?.user?.workstationId) {
      setError("Cannot create order: workstation ID not found in session");
      return;
    }

    const orderItems = Object.entries(selectedProducts)
      .filter(([_, quantity]) => quantity > 0)
      .map(([productId, quantity]) => ({
        itemType: "PRODUCT",
        itemId: parseInt(productId),
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
      const response = await api.post("/customer-orders", {
        orderItems,
        workstationId: session.user.workstationId,
        notes: "Plant warehouse order",
      });

      setSuccessMessage(`Order created: ${response.data.orderNumber} - Click "Fulfill" to process`);
      setSelectedProducts({});
      fetchOrders();
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
      const response = await api.put(`/customer-orders/${orderId}/fulfill`);
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
    setError(null); setSuccessMessage(null);
    try {
      await api.put(`/customer-orders/${orderId}/confirm`);
      setSuccessMessage('Order confirmed');
      fetchOrders();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleProcessing = async (orderId) => {
    setError(null); setSuccessMessage(null);
    try {
      await api.put(`/customer-orders/${orderId}/processing`);
      setSuccessMessage('Order moved to PROCESSING');
      fetchOrders();
    } catch (err) {
      setError("Failed to mark processing: " + (err.response?.data?.message || err.message));
    }
  };

  const handleComplete = async (orderId) => {
    setError(null); setSuccessMessage(null);
    try {
      await api.put(`/customer-orders/${orderId}/complete`);
      setSuccessMessage('Order completed');
      fetchOrders();
      fetchInventory();
    } catch (err) {
      setError("Failed to complete order: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm('Cancel this order?')) return;
    setError(null); setSuccessMessage(null);
    try {
      await api.put(`/customer-orders/${orderId}/cancel`);
      setSuccessMessage('Order cancelled');
      fetchOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
    }
  };

  return (
    <section className="plant-warehouse-page">
      {/* Header */}
      <div className="page-header">
        <h1 className="page-title">🏢 Plant Warehouse Dashboard</h1>
        <p className="page-subtitle">Manage inventory and customer orders</p>
      </div>

      {session?.user?.workstationId ? (
        <p style={{ fontSize: "0.9rem", color: "#666", marginBottom: "1rem" }}>Workstation ID: <strong>{session.user.workstationId}</strong></p>
      ) : (
        <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
          <p className="font-semibold text-red-900">⚠️ No workstation assigned</p>
          <p className="text-red-800 text-sm mt-1">Contact administrator to assign a workstation to your account.</p>
        </div>
      )}

      {error && (
        <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
          <p className="font-semibold text-red-900">Error</p>
          <p className="text-red-800 text-sm mt-1">{error}</p>
          <button
            onClick={() => setError(null)}
            className="text-red-600 hover:text-red-800 font-semibold text-sm mt-2"
          >
            Dismiss
          </button>
        </div>
      )}

      {successMessage && (
        <div className="form-success-details mb-6">
          <p className="font-semibold text-green-900">Success</p>
          <p className="text-green-800 text-sm mt-1">{successMessage}</p>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-green-700 hover:text-green-900 font-semibold text-sm mt-2"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Two Column Section: Create Order (70%) + Current Inventory (30%) */}
      <div className="two-column-section">
        {/* Create Customer Order - 70% */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 create-order-box">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">➕ Create Customer Order</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="products-table w-full">
              <thead>
                <tr>
                  <th>Product Variant</th>
                  <th>Price</th>
                  <th>Est. Time</th>
                  <th>Quantity</th>
                </tr>
              </thead>
              <tbody>
                {products.length > 0 ? (
                  products.map((product) => (
                    <tr key={product.id}>
                      <td>{product.name || "Unknown"}</td>
                      <td>${(product.price || 0).toFixed(2)}</td>
                      <td>{product.estimatedTimeMinutes || 0} min</td>
                      <td>
                        <input
                          type="number"
                          min="0"
                          value={selectedProducts[product.id] || 0}
                          onChange={(e) => handleQuantityChange(product.id, e.target.value)}
                          className="px-3 py-2 border border-gray-300 rounded text-center w-20 focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="4" style={{ textAlign: 'center' }}>No products available</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="px-6 py-4 border-t border-gray-200 bg-gray-50">
            <button
              onClick={handleCreateOrder}
              disabled={loading}
              className="primary-link"
            >
              {loading ? "Creating Order..." : "Create Order"}
            </button>
          </div>
        </div>

        {/* Current Inventory - 30% */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 inventory-box">
          <div className="bg-blue-50 px-6 py-3 border-b border-blue-200">
            <h2 className="text-lg font-semibold text-blue-900">📦 Current Inventory</h2>
          </div>
          <div className="inventory-stats-grid">
            {inventory.length > 0 ? (
              inventory.map((item, idx) => (
                <div key={idx} className="inventory-stat-box">
                  <div className="inventory-stat-label">{PRODUCT_NAMES[item.itemId]?.name || `Product #${item.itemId}`}</div>
                  <div className="inventory-stat-value">{item.quantity}</div>
                  <div className="inventory-stat-id">ID: #{item.itemId}</div>
                </div>
              ))
            ) : (
              <div className="p-4 text-center text-gray-500" style={{ gridColumn: '1 / -1' }}>
                <p className="text-sm">No inventory items</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Recent Orders - 4 Column Grid */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
        <div className="bg-blue-50 px-6 py-3 border-b border-blue-200 flex justify-between items-center">
          <h2 className="text-lg font-semibold text-blue-900">📋 Recent Orders</h2>
          <button 
            onClick={() => navigate("/orders")}
            className="primary-link"
            style={{ marginTop: 0 }}
          >
            View All Orders
          </button>
        </div>
        <div className="p-6">
          {Array.isArray(orders) && orders.length > 0 ? (
            <div className="orders-grid">
              {orders.map((order) => (
                <div key={order.id} className="order-box-card">
                  <div className="order-box-header">
                    <span className={`order-status-badge status-${order.status.toLowerCase()}`}>
                      {order.status}
                    </span>
                  </div>
                  <div className="order-box-body">
                    <p className="order-number">Order #{order.orderNumber}</p>
                    {order.orderItems && order.orderItems.length > 0 ? (
                      <div className="order-items-list">
                        <p style={{ marginBottom: '0.25rem', fontWeight: '500', color: '#666', fontSize: '0.7rem' }}>Items:</p>
                        {order.orderItems.map((item, idx) => {
                          const productName = PRODUCT_NAMES[item.itemId]?.name || item.name || item.itemType || `Item ${idx + 1}`;
                          return (
                          <div key={idx} style={{ fontSize: '0.7rem', marginBottom: '0.15rem', paddingLeft: '0.5rem', color: '#333' }}>
                            <span style={{ fontWeight: '600' }}>{productName}</span>
                            <span style={{ color: '#999', margin: '0 0.25rem' }}>—</span>
                            <span>Qty: <span style={{ color: '#059669', fontWeight: '600' }}>{item.quantity}</span></span>
                            {item.status && (
                              <>
                                <span style={{ color: '#999', margin: '0 0.25rem' }}>|</span>
                                <span style={{ fontSize: '0.65rem', fontWeight: '500', color: '#0b5394' }}>{item.status}</span>
                              </>
                            )}
                          </div>
                          );
                        })}
                      </div>
                    ) : (
                      <p className="order-info"><strong>Items:</strong> None</p>
                    )}
                    <p className="order-date">{new Date(order.orderDate).toLocaleDateString('en-US', {month: 'short', day: 'numeric'})}</p>
                  </div>
                  <div className="order-box-footer">
                    <div className="actions" style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                      <button className="edit-btn" onClick={() => handleConfirm(order.id)} disabled={order.status !== 'PENDING'}>
                        Confirm
                      </button>
                      <button className="edit-btn" onClick={() => handleProcessing(order.id)} disabled={!(order.status === 'PENDING' || order.status === 'CONFIRMED')}>
                        Process
                      </button>
                      <button className="edit-btn" onClick={() => handleFulfillOrder(order.id)} disabled={fulfillingOrderId === order.id}>
                        {fulfillingOrderId === order.id ? 'Processing…' : 'Fulfill'}
                      </button>
                      <button className="edit-btn" onClick={() => handleComplete(order.id)} disabled={!(order.status === 'PROCESSING' || order.status === 'CONFIRMED')}>
                        Complete
                      </button>
                      <button className="delete-btn" onClick={() => handleCancel(order.id)} disabled={order.status === 'COMPLETED'}>
                        Cancel
                      </button>
                    </div>
                  </div>
                </div>
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

        /* Two Column Layout: Create Order (40%) + Inventory (40%) */
        .two-column-section {
          display: grid;
          grid-template-columns: 40% 40%;
          gap: 2rem;
          margin-bottom: 2rem;
          max-width: 100%;
          overflow: hidden;
          justify-content: center;
        }

        .create-order-box {
          display: flex;
          flex-direction: column;
          max-width: 100%;
          overflow: hidden;
        }

        .create-order-box .overflow-x-auto {
          flex-grow: 1;
          overflow-x: auto;
          overflow-y: auto;
          max-height: 400px;
        }

        .inventory-box {
          display: flex;
          flex-direction: column;
          max-width: 100%;
          overflow: hidden;
        }

        .inventory-stats-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 0.5rem;
          padding: 0.75rem;
        }

        .primary-button {
          padding: 0.75rem 1.5rem;
          background: #0b5394;
          color: white;
          border: none;
          border-radius: 0.5rem;
          cursor: pointer;
          font-weight: 600;
          transition: background 0.3s ease;
          width: 100%;
        }

        .primary-button:hover:not(:disabled) {
          background: #0a4070;
        }

        .primary-button:disabled {
          background: #ccc;
          cursor: not-allowed;
        }

        .orders-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 1rem;
        }

        .order-box-card {
          background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
          border: 1px solid #e0e0e0;
          border-radius: 0.75rem;
          overflow: hidden;
          transition: all 0.3s ease;
          display: flex;
          flex-direction: column;
        }

        .order-box-card:hover {
          border-color: #0b5394;
          box-shadow: 0 4px 12px rgba(11, 83, 148, 0.1);
          transform: translateY(-2px);
        }

        .order-box-header {
          padding: 0.4rem 0.5rem;
          background: #f0f4f8;
          border-bottom: 1px solid #e0e0e0;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .order-box-body {
          padding: 0.5rem;
          flex-grow: 1;
        }

        .order-number {
          font-size: 0.8rem;
          font-weight: 700;
          color: #0b5394;
          margin: 0 0 0.3rem 0;
        }

        .order-info {
          font-size: 0.75rem;
          color: #666;
          margin: 0.15rem 0;
        }

        .order-date {
          font-size: 0.7rem;
          color: #999;
          margin: 0.3rem 0 0 0;
        }

        .order-box-footer {
          padding: 0.4rem 0.5rem;
          border-top: 1px solid #e0e0e0;
          background: #fafbfc;
        }

        .order-status-badge {
          display: inline-block;
          padding: 0.15rem 0.4rem;
          border-radius: 0.25rem;
          font-size: 0.65rem;
          font-weight: 700;
          text-transform: uppercase;
        }

        .status-pending {
          background: #fff3cd;
          color: #856404;
        }

        .status-processing {
          background: #cfe2ff;
          color: #084298;
        }

        .status-completed {
          background: #d1e7dd;
          color: #0f5132;
        }

        .fulfill-button {
          width: 100%;
          padding: 0.5rem;
          background: #27ae60;
          color: white;
          border: none;
          border-radius: 0.375rem;
          cursor: pointer;
          font-weight: 600;
          font-size: 0.85rem;
          transition: background 0.3s ease;
        }

        .fulfill-button:hover:not(:disabled) {
          background: #229954;
        }

        .fulfill-button:disabled {
          background: #ccc;
          cursor: not-allowed;
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
  );
}

export default PlantWarehousePage;
