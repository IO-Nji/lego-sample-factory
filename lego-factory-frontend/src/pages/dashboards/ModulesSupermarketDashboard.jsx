import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import WarehouseOrderCard from "../../components/WarehouseOrderCard";
import StatsCard from "../../components/StatsCard";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function ModulesSupermarketDashboard() {
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
    const interval = setInterval(() => {
      if (session?.user?.workstationId) {
        fetchWarehouseOrders();
        fetchInventory();
      }
    }, 15000);
    return () => clearInterval(interval);
  }, [session?.user?.workstationId]);

  const fetchWarehouseOrders = async () => {
    try {
      const workstationId = session?.user?.workstationId || 8;
      const response = await axios.get(`/api/warehouse-orders/workstation/${workstationId}`);
      const data = response.data;
      if (Array.isArray(data)) {
        setWarehouseOrders(data);
        setError(null);
      } else {
        setWarehouseOrders([]);
      }
    } catch (err) {
      if (err.response?.status === 404) {
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
      setInventory(Array.isArray(response.data) ? response.data : []);
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
      const response = await axios.put(`/api/warehouse-orders/${orderId}/fulfill-modules`);
      setSuccessMessage(`Warehouse order ${orderNumber} fulfilled successfully!`);
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
    } finally {
      setFulfillmentInProgress(prev => ({ ...prev, [orderId]: false }));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this warehouse order?")) return;
    setError(null);
    setSuccessMessage(null);
    
    try {
      await axios.patch(`/api/warehouse-orders/${orderId}/status?status=CANCELLED`);
      setSuccessMessage("Warehouse order cancelled successfully!");
      await fetchWarehouseOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
    }
  };

  const getInventoryStatusColor = (quantity) => {
    if (quantity === 0) return '#991b1b';
    if (quantity <= 5) return '#ef4444';
    if (quantity <= 20) return '#f59e0b';
    return '#10b981';
  };

  return (
    <div className="standard-page-container">
      <PageHeader
        title="Modules Supermarket"
        subtitle={`Manage warehouse orders and module inventory${session?.user?.workstationId ? ` | Workstation ID: ${session.user.workstationId}` : ''}`}
        icon="🏢"
      />
      <section className="modules-supermarket-page">

        {/* Error and Success Messages */}
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

        {/* Order Statistics */}
        <div className="stats-grid mb-6">
          <StatsCard value={warehouseOrders.length} label="Total Orders" variant="default" />
          <StatsCard value={warehouseOrders.filter(o => o.status === "PENDING").length} label="Pending" variant="pending" />
          <StatsCard value={warehouseOrders.filter(o => o.status === "PROCESSING").length} label="Processing" variant="processing" />
          <StatsCard value={warehouseOrders.filter(o => o.status === "FULFILLED").length} label="Fulfilled" variant="completed" />
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
              <table className="products-table w-full">
                <thead>
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Item Type</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Item ID</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Quantity</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Last Updated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {inventory.map((item, index) => {
                    const statusColor = getInventoryStatusColor(item.quantity);
                    return (
                      <tr key={index} onClick={() => setSelectedModule(item)} className="hover:bg-gray-50 cursor-pointer">
                        <td className="px-6 py-2 whitespace-nowrap text-sm font-medium text-gray-900">{item.itemType}</td>
                        <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">{item.itemId}</td>
                        <td className="px-6 py-2 whitespace-nowrap text-sm font-bold" style={{ color: statusColor }}>{item.quantity}</td>
                        <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-600">{new Date(item.updatedAt).toLocaleString()}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Warehouse Orders */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
          <div className="bg-orange-50 px-6 py-3 border-b border-orange-200 flex justify-between items-center">
            <h2 className="text-lg font-semibold text-orange-900">📋 Warehouse Orders</h2>
            <button onClick={fetchWarehouseOrders} disabled={loading} className="px-4 py-2 text-sm bg-orange-600 text-white rounded hover:bg-orange-700 transition disabled:opacity-50">
              {loading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
          <div className="p-6">
            {Array.isArray(warehouseOrders) && warehouseOrders.length > 0 ? (
              <div className="orders-grid">
                {warehouseOrders.map((order) => (
                  <WarehouseOrderCard
                    key={order.id}
                    order={order}
                    onFulfill={handleFulfillOrder}
                    onCancel={handleCancel}
                    isProcessing={fulfillmentInProgress[order.id]}
                  />
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500">
                <p className="text-sm">No warehouse orders found</p>
              </div>
            )}
          </div>
        </div>

        {/* Information Box */}
        <div className="bg-orange-50 border-l-4 border-orange-600 rounded p-6">
          <h3 className="font-bold text-orange-900 text-base mb-4">About Warehouse Orders</h3>
          <ul className="space-y-2 text-orange-800 text-sm">
            <li><strong>SCENARIO 2:</strong> Plant Warehouse has no stock, requesting all items from Modules Supermarket</li>
            <li><strong>SCENARIO 3:</strong> Plant Warehouse has partial stock, requesting remaining items from Modules Supermarket</li>
            <li>Click <strong>Fulfill</strong> to complete the warehouse order and deduct stock from inventory</li>
            <li>Orders are automatically fetched every 15 seconds for real-time updates</li>
            <li>Click on any <strong>inventory item</strong> to view detailed module information</li>
          </ul>
        </div>

        {/* Module Details Modal */}
        {selectedModule && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={() => setSelectedModule(null)}>
            <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4" onClick={(e) => e.stopPropagation()}>
              <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h2 className="text-xl font-bold text-gray-900">Module Details</h2>
                <button className="text-gray-400 hover:text-gray-600 text-2xl font-bold" onClick={() => setSelectedModule(null)}>×</button>
              </div>
              <div className="px-6 py-4 space-y-4">
                <div>
                  <dt className="text-sm font-medium text-gray-500">Item Type</dt>
                  <dd className="mt-1 text-lg font-semibold text-gray-900">{selectedModule.itemType}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">Item ID</dt>
                  <dd className="mt-1 text-lg font-semibold text-gray-900">{selectedModule.itemId}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">Stock Level</dt>
                  <dd className="mt-1 text-2xl font-bold" style={{ color: selectedModule.quantity > 0 ? '#059669' : '#dc2626' }}>
                    {selectedModule.quantity} units
                  </dd>
                </div>
                {selectedModule.description && (
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Description</dt>
                    <dd className="mt-1 text-sm text-gray-700">{selectedModule.description}</dd>
                  </div>
                )}
                <div>
                  <dt className="text-sm font-medium text-gray-500">Last Updated</dt>
                  <dd className="mt-1 text-sm text-gray-700">{new Date(selectedModule.updatedAt).toLocaleString()}</dd>
                </div>
              </div>
              <div className="px-6 py-4 border-t border-gray-200 flex justify-end">
                <button className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition" onClick={() => setSelectedModule(null)}>
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

export default ModulesSupermarketDashboard;
