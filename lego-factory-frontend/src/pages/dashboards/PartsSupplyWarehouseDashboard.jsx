/**
 * PartsSupplyWarehouseDashboard - WS-9 Parts Supply Warehouse
 * 
 * Supply Order Management hub. Handles:
 * - Supply orders (parts distribution)
 * - Parts inventory management
 * - Stock adjustments
 * - Supply fulfillment for production and assembly
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

const WORKSTATION_ID = 9;

const ORDER_ACTIONS = [
  { type: 'VIEW', label: 'View', icon: '👁️', variant: 'secondary', showFor: ['PENDING', 'FULFILLED', 'REJECTED'] },
  { type: 'FULFILL', label: 'Fulfill', icon: '📦', variant: 'success', showFor: ['PENDING'] },
  { type: 'REJECT', label: 'Reject', icon: '✕', variant: 'danger', showFor: ['PENDING'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'FULFILLED', label: 'Fulfilled' },
  { value: 'REJECTED', label: 'Rejected' }
];

const TYPE_FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Types' },
  { value: 'PRODUCTION', label: 'Production' },
  { value: 'ASSEMBLY', label: 'Assembly' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'sourceType', label: 'Source Type' }
];

function PartsSupplyWarehouseDashboard() {
  const { session } = useAuth();
  
  // Local state
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [error, setError] = useState(null);
  const [filterType, setFilterType] = useState('ALL');
  
  // Stock adjustment modal state
  const [showStockAdjust, setShowStockAdjust] = useState(false);
  const [selectedPart, setSelectedPart] = useState(null);
  const [adjustAmount, setAdjustAmount] = useState(0);
  const [adjustReason, setAdjustReason] = useState('');
  
  // View supply order modal state
  const [showViewOrder, setShowViewOrder] = useState(false);
  const [viewingOrder, setViewingOrder] = useState(null);
  
  // Inventory hook for parts
  const { 
    inventory: partsStock, 
    getItemName,
    fetchInventory: fetchPartsStock 
  } = useInventoryDisplay('PART', WORKSTATION_ID);

  // Activity log hook - unified formatting
  const { notifications, addNotification, addOrderNotification, clearNotifications } = useActivityLog(session, WORKSTATION_ID);

  // Fetch supply orders
  const fetchSupplyOrders = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get("/supply-orders/warehouse");
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setSupplyOrders(ordersList);
      setError(null);
    } catch (err) {
      setError(`Failed to fetch supply orders: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial load and refresh
  useEffect(() => {
    fetchSupplyOrders();
    fetchPartsStock();
    
    const interval = setInterval(() => {
      fetchSupplyOrders();
      fetchPartsStock();
    }, 30000);
    
    return () => clearInterval(interval);
  }, [fetchSupplyOrders, fetchPartsStock]);

  // Order action handlers
  const handleView = (order) => {
    setViewingOrder(order);
    setShowViewOrder(true);
  };

  const handleFulfill = async (orderId) => {
    setProcessingOrderId(orderId);
    try {
      const order = supplyOrders.find(o => o.id === orderId);
      await api.put(`/supply-orders/${orderId}/fulfill`);
      addOrderNotification('fulfilled', order?.orderNumber || `SUP-${orderId}`, 'success');
      await fetchSupplyOrders();
      await fetchPartsStock();
    } catch (err) {
      setError(`Failed to fulfill supply order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleReject = async (orderId) => {
    const reason = globalThis.prompt("Enter rejection reason:");
    if (!reason) return;
    
    setProcessingOrderId(orderId);
    try {
      const order = supplyOrders.find(o => o.id === orderId);
      await api.put(`/supply-orders/${orderId}/reject`, { reason });
      addOrderNotification('rejected', order?.orderNumber || `SUP-${orderId}`, 'warning');
      await fetchSupplyOrders();
    } catch (err) {
      setError(`Failed to reject supply order: ${err.response?.data?.message || err.message}`);
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Stock adjustment handler
  const handleStockAdjust = async () => {
    if (!selectedPart || !adjustReason) {
      setError("Please provide adjustment quantity and reason");
      return;
    }

    try {
      const newQuantity = selectedPart.quantity + adjustAmount;
      await api.post("/stock/update", null, {
        params: {
          workstationId: WORKSTATION_ID,
          itemType: "PART",
          itemId: selectedPart.itemId,
          quantity: newQuantity
        }
      });
      
      const partName = selectedPart.itemName || `Part #${selectedPart.itemId}`;
      addNotification(`Stock adjusted: ${partName} (${adjustAmount > 0 ? '+' : ''}${adjustAmount})`, 'success');
      setShowStockAdjust(false);
      setSelectedPart(null);
      setAdjustAmount(0);
      setAdjustReason('');
      await fetchPartsStock();
    } catch (err) {
      setError(`Failed to adjust stock: ${err.response?.data?.message || err.message}`);
    }
  };

  // Unified action handler
  const handleAction = useCallback((actionType, orderId, order) => {
    switch (actionType) {
      case 'VIEW': handleView(order); break;
      case 'FULFILL': handleFulfill(orderId); break;
      case 'REJECT': handleReject(orderId); break;
      default: logger.warn('PartsSupply', 'Unknown action:', actionType);
    }
  }, []);

  // Filter orders by type
  const filteredOrders = filterType === 'ALL' 
    ? supplyOrders 
    : supplyOrders.filter(o => o.sourceControlOrderType === filterType);

  // Render helpers
  const renderOrderItems = (order) => {
    if (!order.items || order.items.length === 0) {
      return (
        <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>
          {order.partId ? `Part #${order.partId} ×${order.quantity || 1}` : 'No items'}
        </span>
      );
    }
    return order.items.slice(0, 3).map((item, idx) => (
      <div key={idx} style={{ marginBottom: '0.25rem' }}>
        <strong>{getItemName(item.itemId, 'PART') || `Part #${item.itemId}`}</strong>
        <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{item.quantity}</span>
      </div>
    ));
  };

  const renderOrderExtra = (order) => (
    <>
      {order.sourceControlOrderType && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: order.sourceControlOrderType === 'PRODUCTION' ? 'rgba(139, 92, 246, 0.1)' : 'rgba(236, 72, 153, 0.1)',
          color: order.sourceControlOrderType === 'PRODUCTION' ? '#7c3aed' : '#db2777',
          fontWeight: 600
        }}>
          {order.sourceControlOrderType}
        </span>
      )}
      {order.targetWorkstationId && (
        <span style={{
          fontSize: '0.625rem',
          padding: '0.125rem 0.375rem',
          borderRadius: '4px',
          background: 'rgba(59, 130, 246, 0.1)',
          color: '#2563eb',
          fontWeight: 600
        }}>
          → WS-{order.targetWorkstationId}
        </span>
      )}
    </>
  );

  // Stats
  const totalOrders = supplyOrders.length;
  const pendingOrders = supplyOrders.filter(o => o.status === 'PENDING').length;
  const fulfilledOrders = supplyOrders.filter(o => o.status === 'FULFILLED').length;
  const rejectedOrders = supplyOrders.filter(o => o.status === 'REJECTED').length;
  const lowStockParts = partsStock.filter(p => p.quantity < 50).length;
  const totalPartsQty = partsStock.reduce((sum, p) => sum + p.quantity, 0);

  // Refresh handler (defined before panelRowContent to avoid reference error)
  const handleRefresh = async () => {
    await fetchPartsStock();
    await fetchSupplyOrders();
  };

  const panelRowContent = (
    <>
      {/* Supply Orders Panel */}
      <DashboardPanel
        icon="📤"
        title="Supply Orders"
        badge={totalOrders > 0 ? `${totalOrders}` : null}
        badgeVariant="info"
        themeClass="ws-theme-9"
        stats={[
          { label: 'Pending', value: pendingOrders, variant: 'pending' },
          { label: 'Fulfilled', value: fulfilledOrders, variant: 'completed' },
          { label: 'Rejected', value: rejectedOrders, variant: 'danger' },
          { label: 'Rate', value: fulfilledOrders > 0 ? `${Math.round(fulfilledOrders / totalOrders * 100)}%` : '0%', variant: 'success' },
        ]}
        alerts={pendingOrders > 5 ? [{ message: `${pendingOrders} orders waiting`, variant: 'warning' }] : []}
      />

      {/* Parts Stock Panel */}
      <DashboardPanel
        icon="🔩"
        title="Parts Stock"
        badge={lowStockParts > 0 ? `${lowStockParts} low` : 'OK'}
        badgeVariant={lowStockParts > 0 ? 'warning' : 'success'}
        themeClass="ws-theme-9"
        stats={[
          { label: 'Part Types', value: partsStock.length, variant: 'total' },
          { label: 'Total Qty', value: totalPartsQty, variant: 'info' },
          { label: 'Low Stock', value: lowStockParts, variant: lowStockParts > 0 ? 'warning' : 'success' },
          { label: 'Critical', value: partsStock.filter(p => p.quantity < 20).length, variant: 'danger' },
        ]}
      />

      {/* Demand Panel */}
      <DashboardPanel
        icon="📊"
        title="Demand"
        badge="Live"
        badgeVariant="info"
        themeClass="ws-theme-9"
        stats={[
          { label: 'Production', value: supplyOrders.filter(o => o.sourceType === 'PRODUCTION').length, variant: 'info' },
          { label: 'Assembly', value: supplyOrders.filter(o => o.sourceType === 'ASSEMBLY').length, variant: 'info' },
          { label: 'Today', value: fulfilledOrders, variant: 'completed' },
          { label: 'Backlog', value: pendingOrders, variant: pendingOrders > 0 ? 'warning' : 'success' },
        ]}
        subStats={[
          { label: 'Target WS', value: 'WS-1 to WS-6' },
        ]}
      />

      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        themeClass="ws-theme-9"
        actions={[
          { label: 'Fulfill Next', icon: '📦', variant: 'success', onClick: () => {
            const nextOrder = supplyOrders.find(o => o.status === 'PENDING');
            if (nextOrder) handleFulfill(nextOrder.id);
          }},
          { label: 'Fulfill All', icon: '✅', variant: 'primary', onClick: async () => {
            const pending = supplyOrders.filter(o => o.status === 'PENDING');
            for (const order of pending.slice(0, 5)) {
              await handleFulfill(order.id);
            }
          }},
          { label: 'Adjust Stock', icon: '📝', variant: 'secondary', onClick: () => {
            if (partsStock.length > 0) {
              setSelectedPart(partsStock[0]);
              setShowStockAdjust(true);
            }
          }},
          { label: 'Refresh', icon: '🔄', variant: 'secondary', onClick: handleRefresh },
        ]}
      />
    </>
  );

  // Type filter controls (custom header component)
  const typeFilterControls = (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <span style={{ fontSize: '0.6875rem', fontWeight: 600, color: '#64748b' }}>Type:</span>
      <select
        value={filterType}
        onChange={(e) => setFilterType(e.target.value)}
        style={{
          padding: '0.25rem 0.5rem',
          fontSize: '0.75rem',
          border: '1px solid rgba(100, 116, 139, 0.2)',
          borderRadius: '4px',
          background: 'rgba(255, 255, 255, 0.8)',
          color: '#1e3d66',
          cursor: 'pointer'
        }}
      >
        {TYPE_FILTER_OPTIONS.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  );

  // Content sections
  const ordersContent = (
    <OrdersGrid
      title="Supply Orders"
      icon="📤"
      orders={filteredOrders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search SO-XXX..."
      emptyMessage="No supply orders"
      emptySubtext="Supply orders will appear from production/assembly"
      headerExtra={typeFilterControls}
      renderCard={(order) => (
        <GridCard
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          renderItems={renderOrderItems}
          renderExtra={renderOrderExtra}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderCompactCard={(order) => (
        <CompactCard
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          isProcessing={processingOrderId === order.id}
        />
      )}
      renderListItem={(order) => (
        <ListRow
          order={order}
          onAction={(type, id) => handleAction(type, id, order)}
          actions={ORDER_ACTIONS}
          columns={['orderNumber', 'status', 'date', 'type']}
          isProcessing={processingOrderId === order.id}
        />
      )}
    />
  );

  const inventoryContent = (
    <InventoryPanel
      title="Parts Stock"
      inventory={partsStock}
      getItemName={getItemName}
      lowStockThreshold={50}
      criticalStockThreshold={20}
      showActions={true}
      onAdjustStock={(part) => {
        setSelectedPart(part);
        setShowStockAdjust(true);
      }}
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
    <>
      <WarehouseDashboard
        workstationId={WORKSTATION_ID}
        title="Parts Supply"
        subtitle="WS-9 • Supply Orders"
        icon="🔩"
        panelRowContent={panelRowContent}
        ordersContent={ordersContent}
        activityContent={activityContent}
        inventoryContent={inventoryContent}
        loading={loading}
        error={error}
        onRefresh={handleRefresh}
        onDismissError={() => setError(null)}
      />
      
      {/* View Supply Order Modal */}
      {showViewOrder && viewingOrder && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '1.5rem',
            width: '90%',
            maxWidth: '500px',
            maxHeight: '80vh',
            overflow: 'auto',
            boxShadow: '0 20px 50px rgba(0, 0, 0, 0.2)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0, fontSize: '1.125rem', color: '#1e3d66' }}>
                📦 {viewingOrder.supplyOrderNumber || `Supply Order #${viewingOrder.id}`}
              </h3>
              <button
                onClick={() => { setShowViewOrder(false); setViewingOrder(null); }}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '1.25rem',
                  cursor: 'pointer',
                  color: '#64748b'
                }}
              >
                ✕
              </button>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
              <div style={{ padding: '0.5rem', background: '#f8fafc', borderRadius: '6px' }}>
                <span style={{ fontSize: '0.625rem', color: '#64748b', display: 'block' }}>Status</span>
                <span style={{
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: viewingOrder.status === 'FULFILLED' ? '#059669' :
                         viewingOrder.status === 'CONFIRMED' ? '#2563eb' :
                         viewingOrder.status === 'REJECTED' ? '#dc2626' : '#d97706'
                }}>
                  {viewingOrder.status}
                </span>
              </div>
              <div style={{ padding: '0.5rem', background: '#f8fafc', borderRadius: '6px' }}>
                <span style={{ fontSize: '0.625rem', color: '#64748b', display: 'block' }}>Source</span>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#1e3d66' }}>
                  {viewingOrder.sourceControlOrderType || 'N/A'}
                </span>
              </div>
              <div style={{ padding: '0.5rem', background: '#f8fafc', borderRadius: '6px' }}>
                <span style={{ fontSize: '0.625rem', color: '#64748b', display: 'block' }}>Target</span>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#1e3d66' }}>
                  WS-{viewingOrder.targetWorkstationId || 'N/A'}
                </span>
              </div>
              <div style={{ padding: '0.5rem', background: '#f8fafc', borderRadius: '6px' }}>
                <span style={{ fontSize: '0.625rem', color: '#64748b', display: 'block' }}>Priority</span>
                <span style={{ fontSize: '0.875rem', fontWeight: 600, color: '#1e3d66' }}>
                  {viewingOrder.priority || 'NORMAL'}
                </span>
              </div>
            </div>
            
            <div style={{ marginBottom: '1rem' }}>
              <h4 style={{ margin: '0 0 0.5rem', fontSize: '0.875rem', color: '#1e3d66' }}>
                🔩 Parts Required ({viewingOrder.supplyOrderItems?.length || viewingOrder.items?.length || 0})
              </h4>
              <div style={{
                background: '#f8fafc',
                borderRadius: '8px',
                border: '1px solid #e2e8f0',
                maxHeight: '200px',
                overflow: 'auto'
              }}>
                {(viewingOrder.supplyOrderItems || viewingOrder.items || []).length === 0 ? (
                  <div style={{ padding: '1rem', textAlign: 'center', color: '#94a3b8' }}>
                    {viewingOrder.partId ? (
                      <div>
                        <strong>{getItemName(viewingOrder.partId, 'PART') || `Part #${viewingOrder.partId}`}</strong>
                        <span style={{ marginLeft: '0.5rem' }}>×{viewingOrder.quantity || 1}</span>
                      </div>
                    ) : (
                      'No parts specified'
                    )}
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8125rem' }}>
                    <thead>
                      <tr style={{ background: '#f1f5f9' }}>
                        <th style={{ padding: '0.5rem', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Part</th>
                        <th style={{ padding: '0.5rem', textAlign: 'right', borderBottom: '1px solid #e2e8f0' }}>Qty</th>
                        <th style={{ padding: '0.5rem', textAlign: 'left', borderBottom: '1px solid #e2e8f0' }}>Unit</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(viewingOrder.supplyOrderItems || viewingOrder.items || []).map((item, idx) => (
                        <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                          <td style={{ padding: '0.5rem' }}>
                            <strong>{getItemName(item.partId || item.itemId, 'PART') || `Part #${item.partId || item.itemId}`}</strong>
                          </td>
                          <td style={{ padding: '0.5rem', textAlign: 'right', fontWeight: 600, color: '#2563eb' }}>
                            {item.quantityRequested || item.quantity || 1}
                          </td>
                          <td style={{ padding: '0.5rem', color: '#64748b' }}>
                            {item.unit || 'pcs'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
            
            {viewingOrder.notes && (
              <div style={{ marginBottom: '1rem' }}>
                <h4 style={{ margin: '0 0 0.25rem', fontSize: '0.75rem', color: '#64748b' }}>Notes</h4>
                <p style={{ margin: 0, fontSize: '0.8125rem', color: '#1e3d66' }}>{viewingOrder.notes}</p>
              </div>
            )}
            
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              {viewingOrder.status === 'PENDING' && (
                <button
                  onClick={() => { handleFulfill(viewingOrder.id); setShowViewOrder(false); setViewingOrder(null); }}
                  style={{
                    padding: '0.5rem 1rem',
                    background: 'linear-gradient(135deg, #10b981, #059669)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '6px',
                    fontSize: '0.875rem',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  📦 Fulfill
                </button>
              )}
              <button
                onClick={() => { setShowViewOrder(false); setViewingOrder(null); }}
                style={{
                  padding: '0.5rem 1rem',
                  background: '#f1f5f9',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  cursor: 'pointer'
                }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
      
      {/* Stock Adjustment Modal */}
      {showStockAdjust && selectedPart && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '1.5rem',
            width: '90%',
            maxWidth: '400px',
            boxShadow: '0 20px 50px rgba(0, 0, 0, 0.2)'
          }}>
            <h3 style={{ margin: '0 0 1rem', fontSize: '1.125rem', color: '#1e3d66' }}>
              Adjust Stock: {selectedPart.itemName || `Part #${selectedPart.itemId}`}
            </h3>
            
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#64748b', marginBottom: '0.25rem' }}>
                Current: {selectedPart.quantity} | New: {selectedPart.quantity + adjustAmount}
              </label>
              <input
                type="number"
                value={adjustAmount}
                onChange={(e) => setAdjustAmount(parseInt(e.target.value, 10) || 0)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  fontSize: '1rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px'
                }}
                placeholder="Adjustment (+/-)"
              />
            </div>
            
            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 600, color: '#64748b', marginBottom: '0.25rem' }}>
                Reason
              </label>
              <input
                type="text"
                value={adjustReason}
                onChange={(e) => setAdjustReason(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.5rem',
                  fontSize: '0.875rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '6px'
                }}
                placeholder="Reason for adjustment"
              />
            </div>
            
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              <button
                onClick={() => { setShowStockAdjust(false); setSelectedPart(null); setAdjustAmount(0); setAdjustReason(''); }}
                style={{
                  padding: '0.5rem 1rem',
                  background: '#f1f5f9',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  cursor: 'pointer'
                }}
              >
                Cancel
              </button>
              <button
                onClick={handleStockAdjust}
                disabled={!adjustReason}
                style={{
                  padding: '0.5rem 1rem',
                  background: adjustReason ? 'linear-gradient(135deg, #84cc16, #65a30d)' : '#d1d5db',
                  color: adjustReason ? 'white' : '#9ca3af',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  cursor: adjustReason ? 'pointer' : 'not-allowed'
                }}
              >
                Apply
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default PartsSupplyWarehouseDashboard;
