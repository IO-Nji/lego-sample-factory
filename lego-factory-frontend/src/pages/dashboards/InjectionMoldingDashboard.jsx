/**
 * InjectionMoldingDashboard - WS-1 Injection Molding Station
 * 
 * Handles injection molding orders for plastic parts production.
 * First step in the manufacturing workflow (WS-1 -> WS-2 -> WS-3).
 * 
 * Uses the new unified WorkstationDashboard layout with:
 * - Glassmorphism styling
 * - 4-column panel row for operational status
 * - Orders grid with view modes
 * - Compact activity sidebar
 */

import { useCallback } from 'react';
import { 
  WorkstationDashboard,
  DashboardPanel,
  ActivityPanel,
  OrdersGrid,
  GridCard,
  CompactCard,
  ListRow
} from "../../components";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";

// Workstation configuration
const WORKSTATION_ID = 1;

// Order actions configuration
const ORDER_ACTIONS = [
  { 
    type: 'START', 
    label: 'Start', 
    icon: '▶', 
    variant: 'primary',
    showFor: ['PENDING', 'CONFIRMED']
  },
  { 
    type: 'COMPLETE', 
    label: 'Complete', 
    icon: '✓', 
    variant: 'success',
    showFor: ['IN_PROGRESS']
  },
  { 
    type: 'HALT', 
    label: 'Halt', 
    icon: '⏸', 
    variant: 'warning',
    showFor: ['IN_PROGRESS']
  },
  { 
    type: 'RESUME', 
    label: 'Resume', 
    icon: '▶', 
    variant: 'primary',
    showFor: ['HALTED']
  }
];

// Filter options for workstation orders
const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'HALTED', label: 'Halted' }
];

// Sort options
const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' }
];

function InjectionMoldingDashboard() {
  const {
    orders,
    loading,
    error,
    processingOrderId,
    notifications,
    statsData,
    handleStartOrder,
    handleCompleteOrder,
    handleHaltOrder,
    handleResumeOrder,
    clearNotifications,
    clearError,
    refreshOrders,
    config,
  } = useWorkstationOrders(WORKSTATION_ID);

  // Handle order actions
  const handleAction = useCallback((actionType, orderId) => {
    switch (actionType) {
      case 'START':
        handleStartOrder(orderId);
        break;
      case 'COMPLETE':
        handleCompleteOrder(orderId);
        break;
      case 'HALT':
        handleHaltOrder(orderId, 'Operator initiated halt');
        break;
      case 'RESUME':
        handleResumeOrder(orderId);
        break;
      default:
        console.warn('Unknown action:', actionType);
    }
  }, [handleStartOrder, handleCompleteOrder, handleHaltOrder, handleResumeOrder]);

  // Render order items (parts being molded)
  const renderOrderItems = (order) => {
    if (!order.items || order.items.length === 0) {
      return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>No items specified</span>;
    }
    return order.items.map((item, idx) => (
      <div key={idx} style={{ marginBottom: '0.25rem' }}>
        <strong>{item.itemName || `Part #${item.itemId}`}</strong>
        {item.quantity && <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{item.quantity}</span>}
      </div>
    ));
  };

  // Render extra info (notes, quantity)
  const renderOrderExtra = (order) => (
    <>
      {order.quantity && (
        <span style={{ 
          background: 'rgba(59, 130, 246, 0.1)', 
          padding: '0.125rem 0.375rem', 
          borderRadius: '4px',
          fontWeight: 600,
          color: '#2563eb'
        }}>
          Qty: {order.quantity}
        </span>
      )}
      {order.notes && (
        <span title={order.notes} style={{ color: '#94a3b8' }}>
          📝 {order.notes.substring(0, 30)}{order.notes.length > 30 ? '...' : ''}
        </span>
      )}
    </>
  );

  // Calculate stats from orders
  const pendingCount = orders.filter(o => o.status === 'PENDING' || o.status === 'CONFIRMED').length;
  const inProgressCount = orders.filter(o => o.status === 'IN_PROGRESS').length;
  const completedCount = orders.filter(o => o.status === 'COMPLETED').length;
  const haltedCount = orders.filter(o => o.status === 'HALTED').length;
  const completionRate = orders.length > 0 ? Math.round((completedCount / orders.length) * 100) : 0;

  // Panel row content - 4 operational panels
  const panelRowContent = (
    <>
      {/* Orders Overview Panel */}
      <DashboardPanel
        icon="📋"
        title="Molding Orders"
        badge={orders.length}
        stats={[
          { value: pendingCount, label: 'Pending', variant: 'pending' },
          { value: inProgressCount, label: 'Active', variant: 'progress' },
          { value: completedCount, label: 'Done', variant: 'completed' },
          { value: `${completionRate}%`, label: 'Rate', variant: 'rate' }
        ]}
      />
      
      {/* Machine Status Panel */}
      <DashboardPanel
        icon="⚙️"
        title="Machine Status"
        badge="Online"
        badgeVariant="active"
        stats={[
          { value: '185°C', label: 'Temp', variant: 'info' },
          { value: '120', label: 'PSI', variant: 'info' },
          { value: '45s', label: 'Cycle', variant: 'info' },
          { value: '94%', label: 'Efficiency', variant: 'success' }
        ]}
      />
      
      {/* Materials Panel */}
      <DashboardPanel
        icon="🧱"
        title="Raw Materials"
        stats={[
          { value: '850kg', label: 'Plastic', variant: 'success' },
          { value: '12L', label: 'Colorant', variant: 'success' },
          { value: '4', label: 'Molds', variant: 'info' },
          { value: '2', label: 'Low', variant: 'warning' }
        ]}
        alerts={[
          { type: 'warning', message: 'Blue colorant running low' }
        ]}
      />
      
      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        variant="actions"
        actions={[
          { type: 'START_BATCH', label: 'Start Batch', icon: '▶️', primary: true },
          { type: 'PAUSE', label: 'Pause', icon: '⏸️' },
          { type: 'QUALITY_CHECK', label: 'QC Check', icon: '✓' },
          { type: 'MAINTENANCE', label: 'Maintenance', icon: '🔧' }
        ]}
        onAction={(actionType) => console.log('Action:', actionType)}
      />
    </>
  );

  // Orders grid content
  const ordersContent = (
    <OrdersGrid
      title={config?.ordersTitle || 'Injection Molding Orders'}
      icon={config?.icon || '🔧'}
      orders={orders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search orders..."
      emptyMessage="No molding orders"
      emptySubtext="Orders will appear when assigned to this station"
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
          columns={['orderNumber', 'status', 'date', 'priority']}
          isProcessing={processingOrderId === order.id}
        />
      )}
    />
  );

  // Activity panel content
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
    <WorkstationDashboard
      workstationId={WORKSTATION_ID}
      title={config?.title || 'Injection Molding'}
      subtitle={config?.subtitle || 'WS-1 • Manufacturing'}
      icon={config?.icon || '🔧'}
      panelRowContent={panelRowContent}
      ordersContent={ordersContent}
      activityContent={activityContent}
      loading={loading}
      error={error}
      onRefresh={refreshOrders}
      onDismissError={clearError}
    />
  );
}

export default InjectionMoldingDashboard;
