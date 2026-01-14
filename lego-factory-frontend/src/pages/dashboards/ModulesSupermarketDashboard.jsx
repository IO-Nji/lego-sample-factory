import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout, StatsCard, InventoryTable, Notification } from "../../components";
import WarehouseOrderCard from "../../components/WarehouseOrderCard";
import { getInventoryStatusColor } from "../../utils/dashboardHelpers";

function ModulesSupermarketDashboard() {
  const { session } = useAuth();
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fulfillmentInProgress, setFulfillmentInProgress] = useState({});
  const [confirmationInProgress, setConfirmationInProgress] = useState({});
  const [selectedModule, setSelectedModule] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: session?.user?.workstation?.name || 'Modules Supermarket'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

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

  const handleConfirmOrder = async (orderId, orderNumber) => {
    setConfirmationInProgress(prev => ({ ...prev, [orderId]: true }));
    setError(null);

    try {
      await axios.put(`/api/warehouse-orders/${orderId}/confirm`);
      addNotification(`Order ${orderNumber} confirmed`, 'success');
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      setError("Failed to confirm order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to confirm order", 'error');
    } finally {
      setConfirmationInProgress(prev => ({ ...prev, [orderId]: false }));
    }
  };

  const handleFulfillOrder = async (orderId, orderNumber) => {
    setFulfillmentInProgress(prev => ({ ...prev, [orderId]: true }));
    setError(null);

    try {
      const response = await axios.put(`/api/warehouse-orders/${orderId}/fulfill-modules`);
      addNotification(`Order ${orderNumber} fulfilled`, 'success');
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      setError("Failed to fulfill order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to fulfill order", 'error');
    } finally {
      setFulfillmentInProgress(prev => ({ ...prev, [orderId]: false }));
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this warehouse order?")) return;
    setError(null);
    
    try {
      await axios.patch(`/api/warehouse-orders/${orderId}/status?status=CANCELLED`);
      addNotification("Warehouse order cancelled", 'warning');
      await fetchWarehouseOrders();
    } catch (err) {
      setError("Failed to cancel order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to cancel order", 'error');
    }
  };

  const getInventoryStatusColor = (quantity) => {
    if (quantity === 0) return '#991b1b';
    if (quantity <= 5) return '#ef4444';
    if (quantity <= 20) return '#f59e0b';
    return '#10b981';
  };

  // Render Stats Cards
  const renderStatsCards = () => (
    <>
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
    </>
  );

  // Render Module Inventory (Primary Content)
  const renderModuleInventory = () => {
    const moduleInventory = inventory.filter(item => item.itemType === "MODULE");
    
    return (
      <InventoryTable
        title="Module Inventory"
        inventory={moduleInventory}
        items={[]}
        getStatusColor={getInventoryStatusColor}
        getItemName={(item) => `${item.itemType} #${item.itemId}`}
        headerColor="purple"
      />
    );
  };

  // Render Warehouse Orders Section
  const renderWarehouseOrdersSection = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-orange">
        <h2 className="dashboard-box-header-title">📋 Warehouse Orders</h2>
        <button 
          onClick={fetchWarehouseOrders} 
          disabled={loading} 
          className="dashboard-box-header-action"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>
      <div className="dashboard-box-content">
        {Array.isArray(warehouseOrders) && warehouseOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {warehouseOrders.map((order) => (
              <WarehouseOrderCard
                key={order.id}
                order={order}
                onConfirm={handleConfirmOrder}
                onFulfill={handleFulfillOrder}
                onCancel={handleCancel}
                isProcessing={fulfillmentInProgress[order.id]}
                isConfirming={confirmationInProgress[order.id]}
              />
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No warehouse orders found</p>
            <p className="dashboard-empty-state-text">Orders will appear here when created</p>
          </div>
        )}
      </div>
    </>
  );

  // Render Info Box
  const renderInfoBox = () => (
    <div className="dashboard-info-box">
      <h3 style={{ fontWeight: 'bold', fontSize: '1rem', marginBottom: '1rem', color: '#9a3412' }}>
        About Warehouse Orders
      </h3>
      <ul style={{ marginLeft: '1.5rem', fontSize: '0.875rem', lineHeight: '1.75', color: '#9a3412' }}>
        <li><strong>SCENARIO 2:</strong> Plant Warehouse has no stock, requesting all items from Modules Supermarket</li>
        <li><strong>SCENARIO 3:</strong> Plant Warehouse has partial stock, requesting remaining items from Modules Supermarket</li>
        <li>Click <strong>Fulfill</strong> to complete the warehouse order and deduct stock from inventory</li>
        <li>Orders are automatically fetched every 15 seconds for real-time updates</li>
      </ul>
    </div>
  );

  return (
    <>
      <DashboardLayout
        title="Modules Supermarket Dashboard"
        subtitle={`Manage warehouse orders and module inventory${session?.user?.workstationId ? ` | Workstation ID: ${session.user.workstationId}` : ''}`}
        icon="🏭"
        layout="compact"
        statsCards={renderStatsCards()}
        primaryContent={renderModuleInventory()}
        notifications={
          <Notification 
            notifications={notifications}
            title="Supermarket Activity"
            maxVisible={5}
            onClear={clearNotifications}
          />
        }
        ordersSection={renderWarehouseOrdersSection()}
        infoBox={renderInfoBox()}
      />

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
    </>
  );
}

export default ModulesSupermarketDashboard;
