import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout, StatsCard, InventoryTable, Notification } from "../../components";
import WarehouseOrderCard from "../../components/WarehouseOrderCard";
import { getInventoryStatusColor, generateAcronym, getProductDisplayName } from "../../utils/dashboardHelpers";

function ModulesSupermarketDashboard() {
  const { session } = useAuth();
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [fulfillmentInProgress, setFulfillmentInProgress] = useState({});
  const [confirmationInProgress, setConfirmationInProgress] = useState({});
  const [selectedModule, setSelectedModule] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [showProductionOrderForm, setShowProductionOrderForm] = useState(false);
  const [productionOrderForm, setProductionOrderForm] = useState({
    sourceWarehouseOrderId: null,
    priority: 'NORMAL',
    notes: '',
    missingModules: [],
    warehouseOrderNumber: ''
  });
  const [creatingProductionOrder, setCreatingProductionOrder] = useState(false);
  const [prioritySelectionMode, setPrioritySelectionMode] = useState({}); // Track which orders are in priority selection mode
  const [orderMessages, setOrderMessages] = useState({}); // Store notification messages for each order

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: session?.user?.workstation?.name || 'MODS-SP'
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
    }, 30000); // Increased to 30s to reduce page jump
    return () => clearInterval(interval);
  }, [session?.user?.workstationId]);

  useEffect(() => {
    applyFilter(warehouseOrders, filterStatus);
  }, [filterStatus, warehouseOrders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const fetchWarehouseOrders = async () => {
    try {
      const workstationId = session?.user?.workstationId || 8;
      const response = await axios.get(`/api/warehouse-orders/workstation/${workstationId}`);
      const data = response.data;
      console.log('[WarehouseOrders] Fetched data:', JSON.stringify(data, null, 2));
      if (Array.isArray(data)) {
        setWarehouseOrders(data);
        applyFilter(data, filterStatus);
        setError(null);
      } else {
        setWarehouseOrders([]);
        setFilteredOrders([]);
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setWarehouseOrders([]);
        setFilteredOrders([]);
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
      addNotification(`Order ${orderNumber} confirmed - checking stock availability...`, 'success');
      
      // Fetch updated orders and inventory to check stock automatically
      await fetchWarehouseOrders();
      await fetchInventory();
      
      // After fetching, the needsProduction check will automatically determine 
      // whether to show Fulfill or Production Order button
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

  const handleCreateProductionOrder = async (warehouseOrder) => {
    // Enter priority selection mode for this order
    setPrioritySelectionMode(prev => ({
      ...prev,
      [warehouseOrder.id]: true
    }));
  };

  const handleSelectPriority = async (warehouseOrder, priority) => {
    setCreatingProductionOrder(true);
    setError(null);

    try {
      const payload = {
        sourceCustomerOrderId: null,
        sourceWarehouseOrderId: warehouseOrder.id,
        priority: priority,
        dueDate: null,
        notes: `Production order for warehouse order ${warehouseOrder.warehouseOrderNumber}`,
        createdByWorkstationId: session?.user?.workstationId,
        assignedWorkstationId: null,
        triggerScenario: 'SCENARIO_3'
      };

      const response = await axios.post('/api/production-orders/create', payload);
      
      // Exit priority selection mode
      setPrioritySelectionMode(prev => ({
        ...prev,
        [warehouseOrder.id]: false
      }));

      // Create enhanced message with SimAL response (if available)
      let messageText = `✓ PO-${response.data.productionOrderNumber} | Priority: ${priority}`;
      
      // If SimAL response is available in the response, include it
      if (response.data.simAlScheduleId) {
        messageText += ` | SimAL ID: ${response.data.simAlScheduleId}`;
      }
      if (response.data.plannedStartTime && response.data.plannedFinishTime) {
        const startTime = new Date(response.data.plannedStartTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        const finishTime = new Date(response.data.plannedFinishTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        messageText += ` | Schedule: ${startTime} - ${finishTime}`;
      }
      
      setOrderMessages(prev => ({
        ...prev,
        [warehouseOrder.id]: {
          text: messageText,
          type: 'success'
        }
      }));

      addNotification(`Production order ${response.data.productionOrderNumber} created with priority ${priority}`, 'success');
      await fetchWarehouseOrders();
    } catch (err) {
      setError("Failed to create production order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to create production order", 'error');
      // Exit priority selection mode on error
      setPrioritySelectionMode(prev => ({
        ...prev,
        [warehouseOrder.id]: false
      }));
    } finally {
      setCreatingProductionOrder(false);
    }
  };

  const handleSubmitProductionOrder = async (e) => {
    e.preventDefault();
    setCreatingProductionOrder(true);
    setError(null);

    try {
      const payload = {
        sourceCustomerOrderId: null,
        sourceWarehouseOrderId: productionOrderForm.sourceWarehouseOrderId,
        priority: productionOrderForm.priority,
        dueDate: null,
        notes: productionOrderForm.notes,
        createdByWorkstationId: session?.user?.workstationId,
        assignedWorkstationId: null, // Will be assigned by production planning
        triggerScenario: 'SCENARIO_3'
      };

      const response = await axios.post('/api/production-orders/create', payload);
      addNotification(`Production order ${response.data.productionOrderNumber} created`, 'success');
      setShowProductionOrderForm(false);
      setProductionOrderForm({
        sourceWarehouseOrderId: null,
        priority: 'NORMAL',
        notes: '',
        missingModules: [],
        warehouseOrderNumber: ''
      });
      await fetchWarehouseOrders();
    } catch (err) {
      setError("Failed to create production order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to create production order", 'error');
    } finally {
      setCreatingProductionOrder(false);
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

  // Check if production order is needed for a warehouse order
  // ARCHITECTURE:
  // - Plant Warehouse stores PRODUCTS (finished goods)
  // - Modules Supermarket stores MODULES (sub-assemblies)
  // - Warehouse Orders contain PRODUCTS (what Plant Warehouse needs)
  // - This function checks if Modules Supermarket has enough MODULES to assemble those PRODUCTS
  const checkIfProductionNeeded = (warehouseOrder) => {
    console.log(`[ProductionCheck] ==== CHECKING ORDER ${warehouseOrder.warehouseOrderNumber} ====`);
    console.log(`[ProductionCheck] Order Status: ${warehouseOrder.status}`);
    console.log(`[ProductionCheck] Architecture: WH Order contains PRODUCTS, Supermarket stocks MODULES`);
    
    if (warehouseOrder.status !== 'PROCESSING') {
      console.log(`[ProductionCheck] ❌ Order not PROCESSING (status: ${warehouseOrder.status}) - returning false`);
      return false;
    }
    
    // Check if warehouseOrderItems exists and has data
    if (!warehouseOrder.warehouseOrderItems || warehouseOrder.warehouseOrderItems.length === 0) {
      console.log(`[ProductionCheck] ❌ CRITICAL: Warehouse order has NO ITEMS!`);
      return false;
    }
    
    // Warehouse orders contain PRODUCTS requested by Plant Warehouse
    const productItems = warehouseOrder.warehouseOrderItems.filter(item => item.itemType === 'PRODUCT');
    
    console.log(`[ProductionCheck] Warehouse order has ${productItems.length} PRODUCT items:`);
    productItems.forEach(item => {
      console.log(`[ProductionCheck]   - ${item.itemName} (ID: ${item.itemId}) x${item.requestedQuantity}`);
    });
    
    if (productItems.length === 0) {
      console.log(`[ProductionCheck] ⚠️ No PRODUCT items in warehouse order`);
      return false;
    }
    
    console.log(`[ProductionCheck] Supermarket Inventory (${inventory.length} items):`);
    inventory.forEach(inv => {
      console.log(`[ProductionCheck]   - ${inv.itemName} (${inv.itemType}) x${inv.quantity}`);
    });
    
    // CRITICAL LOGIC:
    // We need to check if Modules Supermarket has enough MODULES to assemble the requested PRODUCTS
    // This requires a BOM (Bill of Materials) mapping: PRODUCT → required MODULES
    // 
    // TEMPORARY SOLUTION: For now, check if the SAME ITEM exists in supermarket inventory
    // This works if the backend is mapping products to their required modules correctly
    const needsProduction = productItems.some(productItem => {
      // Check if we have this product's modules in inventory
      // NOTE: This assumes productItem.itemId corresponds to a module or the item exists in inventory
      const stockItem = inventory.find(inv => inv.itemId === productItem.itemId);
      const currentStock = stockItem ? stockItem.quantity : 0;
      const needed = productItem.requestedQuantity;
      const insufficient = !stockItem || currentStock < needed;
      
      console.log(`[ProductionCheck] Checking PRODUCT "${productItem.itemName}" (ID: ${productItem.itemId})`);
      console.log(`[ProductionCheck]   → Looking for matching stock item...`);
      
      if (stockItem) {
        console.log(`[ProductionCheck]   → Found: ${stockItem.itemName} (${stockItem.itemType})`);
        console.log(`[ProductionCheck]   → Stock: ${currentStock}, Needed: ${needed}, Insufficient: ${insufficient}`);
      } else {
        console.log(`[ProductionCheck]   → NOT FOUND in inventory - Production needed!`);
      }
      
      return insufficient;
    });
    
    console.log(`[ProductionCheck] ✅ Final Result - Needs Production: ${needsProduction}`);
    console.log(`[ProductionCheck] ${needsProduction ? '🔴 INSUFFICIENT MODULES → Show "Production Order" button' : '🟢 SUFFICIENT MODULES → Show "Fulfill" button'}`);
    console.log(`[ProductionCheck] ==== END CHECK ====\n`);
    return needsProduction;
  };

  // Render Module Inventory (Primary Content)
  const renderModuleInventory = () => {
    const moduleInventory = inventory.filter(item => item.itemType === "MODULE");
    
    return (
      <InventoryTable
        title="Module Inventory"
        inventory={moduleInventory}
        items={[]}
        getStatusColor={getInventoryStatusColor}
        getItemName={(item) => {
          if (item.itemName) {
            const acronym = generateAcronym(item.itemName);
            return `${acronym} (${item.itemName})`;
          }
          return `${item.itemType} #${item.itemId}`;
        }}
        headerColor="purple"
      />
    );
  };

  // Render Warehouse Orders Section
  const renderWarehouseOrdersSection = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-orange">
        <h2 className="dashboard-box-header-title">📋 Warehouse Orders</h2>
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
            <option value="FULFILLED">Fulfilled</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button 
            onClick={fetchWarehouseOrders} 
            disabled={loading} 
            className="dashboard-box-header-action"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>
      <div className="dashboard-box-content">
        {Array.isArray(filteredOrders) && filteredOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <WarehouseOrderCard
                key={order.id}
                order={order}
                onConfirm={handleConfirmOrder}
                onFulfill={handleFulfillOrder}
                onCancel={handleCancel}
                onCreateProductionOrder={handleCreateProductionOrder}
                  onSelectPriority={handleSelectPriority}
                  needsProduction={checkIfProductionNeeded(order)}
                  isProcessing={fulfillmentInProgress[order.id]}
                  isConfirming={confirmationInProgress[order.id]}
                  showPrioritySelection={prioritySelectionMode[order.id]}
                  creatingOrder={creatingProductionOrder}
                  notificationMessage={orderMessages[order.id]}
                  getProductDisplayName={getProductDisplayName}
              />
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No warehouse orders found</p>
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

  // Render Info Box
  const renderInfoBox = () => (
    <div className="dashboard-info-box">
      <h3 style={{ fontWeight: 'bold', fontSize: '1rem', marginBottom: '1rem', color: '#9a3412' }}>
        About Warehouse Orders & Production
      </h3>
      <ul style={{ marginLeft: '1.5rem', fontSize: '0.875rem', lineHeight: '1.75', color: '#9a3412' }}>
        <li><strong>SCENARIO 2:</strong> Plant Warehouse requests items from Modules Supermarket</li>
        <li><strong>Fulfill Order:</strong> Complete warehouse orders if sufficient module stock available</li>
        <li><strong>Create Production Order:</strong> If modules are out of stock, create production order</li>
        <li><strong>Module Inventory:</strong> View current stock levels for all modules</li>
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

      {/* Production Order Creation Modal */}
      {showProductionOrderForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onClick={() => setShowProductionOrderForm(false)}>
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSubmitProductionOrder}>
              <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
                <h2 className="text-xl font-bold text-gray-900">Create Production Order</h2>
                <button type="button" className="text-gray-400 hover:text-gray-600 text-2xl font-bold" onClick={() => setShowProductionOrderForm(false)}>×</button>
              </div>
              
              <div className="px-6 py-4 space-y-4">
                <p className="text-sm text-gray-600">
                  Production order will be automatically created for missing modules from warehouse order <strong>{productionOrderForm.warehouseOrderNumber}</strong>.
                </p>

                {/* Display auto-loaded missing modules */}
                {productionOrderForm.missingModules && productionOrderForm.missingModules.length > 0 && (
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <h3 className="text-sm font-semibold text-gray-900 mb-3">Missing Modules (Auto-loaded)</h3>
                    <div className="space-y-2">
                      {productionOrderForm.missingModules.map((module, index) => (
                        <div key={index} className="flex items-center justify-between bg-white px-3 py-2 rounded border border-gray-200">
                          <div>
                            <span className="font-medium text-gray-900">{module.moduleName}</span>
                            <span className="text-xs text-gray-500 ml-2">(ID: {module.moduleId})</span>
                          </div>
                          <span className="text-sm font-semibold text-blue-600">Qty: {module.quantityNeeded}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Priority</label>
                  <select
                    value={productionOrderForm.priority}
                    onChange={(e) => setProductionOrderForm({ ...productionOrderForm, priority: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  >
                    <option value="LOW">Low</option>
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Notes (Optional)</label>
                  <textarea
                    value={productionOrderForm.notes}
                    onChange={(e) => setProductionOrderForm({ ...productionOrderForm, notes: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows="3"
                    placeholder="Optional notes about this production order..."
                  />
                </div>
              </div>
              
              <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowProductionOrderForm(false)}
                  className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition"
                  disabled={creatingProductionOrder}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition disabled:opacity-50"
                  disabled={creatingProductionOrder}
                >
                  {creatingProductionOrder ? 'Creating...' : 'Create Production Order'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
