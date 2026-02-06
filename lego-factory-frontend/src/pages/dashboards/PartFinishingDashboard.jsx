/**
 * PartFinishingDashboard - WS-3 Part Finishing Station
 * 
 * Handles finishing operations for manufactured parts.
 * Final step in the manufacturing workflow (WS-1 -> WS-2 -> WS-3).
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

const WORKSTATION_ID = 3;

const ORDER_ACTIONS = [
  { type: 'START', label: 'Start', icon: '▶', variant: 'primary', showFor: ['PENDING', 'CONFIRMED'] },
  { type: 'COMPLETE', label: 'Complete', icon: '✓', variant: 'success', showFor: ['IN_PROGRESS'] },
  { type: 'HALT', label: 'Halt', icon: '⏸', variant: 'warning', showFor: ['IN_PROGRESS'] },
  { type: 'RESUME', label: 'Resume', icon: '▶', variant: 'primary', showFor: ['HALTED'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'HALTED', label: 'Halted' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' }
];

function PartFinishingDashboard() {
  const {
    orders, loading, error, processingOrderId, notifications, statsData,
    handleStartOrder, handleCompleteOrder, handleHaltOrder, handleResumeOrder,
    clearNotifications, clearError, refreshOrders, config,
  } = useWorkstationOrders(WORKSTATION_ID);

  const handleAction = useCallback((actionType, orderId) => {
    switch (actionType) {
      case 'START': handleStartOrder(orderId); break;
      case 'COMPLETE': handleCompleteOrder(orderId); break;
      case 'HALT': handleHaltOrder(orderId, 'Operator initiated halt'); break;
      case 'RESUME': handleResumeOrder(orderId); break;
      default: console.warn('Unknown action:', actionType);
    }
  }, [handleStartOrder, handleCompleteOrder, handleHaltOrder, handleResumeOrder]);

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

  const renderOrderExtra = (order) => (
    <>
      {order.quantity && (
        <span style={{ background: 'rgba(6, 182, 212, 0.1)', padding: '0.125rem 0.375rem', borderRadius: '4px', fontWeight: 600, color: '#0891b2' }}>
          Qty: {order.quantity}
        </span>
      )}
      {order.notes && <span title={order.notes} style={{ color: '#94a3b8' }}>📝 {order.notes.substring(0, 30)}{order.notes.length > 30 ? '...' : ''}</span>}
    </>
  );

  // Calculate stats from orders
  const pendingCount = orders.filter(o => o.status === 'PENDING' || o.status === 'CONFIRMED').length;
  const inProgressCount = orders.filter(o => o.status === 'IN_PROGRESS').length;
  const completedCount = orders.filter(o => o.status === 'COMPLETED').length;
  const haltedCount = orders.filter(o => o.status === 'HALTED').length;
  const completionRate = orders.length > 0 ? Math.round((completedCount / orders.length) * 100) : 0;
  const yieldRate = completedCount > 0 ? Math.round(((completedCount - 2) / completedCount) * 100) : 100;

  // Panel row content - 4 operational panels
  const panelRowContent = (
    <>
      {/* Finishing Orders Panel */}
      <DashboardPanel
        icon="📋"
        title="Finishing Orders"
        badge={orders.length}
        stats={[
          { value: pendingCount, label: 'Pending', variant: 'pending' },
          { value: inProgressCount, label: 'Active', variant: 'progress' },
          { value: completedCount, label: 'Done', variant: 'completed' },
          { value: `${completionRate}%`, label: 'Rate', variant: 'rate' }
        ]}
      />
      
      {/* Quality Control Panel */}
      <DashboardPanel
        icon="✓"
        title="Quality Control"
        badge={`${yieldRate}% Yield`}
        badgeVariant={yieldRate >= 95 ? 'success' : yieldRate >= 90 ? 'warning' : 'danger'}
        stats={[
          { value: '156', label: 'Passed', variant: 'success' },
          { value: '8', label: 'Failed', variant: 'danger' },
          { value: '3', label: 'Rework', variant: 'warning' },
          { value: `${yieldRate}%`, label: 'Yield', variant: 'success' }
        ]}
      />
      
      {/* Throughput Panel */}
      <DashboardPanel
        icon="📊"
        title="Throughput"
        stats={[
          { value: '42', label: 'Hourly', variant: 'info' },
          { value: '336', label: 'Daily', variant: 'info' },
          { value: '400', label: 'Target', variant: 'primary' },
          { value: '-16%', label: 'Variance', variant: 'warning' }
        ]}
      />
      
      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        variant="actions"
        actions={[
          { type: 'COMPLETE_BATCH', label: 'Complete', icon: '✅', primary: true },
          { type: 'QUALITY_HOLD', label: 'QC Hold', icon: '⏸️' },
          { type: 'SHIP_WS9', label: 'Ship to WS-9', icon: '📤' },
          { type: 'REPORT', label: 'Report Issue', icon: '⚠️' }
        ]}
        onAction={(actionType) => console.log('Action:', actionType)}
      />
    </>
  );

  const ordersContent = (
    <OrdersGrid
      title={config?.ordersTitle || 'Part Finishing Orders'}
      icon={config?.icon || '✨'}
      orders={orders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search orders..."
      emptyMessage="No finishing orders"
      emptySubtext="Orders will appear when assigned to this station"
      renderCard={(order) => <GridCard order={order} onAction={handleAction} actions={ORDER_ACTIONS} renderItems={renderOrderItems} renderExtra={renderOrderExtra} isProcessing={processingOrderId === order.id} />}
      renderCompactCard={(order) => <CompactCard order={order} onAction={handleAction} actions={ORDER_ACTIONS} isProcessing={processingOrderId === order.id} />}
      renderListItem={(order) => <ListRow order={order} onAction={handleAction} actions={ORDER_ACTIONS} columns={['orderNumber', 'status', 'date', 'priority']} isProcessing={processingOrderId === order.id} />}
    />
  );

  const activityContent = <ActivityPanel title="Activity" notifications={notifications} onClear={clearNotifications} maxItems={5} compact={true} />;

  return (
    <WorkstationDashboard
      workstationId={WORKSTATION_ID}
      title={config?.title || 'Part Finishing'}
      subtitle={config?.subtitle || 'WS-3 • Manufacturing'}
      icon={config?.icon || '✨'}
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

export default PartFinishingDashboard;
