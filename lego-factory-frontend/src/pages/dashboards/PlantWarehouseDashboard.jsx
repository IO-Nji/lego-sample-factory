/**
 * PlantWarehouseDashboard - WS-7 Plant Warehouse
 * 
 * Customer Order Management hub. Handles:
 * - Order creation for products
 * - Scenario routing (1: Direct, 2: Warehouse, 3: Production, 4: High-volume)
 * - Product inventory display
 * - Order fulfillment and completion
 */

import { useState, useEffect, useCallback } from 'react';
import { useAuth } from "../../context/AuthContext";
import { useDashboardRefresh } from "../../context/DashboardRefreshContext";
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
  DashboardPanel,
  QuickOrderPanel
} from "../../components";
import { useInventoryDisplay } from "../../hooks/useInventoryDisplay";
import { useActivityLog } from "../../hooks/useActivityLog";

const WORKSTATION_ID = 7;

// Button actions for customer orders - only ONE scenario button shows based on triggerScenario
const ORDER_ACTIONS = [
  { type: 'CONFIRM', label: 'Confirm', icon: '✓', variant: 'info', showFor: ['PENDING'] },
  { type: 'FULFILL', label: 'Fulfill', icon: '📦', variant: 'success', showFor: ['CONFIRMED'], condition: (order) => order?.triggerScenario === 'DIRECT_FULFILLMENT' },
  { type: 'PROCESS', label: 'Process', icon: '⚙️', variant: 'primary', showFor: ['CONFIRMED'], condition: (order) => order?.triggerScenario === 'WAREHOUSE_ORDER_NEEDED' },
  { type: 'ORDER_PRODUCTION', label: 'Order Production', icon: '🏭', variant: 'warning', showFor: ['CONFIRMED'], condition: (order) => order?.triggerScenario === 'DIRECT_PRODUCTION' },
  { type: 'COMPLETE', label: 'Complete', icon: '✅', variant: 'success', showFor: ['PROCESSING'], condition: (order) => order?.canComplete === true },
  { type: 'CANCEL', label: 'Cancel', icon: '✕', variant: 'danger', showFor: ['PENDING'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' }
];

function PlantWarehouseDashboard() {
  const { session } = useAuth();
  const { refresh } = useDashboardRefresh();
  
  // Local state
  const [orders, setOrders] = useState([]);
  const [orderCanComplete, setOrderCanComplete] = useState({});
  const [loading, setLoading] = useState(false);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [error, setError] = useState(null);
  
  // Inventory hook for products
  const { 
    inventory, 
    masterdata: products,
    getItemName: getProductName,
    getStockLevel,
    fetchInventory,
    loading: inventoryLoading,
  } = useInventoryDisplay('PRODUCT', WORKSTATION_ID);
  
  // Activity log hook - now uses workstation ID for unified formatting
  const { notifications, addNotification, addOrderNotification, clearNotifications } = useActivityLog(session, WORKSTATION_ID);

  // Check if PROCESSING orders can be completed
  const checkProcessingOrdersCompletion = useCallback(async (ordersList) => {
    const processingOrders = ordersList.filter(o => o.status === 'PROCESSING');
    const canCompleteMap = {};
    
    for (const order of processingOrders) {
      try {
        const response = await api.get(`/customer-orders/${order.id}/can-complete`);
        canCompleteMap[order.id] = response.data === true;
      } catch (err) {
        logger.error('PlantWarehouse', `Failed to check completion status for order ${order.id}`, err);
        canCompleteMap[order.id] = false;
      }
    }
    setOrderCanComplete(canCompleteMap);
  }, []);

  // Fetch orders with current scenario
  const fetchOrders = useCallback(async () => {
    const workstationId = session?.user?.workstationId || WORKSTATION_ID;
    try {
      const response = await api.get(`/customer-orders/workstation/${workstationId}`);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      
      // Dynamically check current trigger scenario for CONFIRMED orders
      const ordersWithCurrentScenario = await Promise.all(
        ordersList.map(async (order) => {
          if (order.status === 'CONFIRMED') {
            try {
              const scenarioResponse = await api.get(`/customer-orders/${order.id}/current-scenario`);
              return { ...order, triggerScenario: scenarioResponse.data.triggerScenario };
            } catch (err) {
              return order;
            }
          }
          return order;
        })
      );
      
      setOrders(ordersWithCurrentScenario);
      await checkProcessingOrdersCompletion(ordersWithCurrentScenario);
    } catch (err) {
      if (err.response?.status !== 404) {
        setError(`Failed to load orders: ${err.response?.data?.message || err.message}`);
      }
    }
  }, [session?.user?.workstationId, checkProcessingOrdersCompletion]);

  // Initial load and refresh interval
  useEffect(() => {
    const initializeData = async () => {
      await fetchInventory();
      await fetchOrders();
    };
    initializeData();

    const interval = setInterval(() => {
      fetchInventory();
      fetchOrders();
    }, 30000);

    return () => clearInterval(interval);
  }, [fetchInventory, fetchOrders]);

  // Handle create order - receives quantities from QuickOrderPanel
  const handleCreateOrder = async (quantities) => {
    const workstationId = session?.user?.workstationId || WORKSTATION_ID;
    const orderItems = Object.entries(quantities)
      .filter(([_, quantity]) => quantity > 0)
      .map(([productId, quantity]) => ({
        itemType: "PRODUCT",
        itemId: parseInt(productId, 10),
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
        workstationId,
        notes: "Plant warehouse order",
      });

      const orderNum = response.data.orderNumber;
      addOrderNotification('created', orderNum, 'success');
      await fetchInventory();
      await fetchOrders();
      refresh();
    } catch (err) {
      setError(`Failed to create order: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Order action handlers
  const handleConfirm = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.put(`/customer-orders/${orderId}/confirm`);
      await fetchOrders();
    } catch (err) {
      setError(`Failed to confirm order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleFulfill = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      const response = await api.put(`/customer-orders/${orderId}/fulfill`);
      const orderNum = response.data.orderNumber;
      const isCompleted = response.data.status === 'COMPLETED';
      addOrderNotification(isCompleted ? 'fulfilled' : 'processing', orderNum, 'success');
      await fetchOrders();
      await fetchInventory();
    } catch (err) {
      setError(`Failed to fulfill order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleOrderProduction = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post('/production-orders/from-customer-order', {
        customerOrderId: orderId,
        priority: 'NORMAL',
        notes: 'Scenario 4: Direct production',
        createdByWorkstationId: session?.user?.workstationId || WORKSTATION_ID
      });
      
      const order = orders.find(o => o.id === orderId);
      addOrderNotification('sent to production', order?.orderNumber || `ORD-${orderId}`, 'success');
      await fetchOrders();
    } catch (err) {
      setError(`Failed to create production order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleComplete = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      await api.post(`/customer-orders/${orderId}/complete`);
      const order = orders.find(o => o.id === orderId);
      addOrderNotification('completed', order?.orderNumber || `ORD-${orderId}`, 'success');
      await fetchOrders();
      await fetchInventory();
    } catch (err) {
      setError(`Failed to complete order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCancel = async (orderId) => {
    if (!globalThis.confirm("Cancel this order?")) return;
    setProcessingOrderId(orderId);
    try {
      await api.post(`/customer-orders/${orderId}/cancel`);
      const order = orders.find(o => o.id === orderId);
      addOrderNotification('cancelled', order?.orderNumber || `ORD-${orderId}`, 'warning');
      await fetchOrders();
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
      case 'PROCESS': handleFulfill(orderId); break;
      case 'ORDER_PRODUCTION': handleOrderProduction(orderId); break;
      case 'COMPLETE': handleComplete(orderId); break;
      case 'CANCEL': handleCancel(orderId); break;
      default: logger.warn('PlantWarehouse', 'Unknown action:', actionType);
    }
  }, []);

  // Render helpers
  const renderOrderItems = (order) => {
    if (!order.orderItems || order.orderItems.length === 0) {
      return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>No items</span>;
    }
    return order.orderItems.slice(0, 3).map((item, idx) => (
      <div key={idx} style={{ marginBottom: '0.25rem' }}>
        <strong>{getProductName(item.itemId, item.itemType) || `Product #${item.itemId}`}</strong>
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
                     order.triggerScenario === 'DIRECT_PRODUCTION' ? 'rgba(245, 158, 11, 0.1)' :
                     'rgba(59, 130, 246, 0.1)',
          color: order.triggerScenario === 'DIRECT_FULFILLMENT' ? '#059669' :
                 order.triggerScenario === 'DIRECT_PRODUCTION' ? '#d97706' :
                 '#2563eb',
          fontWeight: 600
        }}>
          {order.triggerScenario.replace(/_/g, ' ')}
        </span>
      )}
    </>
  );

  // Enrich orders with canComplete
  const enrichedOrders = orders.map(order => ({
    ...order,
    canComplete: orderCanComplete[order.id] === true
  }));

  // Refresh handler (defined before panelRowContent to avoid reference error)
  const handleRefresh = async () => {
    await fetchInventory();
    await fetchOrders();
  };

  // Calculate stats for panels
  const pendingCount = orders.filter(o => o.status === 'PENDING').length;
  const confirmedCount = orders.filter(o => o.status === 'CONFIRMED').length;
  const processingCount = orders.filter(o => o.status === 'PROCESSING').length;
  const completedCount = orders.filter(o => o.status === 'COMPLETED').length;
  const productStock = inventory.filter(i => i.itemType === 'PRODUCT');
  const totalStock = productStock.reduce((sum, i) => sum + (i.quantity || 0), 0);
  const lowStockItems = productStock.filter(i => (i.quantity || 0) < 10).length;
  const readyToComplete = Object.values(orderCanComplete).filter(Boolean).length;

  const panelRowContent = (
    <>
      {/* Customer Orders Panel */}
      <DashboardPanel
        icon="📋"
        title="Customer Orders"
        badge={orders.length > 0 ? `${orders.length}` : null}
        badgeVariant="info"
        themeClass="ws-theme-7"
        stats={[
          { label: 'Pending', value: pendingCount, variant: 'pending' },
          { label: 'Confirmed', value: confirmedCount, variant: 'info' },
          { label: 'Processing', value: processingCount, variant: 'progress' },
          { label: 'Done', value: completedCount, variant: 'completed' },
        ]}
        alerts={readyToComplete > 0 ? [{ message: `${readyToComplete} ready to complete`, variant: 'success' }] : []}
      />

      {/* Product Stock Panel */}
      <DashboardPanel
        icon="📦"
        title="Product Stock"
        badge={lowStockItems > 0 ? `${lowStockItems} low` : 'OK'}
        badgeVariant={lowStockItems > 0 ? 'warning' : 'success'}
        themeClass="ws-theme-7"
        stats={[
          { label: 'Products', value: productStock.length, variant: 'total' },
          { label: 'Total Qty', value: totalStock, variant: 'info' },
          { label: 'Low Stock', value: lowStockItems, variant: lowStockItems > 0 ? 'warning' : 'success' },
          { label: 'Avg/Item', value: productStock.length > 0 ? Math.round(totalStock / productStock.length) : 0, variant: 'total' },
        ]}
      />

      {/* Fulfillment Status Panel */}
      <DashboardPanel
        icon="🚚"
        title="Fulfillment"
        badge="Today"
        badgeVariant="info"
        themeClass="ws-theme-7"
        stats={[
          { label: 'Direct', value: orders.filter(o => o.triggerScenario === 'DIRECT_FULFILLMENT').length, variant: 'completed' },
          { label: 'Via WS-8', value: orders.filter(o => o.triggerScenario === 'WAREHOUSE_ORDER_NEEDED').length, variant: 'info' },
          { label: 'Production', value: orders.filter(o => o.triggerScenario === 'DIRECT_PRODUCTION').length, variant: 'warning' },
          { label: 'Delivered', value: orders.filter(o => o.status === 'DELIVERED').length, variant: 'success' },
        ]}
        subStats={[
          { label: 'Scenarios active', value: '1, 2, 3, 4' },
        ]}
      />

      {/* Quick Order Panel - Replaces Quick Actions */}
      <QuickOrderPanel
        icon="➕"
        title="New Order"
        items={products}
        onSubmit={handleCreateOrder}
        loading={loading}
        themeClass="ws-theme-7"
        getItemName={(item) => item.name || `Product #${item.id}`}
        getItemStock={(itemId) => getStockLevel(itemId)}
        maxDisplayItems={4}
        submitLabel="Create"
        itemLabel="Product"
      />
    </>
  );

  // Content sections
  const ordersContent = (
    <OrdersGrid
      title="Customer Orders"
      icon="📋"
      orders={enrichedOrders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search CUST-XXX..."
      emptyMessage="No customer orders"
      emptySubtext="Create an order using the form"
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
      title="Product Stock"
      inventory={inventory.filter(i => i.itemType === 'PRODUCT')}
      getItemName={getProductName}
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
      title="Plant Warehouse"
      subtitle="WS-7 • Customer Orders"
      icon="🏢"
      panelRowContent={panelRowContent}
      ordersContent={ordersContent}
      activityContent={activityContent}
      inventoryContent={inventoryContent}
      loading={inventoryLoading}
      error={error}
      onRefresh={handleRefresh}
      onDismissError={() => setError(null)}
    />
  );
}

export default PlantWarehouseDashboard;