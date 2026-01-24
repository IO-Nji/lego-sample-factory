import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { StandardDashboardLayout, StatisticsGrid, InventoryTable, ActivityLog, OrdersSection } from "../../components";
import WarehouseOrderCard from "../../components/WarehouseOrderCard";
import { getInventoryStatusColor, generateAcronym } from "../../utils/dashboardHelpers";
import { useInventoryDisplay } from "../../hooks/useInventoryDisplay";

function ModulesSupermarketDashboard() {
  const { session } = useAuth();
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  
  // Use centralized inventory hook for modules management (hardcoded WS-8 for Modules Supermarket)
  const { 
    inventory, 
    masterdata: modules,
    getItemName: getModuleName,
    fetchInventory 
  } = useInventoryDisplay('MODULE', 8);
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

  // Module-aware display name function for warehouse order items
  // Uses modules masterdata from useInventoryDisplay hook to get proper names
  const getModuleDisplayName = (itemId, itemType) => {
    if (itemType === 'MODULE') {
      // Find module in masterdata by ID
      const moduleData = modules.find(m => m.id === itemId);
      if (moduleData?.name) {
        return generateAcronym(moduleData.name);
      }
      return `M${itemId}`; // Fallback if module not found in masterdata
    }
    // For other item types, just generate acronym from type
    return generateAcronym(itemType);
  };

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
    // Always fetch warehouse orders on mount (fallback to workstation 8 for Modules Supermarket)
    fetchWarehouseOrders();
    
    const interval = setInterval(() => {
      fetchWarehouseOrders();
      fetchInventory(); // Manual refresh for periodic updates
    }, 30000); // Increased to 30s to reduce page jump
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.user?.workstationId]); // fetchInventory is stable from hook

  const fetchWarehouseOrders = async () => {
    try {
      const workstationId = session?.user?.workstationId || 8;
      const response = await api.get(`/warehouse-orders/workstation/${workstationId}`);
      const data = response.data;
      console.log('[WarehouseOrders] Fetched data:', JSON.stringify(data, null, 2));
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

  // fetchInventory is now provided by useInventoryDisplay hook

  const handleConfirmOrder = async (orderId, orderNumber) => {
    setConfirmationInProgress(prev => ({ ...prev, [orderId]: true }));
    setError(null);

    try {
      await api.put(`/warehouse-orders/${orderId}/confirm`);
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
      const response = await api.put(`/warehouse-orders/${orderId}/fulfill-modules`);
      console.log('[Fulfillment] Response:', response.data);
      
      if (response.data.status === 'FULFILLED') {
        addNotification(`Order ${orderNumber} fulfilled - Final Assembly orders created`, 'success');
        // Clear any notification messages for this order
        setOrderMessages(prev => {
          const updated = { ...prev };
          delete updated[orderId];
          return updated;
        });
      } else {
        addNotification(`Order ${orderNumber} updated - Status: ${response.data.status}`, 'info');
      }
      
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      console.error('[Fulfillment] Error:', err);
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
      await api.patch(`/warehouse-orders/${orderId}/status?status=CANCELLED`);
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
    console.log('[handleSelectPriority] Called with:', { warehouseOrder, priority });
    setCreatingProductionOrder(true);
    setError(null);

    try {
      const orderNum = warehouseOrder.orderNumber || warehouseOrder.warehouseOrderNumber || `ID-${warehouseOrder.id}`;
      const workstationId = session?.user?.workstationId;
      
      console.log('[handleSelectPriority] Session workstation ID:', workstationId);
      console.log('[handleSelectPriority] Warehouse Order ID:', warehouseOrder.id);
      
      const payload = {
        sourceCustomerOrderId: null,
        sourceWarehouseOrderId: warehouseOrder.id,
        priority: priority,
        dueDate: null,
        notes: `Production order for warehouse order ${orderNum}`,
        createdByWorkstationId: workstationId,
        assignedWorkstationId: null,
        triggerScenario: 'SCENARIO_3'
      };

      console.log('[Production Order Creation] Payload:', JSON.stringify(payload, null, 2));
      console.log('[Production Order Creation] Sending POST to /production-orders/create');

      const response = await api.post('/production-orders/create', payload);
      
      console.log('[Production Order Creation] Success! Response:', response.data);
      
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
      
      // Small delay to ensure backend transaction is committed before refresh
      await new Promise(resolve => setTimeout(resolve, 500));
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

      const response = await api.post('/production-orders/create', payload);
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

  // Stats data for StatisticsGrid
  const statsData = [
    { value: warehouseOrders.length, label: 'Total Orders', variant: 'default', icon: '📦' },
    { value: warehouseOrders.filter(o => o.status === "PENDING").length, label: 'Pending', variant: 'pending', icon: '⏳' },
    { value: warehouseOrders.filter(o => o.status === "CONFIRMED").length, label: 'Confirmed', variant: 'info', icon: '✓' },
    { value: warehouseOrders.filter(o => o.status === "AWAITING_PRODUCTION").length, label: 'Production', variant: 'warning', icon: '🏭' },
    { value: warehouseOrders.filter(o => o.status === "PROCESSING").length, label: 'Processing', variant: 'processing', icon: '⚙️' },
    { value: warehouseOrders.filter(o => o.status === "FULFILLED").length, label: 'Fulfilled', variant: 'success', icon: '✅' },
    { value: inventory.length, label: 'Module Types', variant: 'info', icon: '🔧' },
    { value: inventory.reduce((sum, item) => sum + item.quantity, 0), label: 'Total Stock', variant: 'default', icon: '📊' },
    { value: inventory.filter(item => item.quantity < 10).length, label: 'Low Stock', variant: 'warning', icon: '⚠️' },
    { value: inventory.filter(item => item.quantity === 0).length, label: 'Out of Stock', variant: 'danger', icon: '🚫' },
  ];

  // Check if production order is needed for a warehouse order
  // CRITICAL ARCHITECTURE:
  // - Warehouse Orders request PRODUCTS but are fulfilled using MODULES
  // - Modules Supermarket has MODULE inventory
  // - Backend resolves PRODUCT → MODULES mapping via ProductModule table
  // - Backend sets triggerScenario during confirmation based on module availability
  // - DIRECT_FULFILLMENT = modules available → show "Fulfill" button
  // - PRODUCTION_REQUIRED = modules NOT available → show "Order Production" button
  // - PRODUCTION_CREATED = production already ordered → show "Awaiting Production" (disabled)
  const checkIfProductionNeeded = (warehouseOrder) => {
    const orderNum = warehouseOrder.orderNumber || warehouseOrder.warehouseOrderNumber || 'Unknown';
    console.log(`[ProductionCheck] ==== CHECKING ORDER ${orderNum} ====`);
    console.log(`[ProductionCheck] Order Status: ${warehouseOrder.status}`);
    console.log(`[ProductionCheck] Trigger Scenario: ${warehouseOrder.triggerScenario}`);
    
    // If status is AWAITING_PRODUCTION, production order already exists - no need to order again
    if (warehouseOrder.status === 'AWAITING_PRODUCTION') {
      console.log(`[ProductionCheck] ✅ Status is AWAITING_PRODUCTION - production already ordered`);
      return false;
    }
    
    // If triggerScenario indicates production was already created, return false
    if (warehouseOrder.triggerScenario === 'PRODUCTION_CREATED') {
      console.log(`[ProductionCheck] ✅ Trigger scenario is PRODUCTION_CREATED - production already ordered`);
      return false;
    }
    
    // Only check orders in CONFIRMED status (after confirmation, ready for action)
    if (warehouseOrder.status !== 'CONFIRMED') {
      console.log(`[ProductionCheck] ❌ Order not CONFIRMED (status: ${warehouseOrder.status}) - returning false`);
      return false;
    }
    
    // CRITICAL: Check specifically for PRODUCTION_REQUIRED
    // This indicates production is needed and NOT yet created
    if (warehouseOrder.triggerScenario === 'PRODUCTION_REQUIRED') {
      console.log(`[ProductionCheck] ✅ Trigger Scenario is PRODUCTION_REQUIRED - production needed`);
      return true;
    }
    
    // DIRECT_FULFILLMENT means modules are available, no production needed
    if (warehouseOrder.triggerScenario === 'DIRECT_FULFILLMENT') {
      console.log(`[ProductionCheck] ✅ Trigger Scenario is DIRECT_FULFILLMENT - no production needed`);
      return false;
    }
    
    // If no trigger scenario is set or unrecognized, default to false
    console.log(`[ProductionCheck] ⚠️ WARNING: Unrecognized triggerScenario - defaulting to false`);
    return false;
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
          // Find module from fetched masterdata by itemId
          const module = modules.find(m => m.id === item.itemId);
          if (module?.name) {
            const acronym = generateAcronym(module.name);
            return `${acronym} (${module.name})`;
          }
          return `MODULE #${item.itemId}`;
        }}
        headerColor="purple"
      />
    );
  };

  // Render Warehouse Orders Section using standardized OrdersSection
  const renderWarehouseOrdersSection = () => (
    <OrdersSection
      title="Warehouse Orders"
      icon="📋"
      orders={warehouseOrders}
      filterOptions={[
        { value: 'ALL', label: 'All Orders' },
        { value: 'PENDING', label: 'Pending' },
        { value: 'CONFIRMED', label: 'Confirmed' },
        { value: 'AWAITING_PRODUCTION', label: 'Production' },
        { value: 'PROCESSING', label: 'Processing' },
        { value: 'FULFILLED', label: 'Fulfilled' },
        { value: 'CANCELLED', label: 'Cancelled' }
      ]}
      sortOptions={[
        { value: 'warehouseOrderNumber', label: 'Order Number' },
        { value: 'orderDate', label: 'Order Date' },
        { value: 'status', label: 'Status' }
      ]}
      renderCard={(order) => (
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
          getProductDisplayName={getModuleDisplayName}
        />
      )}
      searchPlaceholder="Search by order number..."
      emptyMessage="No warehouse orders found"
    />
  );

  // Render Info Box - Compact version to fit row height
  const renderInfoBox = () => (
    <div className="dashboard-info-box" style={{ padding: '0.75rem' }}>
      <h3 style={{ fontWeight: 'bold', fontSize: '0.875rem', marginBottom: '0.5rem', color: '#9a3412' }}>
        Operations Guide
      </h3>
      <ul style={{ marginLeft: '1rem', fontSize: '0.8125rem', lineHeight: '1.4', color: '#9a3412', marginBottom: '0' }}>
        <li><strong>Fulfill:</strong> Complete orders with available modules</li>
        <li><strong>Production:</strong> Create order if modules insufficient</li>
        <li><strong>Priority:</strong> Select urgency level for production</li>
      </ul>
    </div>
  );

  // Render Activity Log using standardized ActivityLog component
  const renderActivity = () => (
    <ActivityLog
      title="Supermarket Activity"
      icon="📢"
      notifications={notifications}
      onClear={clearNotifications}
      maxVisible={50}
      emptyMessage="No recent activity"
    />
  );

  // Render Statistics
  const renderStats = () => <StatisticsGrid stats={statsData} />;

  return (
    <>
      <StandardDashboardLayout
        title="Modules Supermarket"
        subtitle="Module Warehouse Operations | WS-8"
        icon="🏭"
        activityContent={renderActivity()}
        statsContent={renderStats()}
        formContent={renderInfoBox()}
        contentGrid={renderWarehouseOrdersSection()}
        inventoryContent={renderModuleInventory()}
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
