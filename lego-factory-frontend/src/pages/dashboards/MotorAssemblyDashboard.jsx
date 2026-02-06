/**
 * MotorAssemblyDashboard - WS-5 Motor Assembly Station
 * 
 * Handles motor assembly orders for module production.
 * Second step in assembly workflow (WS-4 -> WS-5 -> WS-6).
 */

import { useCallback } from 'react';
import { 
  WorkstationDashboard,
  ActivityPanel,
  OrdersGrid,
  GridCard,
  CompactCard,
  ListRow,
  DashboardPanel
} from "../../components";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";

const WORKSTATION_ID = 5;

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

function MotorAssemblyDashboard() {
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
        <strong>{item.itemName || `Motor Part #${item.itemId}`}</strong>
        {item.quantity && <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{item.quantity}</span>}
      </div>
    ));
  };

  const renderOrderExtra = (order) => (
    <>
      {order.quantity && (
        <span style={{ background: 'rgba(34, 197, 94, 0.1)', padding: '0.125rem 0.375rem', borderRadius: '4px', fontWeight: 600, color: '#16a34a' }}>
          Qty: {order.quantity}
        </span>
      )}
      {order.notes && <span title={order.notes} style={{ color: '#94a3b8' }}>📝 {order.notes.substring(0, 30)}{order.notes.length > 30 ? '...' : ''}</span>}
    </>
  );

  // Calculate stats from orders
  const pendingCount = orders.filter(o => ['PENDING', 'CONFIRMED'].includes(o.status)).length;
  const activeCount = orders.filter(o => o.status === 'IN_PROGRESS').length;
  const completedToday = orders.filter(o => o.status === 'COMPLETED').length;
  const partsReady = orders.filter(o => o.supplyStatus === 'FULFILLED').length;

  const panelRowContent = (
    <>
      {/* Motor Orders Panel */}
      <DashboardPanel
        icon="🔧"
        title="Motor Orders"
        badge={orders.length > 0 ? `${orders.length}` : null}
        badgeVariant="info"
        themeClass="ws-theme-5"
        stats={[
          { label: 'Pending', value: pendingCount, variant: 'pending' },
          { label: 'Active', value: activeCount, variant: 'progress' },
          { label: 'Done', value: completedToday, variant: 'completed' },
          { label: 'Rate', value: `${completedToday > 0 ? Math.round(completedToday / (completedToday + activeCount + pendingCount) * 100) : 0}%`, variant: 'total' },
        ]}
        alerts={activeCount > 3 ? [{ message: 'High workload', variant: 'warning' }] : []}
      />

      {/* Components Panel */}
      <DashboardPanel
        icon="🔩"
        title="Components"
        badge={partsReady > 0 ? 'Ready' : 'Staging'}
        badgeVariant={partsReady > 0 ? 'success' : 'warning'}
        themeClass="ws-theme-5"
        stats={[
          { label: 'Motors', value: '24', variant: 'completed' },
          { label: 'Gears', value: '18', variant: 'info' },
          { label: 'Shafts', value: '36', variant: 'total' },
          { label: 'Low', value: '0', variant: 'success' },
        ]}
        subStats={[
          { label: 'From WS-4', value: `${partsReady} orders` },
        ]}
      />

      {/* Test Results Panel */}
      <DashboardPanel
        icon="🧪"
        title="Test Results"
        badge="QC"
        badgeVariant="success"
        themeClass="ws-theme-5"
        stats={[
          { label: 'Tested', value: completedToday, variant: 'total' },
          { label: 'Passed', value: completedToday, variant: 'completed' },
          { label: 'Failed', value: '0', variant: 'danger' },
          { label: 'Pass Rate', value: '100%', variant: 'success' },
        ]}
        alerts={[]}
      />

      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        themeClass="ws-theme-5"
        actions={[
          { label: 'Start Next', icon: '▶️', variant: 'primary', onClick: () => {
            const nextOrder = orders.find(o => o.status === 'CONFIRMED' || o.status === 'PENDING');
            if (nextOrder) handleAction('START', nextOrder.id);
          }},
          { label: 'Run Test', icon: '🧪', variant: 'secondary', onClick: () => console.log('Run test') },
          { label: 'To WS-8', icon: '📤', variant: 'success', onClick: () => console.log('Send to WS-8') },
          { label: 'Report', icon: '📋', variant: 'info', onClick: () => console.log('Report') },
        ]}
      />
    </>
  );

  const ordersContent = (
    <OrdersGrid
      title={config?.ordersTitle || 'Motor Assembly Orders'}
      icon={config?.icon || '🔧'}
      orders={orders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search orders..."
      emptyMessage="No motor assembly orders"
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
      title={config?.title || 'Motor Assembly'}
      subtitle={config?.subtitle || 'WS-5 • Assembly'}
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

export default MotorAssemblyDashboard;
