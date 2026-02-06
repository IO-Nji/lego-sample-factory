/**
 * ModulesSupermarketDashboard - WS-8 Modules Supermarket
 * 
 * Warehouse Order Management hub. Handles:
 * - Warehouse orders (from Plant Warehouse)
 * - Module inventory management
 * - Production order triggering (Scenario 3)
 * - Order fulfillment with Final Assembly creation
 */

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { logger } from "../../utils/logger";
import { 
  WarehouseDashboard,
  ActivityPanel,
  InventoryPanel,
  OrdersGrid,
  GridCard,
  CompactCard,
  ListRow,
  DashboardPanel
} from "../../components";
import { useInventoryDisplay } from "../../hooks/useInventoryDisplay";
import { useActivityLog } from "../../hooks/useActivityLog";
import { generateAcronym } from "../../utils/dashboardHelpers";

const WORKSTATION_ID = 8;

const ORDER_ACTIONS = [
  { type: 'CONFIRM', label: 'Confirm', icon: '✓', variant: 'info', showFor: ['PENDING'] },
  { type: 'FULFILL', label: 'Fulfill', icon: '📦', variant: 'success', showFor: ['CONFIRMED'], condition: (order) => order.triggerScenario === 'DIRECT_FULFILLMENT' },
  { type: 'ORDER_PRODUCTION', label: 'Order Production', icon: '🏭', variant: 'warning', showFor: ['CONFIRMED'], condition: (order) => order.triggerScenario === 'PRODUCTION_REQUIRED' },
  { type: 'CANCEL', label: 'Cancel', icon: '✕', variant: 'danger', showFor: ['PENDING', 'CONFIRMED'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'AWAITING_PRODUCTION', label: 'Awaiting Production' },
  { value: 'FULFILLED', label: 'Fulfilled' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' }
];

function ModulesSupermarketDashboard() {
  const { session } = useAuth();
  
  // Local state
  const [warehouseOrders, setWarehouseOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [prioritySelection, setPrioritySelection] = useState({}); // Track orders in priority selection mode
  
  // Inventory hook for modules
  const { 
    inventory, 
    masterdata: modules,
    getItemName: getModuleName,
    getStockLevel,
    fetchInventory 
  } = useInventoryDisplay('MODULE', WORKSTATION_ID);
  
  // Activity log hook - now uses workstation ID for unified formatting
  const { notifications, addNotification, addOrderNotification, clearNotifications } = useActivityLog(session, WORKSTATION_ID);

  // Module-aware display name function
  const getModuleDisplayName = useCallback((itemId, itemType) => {
    if (itemType === 'MODULE') {
      const moduleData = modules.find(m => m.id === itemId);
      if (moduleData?.name) {
        return generateAcronym(moduleData.name);
      }
      return `M${itemId}`;
    }
    return generateAcronym(itemType);
  }, [modules]);

  // Fetch warehouse orders
  const fetchWarehouseOrders = useCallback(async () => {
    try {
      const workstationId = session?.user?.workstationId || WORKSTATION_ID;
      const response = await api.get(`/warehouse-orders/workstation/${workstationId}`);
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
        setError(`Failed to load warehouse orders: ${err.response?.data?.message || err.message}`);
      }
    }
  }, [session?.user?.workstationId]);

  // Initial load and refresh
  useEffect(() => {
    fetchWarehouseOrders();
    fetchInventory();
    
    const interval = setInterval(() => {
      fetchWarehouseOrders();
      fetchInventory();
    }, 30000);
    
    return () => clearInterval(interval);
  }, [fetchWarehouseOrders, fetchInventory]);

  // Order action handlers
  const handleConfirm = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      const response = await api.put(`/warehouse-orders/${orderId}/confirm`);
      const confirmedOrder = response.data;
      addOrderNotification('confirmed', confirmedOrder.orderNumber, 'success');
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      setError(`Failed to confirm order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleFulfill = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      const response = await api.put(`/warehouse-orders/${orderId}/fulfill-modules`);
      const orderNum = response.data.orderNumber || `WO-${orderId}`;
      addOrderNotification('fulfilled', orderNum, 'success');
      await fetchWarehouseOrders();
      await fetchInventory();
    } catch (err) {
      setError(`Failed to fulfill order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleOrderProduction = (orderId) => {
    // Show priority selection UI
    setPrioritySelection(prev => ({ ...prev, [orderId]: true }));
  };

  const handleSelectPriority = async (orderId, priority) => {
    const order = warehouseOrders.find(o => o.id === orderId);
    if (!order) return;
    
    setProcessingOrderId(orderId);
    try {
      const orderNum = order.orderNumber || `WO-${order.id}`;
      const workstationId = session?.user?.workstationId;
      
      const payload = {
        sourceWarehouseOrderId: orderId,
        priority: priority,
        notes: `Production order for warehouse order ${orderNum}`,
        createdByWorkstationId: workstationId,
        triggerScenario: 'SCENARIO_3'
      };

      await api.post('/production-orders/create', payload);
      addOrderNotification('sent to production', orderNum, 'success');
      
      setPrioritySelection(prev => ({ ...prev, [orderId]: false }));
      await fetchWarehouseOrders();
    } catch (err) {
      setError(`Failed to create production order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this warehouse order?")) return;
    setProcessingOrderId(orderId);
    try {
      await api.patch(`/warehouse-orders/${orderId}/status?status=CANCELLED`);
      const order = warehouseOrders.find(o => o.id === orderId);
      addOrderNotification('cancelled', order?.orderNumber || `WO-${orderId}`, 'warning');
      await fetchWarehouseOrders();
    } catch (err) {
      setError(`Failed to cancel order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Unified action handler
  const handleAction = useCallback((actionType, orderId) => {
    switch (actionType) {
      case 'CONFIRM': handleConfirm(orderId); break;
      case 'FULFILL': handleFulfill(orderId); break;
      case 'ORDER_PRODUCTION': handleOrderProduction(orderId); break;
      case 'CANCEL': handleCancel(orderId); break;
      default: logger.warn('ModulesSupermarket', 'Unknown action:', actionType);
    }
  }, []);

  // Render helpers
  const renderOrderItems = (order) => {
    if (!order.orderItems || order.orderItems.length === 0) {
      return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>No items</span>;
    }
    return order.orderItems.slice(0, 3).map((item, idx) => (
      <div key={idx} style={{ marginBottom: '0.25rem' }}>
        <strong>{getModuleDisplayName(item.itemId, item.itemType)}</strong>
        <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{item.quantity}</span>
      </div>
    ));
  };

  const renderOrderExtra = (order) => (
    <>
      {order.triggerScenario && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: order.triggerScenario === 'DIRECT_FULFILLMENT' ? 'rgba(16, 185, 129, 0.1)' :
                     order.triggerScenario === 'PRODUCTION_REQUIRED' ? 'rgba(245, 158, 11, 0.1)' :
                     'rgba(59, 130, 246, 0.1)',
          color: order.triggerScenario === 'DIRECT_FULFILLMENT' ? '#059669' :
                 order.triggerScenario === 'PRODUCTION_REQUIRED' ? '#d97706' :
                 '#2563eb',
          fontWeight: 600
        }}>
          {order.triggerScenario.replace(/_/g, ' ')}
        </span>
      )}
      {order.productionOrderId && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: 'rgba(139, 92, 246, 0.1)',
          color: '#7c3aed',
          fontWeight: 600
        }}>
          PO-{order.productionOrderId}
        </span>
      )}
      {/* Priority selection UI */}
      {prioritySelection[order.id] && (
        <div style={{ display: 'flex', gap: '0.25rem', marginTop: '0.5rem' }}>
          {['LOW', 'NORMAL', 'HIGH', 'URGENT'].map(priority => (
            <button
              key={priority}
              onClick={(e) => { e.stopPropagation(); handleSelectPriority(order.id, priority); }}
              style={{
                padding: '0.125rem 0.375rem',
                fontSize: '0.5625rem',
                border: 'none',
                borderRadius: '3px',
                background: priority === 'URGENT' ? 'rgba(239, 68, 68, 0.1)' :
                           priority === 'HIGH' ? 'rgba(245, 158, 11, 0.1)' :
                           priority === 'NORMAL' ? 'rgba(59, 130, 246, 0.1)' :
                           'rgba(100, 116, 139, 0.1)',
                color: priority === 'URGENT' ? '#dc2626' :
                       priority === 'HIGH' ? '#d97706' :
                       priority === 'NORMAL' ? '#2563eb' :
                       '#64748b',
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              {priority}
            </button>
          ))}
        </div>
      )}
    </>
  );

  // Refresh handler (defined before panelRowContent to avoid reference error)
  const handleRefresh = async () => {
    await fetchInventory();
    await fetchWarehouseOrders();
  };

  // Calculate stats for panels
  const pendingCount = warehouseOrders.filter(o => o.status === 'PENDING').length;
  const confirmedCount = warehouseOrders.filter(o => o.status === 'CONFIRMED').length;
  const awaitingProd = warehouseOrders.filter(o => o.status === 'AWAITING_PRODUCTION').length;
  const fulfilledCount = warehouseOrders.filter(o => o.status === 'FULFILLED').length;
  const moduleStock = inventory.filter(i => i.itemType === 'MODULE');
  const totalModules = moduleStock.reduce((sum, i) => sum + (i.quantity || 0), 0);
  const lowStockModules = moduleStock.filter(i => (i.quantity || 0) < 10).length;
  const directFulfillable = warehouseOrders.filter(o => o.triggerScenario === 'DIRECT_FULFILLMENT').length;
  const needsProduction = warehouseOrders.filter(o => o.triggerScenario === 'PRODUCTION_REQUIRED').length;

  const panelRowContent = (
    <>
      {/* Warehouse Orders Panel */}
      <DashboardPanel
        icon="📋"
        title="Warehouse Orders"
        badge={warehouseOrders.length > 0 ? `${warehouseOrders.length}` : null}
        badgeVariant="info"
        themeClass="ws-theme-8"
        stats={[
          { label: 'Pending', value: pendingCount, variant: 'pending' },
          { label: 'Confirmed', value: confirmedCount, variant: 'info' },
          { label: 'Awaiting', value: awaitingProd, variant: 'warning' },
          { label: 'Fulfilled', value: fulfilledCount, variant: 'completed' },
        ]}
        alerts={awaitingProd > 0 ? [{ message: `${awaitingProd} awaiting production`, variant: 'warning' }] : []}
      />

      {/* Module Stock Panel */}
      <DashboardPanel
        icon="🔧"
        title="Module Stock"
        badge={lowStockModules > 0 ? `${lowStockModules} low` : 'OK'}
        badgeVariant={lowStockModules > 0 ? 'warning' : 'success'}
        themeClass="ws-theme-8"
        stats={[
          { label: 'Types', value: moduleStock.length, variant: 'total' },
          { label: 'Total Qty', value: totalModules, variant: 'info' },
          { label: 'Low Stock', value: lowStockModules, variant: lowStockModules > 0 ? 'warning' : 'success' },
          { label: 'Avg/Type', value: moduleStock.length > 0 ? Math.round(totalModules / moduleStock.length) : 0, variant: 'total' },
        ]}
      />

      {/* Production Status Panel */}
      <DashboardPanel
        icon="🏭"
        title="Production Status"
        badge={needsProduction > 0 ? 'Active' : 'Idle'}
        badgeVariant={needsProduction > 0 ? 'progress' : 'success'}
        themeClass="ws-theme-8"
        stats={[
          { label: 'Direct', value: directFulfillable, variant: 'completed' },
          { label: 'Need Prod', value: needsProduction, variant: 'warning' },
          { label: 'In Prod', value: awaitingProd, variant: 'progress' },
          { label: 'From WS-4/5', value: fulfilledCount, variant: 'success' },
        ]}
        subStats={[
          { label: 'Scenario', value: needsProduction > 0 ? '3: Production' : '2: Direct' },
        ]}
      />

      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        themeClass="ws-theme-8"
        actions={[
          { label: 'Confirm', icon: '✓', variant: 'info', onClick: () => {
            const nextOrder = warehouseOrders.find(o => o.status === 'PENDING');
            if (nextOrder) handleConfirm(nextOrder.id);
          }},
          { label: 'Fulfill', icon: '📦', variant: 'success', onClick: () => {
            const fulfillable = warehouseOrders.find(o => o.triggerScenario === 'DIRECT_FULFILLMENT');
            if (fulfillable) handleFulfill(fulfillable.id);
          }},
          { label: 'Production', icon: '🏭', variant: 'warning', onClick: () => {
            const prodNeeded = warehouseOrders.find(o => o.triggerScenario === 'PRODUCTION_REQUIRED');
            if (prodNeeded) handleOrderProduction(prodNeeded.id, 'NORMAL');
          }},
          { label: 'Refresh', icon: '🔄', variant: 'secondary', onClick: handleRefresh },
        ]}
      />
    </>
  );

  // Content sections
  const ordersContent = (
    <OrdersGrid
      title="Warehouse Orders"
      icon="📋"
      orders={warehouseOrders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search WO-XXX..."
      emptyMessage="No warehouse orders"
      emptySubtext="Orders will appear when created from Plant Warehouse"
      renderCard={(order) => (
        <GridCard
          order={order}
          onAction={handleAction}
          actions={ORDER_ACTIONS}
          renderItems={renderOrderItems}
          renderExtra={renderOrderExtra}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderCompactCard={(order) => (
        <CompactCard
          order={order}
          onAction={handleAction}
          actions={ORDER_ACTIONS}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderListItem={(order) => (
        <ListRow
          order={order}
          onAction={handleAction}
          actions={ORDER_ACTIONS}
          columns={['orderNumber', 'status', 'date', 'items']}
          isProcessing={processingOrderId === order.id}
        />
      )}
    />
  );

  const inventoryContent = (
    <InventoryPanel
      title="Module Stock"
      inventory={inventory.filter(i => i.itemType === 'MODULE')}
      getItemName={getModuleName}
      lowStockThreshold={10}
      criticalStockThreshold={5}
    />
  );

  const activityContent = (
    <ActivityPanel
      title="Activity"
      notifications={notifications}
      onClear={clearNotifications}
      maxItems={5}
      compact={true}
    />
  );

  return (
    <WarehouseDashboard
      workstationId={WORKSTATION_ID}
      title="Modules Supermarket"
      subtitle="WS-8 • Warehouse Orders"
      icon="🏪"
      panelRowContent={panelRowContent}
      ordersContent={ordersContent}
      activityContent={activityContent}
      inventoryContent={inventoryContent}
      loading={loading}
      error={error}
      onRefresh={handleRefresh}
      onDismissError={() => setError(null)}
    />
  );
}

export default ModulesSupermarketDashboard;