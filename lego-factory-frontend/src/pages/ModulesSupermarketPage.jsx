import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import axios from "axios";
import PageHeader from "../components/PageHeader";
import WarehouseOrderCard from "../components/WarehouseOrderCard";
import StatsCard from "../components/StatsCard";
import "../styles/StandardPage.css";
import "../styles/DashboardStandard.css";

function ModulesSupermarketPage() {
  const { session } = useAuth();
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [fulfillmentInProgress, setFulfillmentInProgress] = useState({});
  const [selectedModule, setSelectedModule] = useState(null);

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchWarehouseOrders();
      fetchInventory();
    }
    // Refresh orders every 15 seconds for live updates
    const interval = setInterval(() => {
      fetchWarehouseOrders();
      fetchInventory();
    }, 15000);
    return () => clearInterval(interval);
  }, [session?.user?.workstationId]);

  const fetchWarehouseOrders = async () => {
    try {
      // Fetch warehouse orders for Modules Supermarket (workstation 8)
      const workstationId = session?.user?.workstationId || 8;
      console.log("Fetching warehouse orders for workstation:", workstationId);
      
      const response = await axios.get(`/api/warehouse-orders/workstation/${workstationId}`);
      console.log("Warehouse orders response:", response.data);
      
      const data = response.data;
      if (Array.isArray(data)) {
        setWarehouseOrders(data);
        setError(null);
      } else {
        console.warn("Expected array of warehouse orders, got:", data);
        setWarehouseOrders([]);
        setError("Unexpected response format from server.");
      }
    } catch (err) {
      console.error("Failed to fetch warehouse orders:", err);
      if (err.response?.status === 403) {
        setError("Authorization denied. You need MODULES_SUPERMARKET role.");
      } else if (err.response?.status === 404) {
        // No orders exist yet
        setWarehouseOrders([]);
        setError(null);
      } else {
        setError("Failed to load warehouse orders: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const fetchInventory = async () => {
    if (!session?.user?.workstationId) return;
    try {
      const response = await axios.get(`/api/stock/workstation/${session.user.workstationId}`);
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

  const handleFulfillOrder = async (orderId, orderNumber) => {
    setFulfillmentInProgress(prev => ({ ...prev, [orderId]: true }));
    setError(null);
    setSuccessMessage(null);

    try {
      console.log("Fulfilling warehouse order:", orderId);
      const response = await axios.put(`/api/warehouse-orders/${orderId}/fulfill-modules`);
      
      console.log("Fulfillment response:", response.data);
      setSuccessMessage(`Warehouse order ${orderNumber} fulfilled successfully!`);
      
      // Refresh orders after fulfillment
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      console.error("Failed to fulfill warehouse order:", err);
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
    } finally {
      setFulfillmentInProgress(prev => ({ ...prev, [orderId]: false }));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm('Cancel this warehouse order?')) return;
    setError(null);
    setSuccessMessage(null);

    try {
      console.log("Cancelling warehouse order:", orderId);
      const response = await axios.patch(
        `/api/warehouse-orders/${orderId}/status?status=CANCELLED`
      );
      
      console.log("Cancel response:", response.data);
      setSuccessMessage("Warehouse order cancelled successfully!");
      
      // Refresh orders after cancellation
      await fetchWarehouseOrders();
    } catch (err) {
      console.error("Failed to cancel warehouse order:", err);
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Modules Supermarket"
        subtitle="Manage incoming warehouse orders for module fulfillment"
        icon="🏢"
      />
      
      <section className="modules-supermarket-page">
        {/* Messages */}
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

        {/* Statistics Cards */}
        <div className="stats-grid" style={{ marginBottom: '2rem' }}>
          <StatsCard 
            value={warehouseOrders.length} 
            label="Total Orders" 
            variant="default"
          />
          <StatsCard 
            value={warehouseOrders.filter(o => o.status === "PENDING").length} 
            label="Pending" 
            variant="pending"
          />
          <StatsCard 
            value={warehouseOrders.filter(o => o.status === "PROCESSING").length} 
            label="Processing" 
            variant="processing"
          />
          <StatsCard 
            value={warehouseOrders.filter(o => o.status === "FULFILLED").length} 
            label="Fulfilled" 
            variant="completed"
          />
        </div>

        {/* Current Inventory */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
          <div className="bg-orange-50 px-6 py-3 border-b border-orange-200">
            <h2 className="text-lg font-semibold text-orange-900">📦 Current Inventory</h2>
          </div>
          <div className="overflow-x-auto">
            {inventory.length === 0 ? (
              <div className="p-6 text-center text-gray-500">
                <p className="text-sm">No inventory items available</p>
              </div>
            ) : (
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Item Type</th>
                    <th>Item ID</th>
                    <th>Quantity</th>
                    <th>Last Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {inventory.map((item, index) => (
                    <tr 
                      key={index} 
                      onClick={() => setSelectedModule(item)}
                      style={{ cursor: 'pointer' }}
                    >
                      <td>{item.itemType}</td>
                      <td>{item.itemId}</td>
                      <td><strong>{item.quantity}</strong></td>
                      <td>{new Date(item.updatedAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Warehouse Orders Section */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
          <div className="bg-orange-50 px-6 py-3 border-b border-orange-200 flex justify-between items-center">
            <h2 className="text-lg font-semibold text-orange-900">📋 Warehouse Orders</h2>
            <button
              onClick={fetchWarehouseOrders}
              disabled={loading}
              className="primary-link"
              style={{ marginTop: 0 }}
            >
              {loading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
          <div className="p-6">
            {warehouseOrders.length === 0 ? (
              <div className="text-center text-gray-500">
                <p className="text-lg font-semibold">No warehouse orders found</p>
                <p className="text-sm mt-2 text-gray-400">Orders will appear here when created</p>
              </div>
            ) : (
              <div className="orders-grid">
                {warehouseOrders.map((order) => (
                  <WarehouseOrderCard
                    key={order.id}
                    order={order}
                    onFulfill={() => handleFulfillOrder(order.id, order.warehouseOrderNumber)}
                    onCancel={() => handleCancel(order.id)}
                    isProcessing={fulfillmentInProgress[order.id]}
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Information Box */}
        <div className="bg-orange-50 border-l-4 border-orange-600 rounded p-6">
          <h3 className="font-bold text-orange-900 text-base mb-4">About Warehouse Orders</h3>
          <ul className="space-y-2 text-orange-800 text-sm">
            <li><strong>SCENARIO_2:</strong> Plant Warehouse has no stock, requesting all items from Modules Supermarket</li>
            <li><strong>SCENARIO_3:</strong> Plant Warehouse has partial stock, requesting remaining items from Modules Supermarket</li>
            <li>Click <strong>Fulfill</strong> to complete the warehouse order and deduct stock from inventory</li>
            <li>Orders are automatically refreshed every 15 seconds for real-time updates</li>
            <li>Click on any <strong>inventory item</strong> in the Current Inventory table to view detailed module information</li>
          </ul>
        </div>

        {/* Module Details Modal */}
        {selectedModule && (
          <div 
            className="modal-overlay" 
            onClick={() => setSelectedModule(null)}
            onKeyDown={(e) => e.key === 'Escape' && setSelectedModule(null)}
            role="button"
            tabIndex={0}
            aria-label="Close modal"
          >
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2 className="modal-title">Module Details</h2>
                <button 
                  className="modal-close"
                  onClick={() => setSelectedModule(null)}
                >
                  ×
                </button>
              </div>
              <div className="modal-body">
                <div className="detail-section">
                  <dl className="detail-list">
                    <dt className="detail-label">Item Type</dt>
                    <dd className="detail-value">{selectedModule.itemType}</dd>
                    
                    <dt className="detail-label">Item ID</dt>
                    <dd className="detail-value">{selectedModule.itemId}</dd>
                  </dl>
                </div>

                <div className="detail-section">
                  <dl className="detail-list">
                    <dt className="detail-label">Stock Level</dt>
                    <dd className="detail-value" style={{ 
                      fontSize: "1.5rem", 
                      fontWeight: "bold",
                      color: selectedModule.quantity > 0 ? "#27ae60" : "#e74c3c"
                    }}>
                      {selectedModule.quantity} units
                    </dd>
                  </dl>
                </div>

                {selectedModule.description && (
                  <div className="detail-section">
                    <dl className="detail-list">
                      <dt className="detail-label">Description</dt>
                      <dd className="detail-value">{selectedModule.description}</dd>
                    </dl>
                  </div>
                )}

                <div className="detail-section">
                  <dl className="detail-list">
                    <dt className="detail-label">Last Updated</dt>
                    <dd className="detail-value">{new Date(selectedModule.updatedAt).toLocaleString()}</dd>
                  </dl>
                </div>
              </div>
              <div className="modal-footer">
                <button 
                  className="secondary-button"
                  onClick={() => setSelectedModule(null)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

export default ModulesSupermarketPage;
