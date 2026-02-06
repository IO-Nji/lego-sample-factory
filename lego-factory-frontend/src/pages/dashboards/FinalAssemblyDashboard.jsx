/**
 * FinalAssemblyDashboard - WS-6 Final Assembly Station
 * 
 * Handles final assembly orders that produce finished products.
 * Final step in assembly workflow (WS-4 -> WS-5 -> WS-6 -> WS-7).
 * 
 * IMPORTANT: FinalAssembly has a 4-step workflow:
 * PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED
 * Actions: Confirm -> Start -> Complete -> Submit (credits Plant Warehouse)
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

const WORKSTATION_ID = 6;

// FinalAssembly has additional CONFIRM and SUBMIT actions
const ORDER_ACTIONS = [
  { type: 'CONFIRM', label: 'Confirm', icon: '✓', variant: 'info', showFor: ['PENDING'] },
  { type: 'START', label: 'Start', icon: '▶', variant: 'primary', showFor: ['CONFIRMED'] },
  { type: 'COMPLETE', label: 'Complete', icon: '✓', variant: 'success', showFor: ['IN_PROGRESS'] },
  { type: 'SUBMIT', label: 'Submit', icon: '📤', variant: 'success', showFor: ['COMPLETED'] },
  { type: 'HALT', label: 'Halt', icon: '⏸', variant: 'warning', showFor: ['IN_PROGRESS'] },
  { type: 'RESUME', label: 'Resume', icon: '▶', variant: 'primary', showFor: ['HALTED'] }
];

const FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'HALTED', label: 'Halted' }
];

const SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' },
  { value: 'productId', label: 'Product' }
];

function FinalAssemblyDashboard() {
  const {
    orders, loading, error, processingOrderId, notifications, statsData,
    handleConfirmOrder, handleStartOrder, handleCompleteOrder, handleSubmitOrder, handleHaltOrder, handleResumeOrder,
    clearNotifications, clearError, refreshOrders, config,
  } = useWorkstationOrders(WORKSTATION_ID);

  const handleAction = useCallback((actionType, orderId) => {
    switch (actionType) {
      case 'CONFIRM': handleConfirmOrder?.(orderId); break;
      case 'START': handleStartOrder(orderId); break;
      case 'COMPLETE': handleCompleteOrder(orderId); break;
      case 'SUBMIT': handleSubmitOrder?.(orderId); break;
      case 'HALT': handleHaltOrder(orderId, 'Operator initiated halt'); break;
      case 'RESUME': handleResumeOrder(orderId); break;
      default: console.warn('Unknown action:', actionType);
    }
  }, [handleConfirmOrder, handleStartOrder, handleCompleteOrder, handleSubmitOrder, handleHaltOrder, handleResumeOrder]);

  const renderOrderItems = (order) => {
    // For final assembly, show output product
    if (order.outputProductId || order.productId) {
      const productId = order.outputProductId || order.productId;
      return (
        <div>
          <strong style={{ color: '#3b82f6' }}>Product #{productId}</strong>
          {order.productName && <span style={{ marginLeft: '0.5rem' }}>{order.productName}</span>}
          {order.quantity && <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{order.quantity}</span>}
        </div>
      );
    }
    if (!order.items || order.items.length === 0) {
      return <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>No items specified</span>;
    }
    return order.items.map((item, idx) => (
      <div key={idx} style={{ marginBottom: '0.25rem' }}>
        <strong>{item.itemName || `Component #${item.itemId}`}</strong>
        {item.quantity && <span style={{ marginLeft: '0.5rem', color: '#64748b' }}>×{item.quantity}</span>}
      </div>
    ));
  };

  const renderOrderExtra = (order) => (
    <>
      {order.outputProductId && (
        <span style={{ background: 'rgba(59, 130, 246, 0.1)', padding: '0.125rem 0.375rem', borderRadius: '4px', fontWeight: 600, color: '#3b82f6' }}>
          → Product #{order.outputProductId}
        </span>
      )}
      {order.quantity && (
        <span style={{ background: 'rgba(34, 197, 94, 0.1)', padding: '0.125rem 0.375rem', borderRadius: '4px', fontWeight: 600, color: '#16a34a' }}>
          Qty: {order.quantity}
        </span>
      )}
      {order.customerOrderId && (
        <span style={{ background: 'rgba(139, 92, 246, 0.1)', padding: '0.125rem 0.375rem', borderRadius: '4px', fontSize: '0.75rem', color: '#7c3aed' }}>
          CO-{order.customerOrderId}
        </span>
      )}
    </>
  );

  // Calculate stats from orders
  const pendingCount = orders.filter(o => o.status === 'PENDING').length;
  const confirmedCount = orders.filter(o => o.status === 'CONFIRMED').length;
  const activeCount = orders.filter(o => o.status === 'IN_PROGRESS').length;
  const completedCount = orders.filter(o => o.status === 'COMPLETED').length;
  const submittedCount = orders.filter(o => o.status === 'SUBMITTED').length;
  const readyToSubmit = completedCount;

  const panelRowContent = (
    <>
      {/* Final Assembly Orders Panel */}
      <DashboardPanel
        icon="🏭"
        title="Assembly Orders"
        badge={orders.length > 0 ? `${orders.length}` : null}
        badgeVariant="info"
        themeClass="ws-theme-6"
        stats={[
          { label: 'Pending', value: pendingCount + confirmedCount, variant: 'pending' },
          { label: 'Active', value: activeCount, variant: 'progress' },
          { label: 'Ready', value: readyToSubmit, variant: 'completed' },
          { label: 'Shipped', value: submittedCount, variant: 'success' },
        ]}
        alerts={readyToSubmit > 0 ? [{ message: `${readyToSubmit} ready to submit`, variant: 'success' }] : []}
      />

      {/* Module Kits Panel */}
      <DashboardPanel
        icon="📦"
        title="Module Kits"
        badge="Staged"
        badgeVariant="success"
        themeClass="ws-theme-6"
        stats={[
          { label: 'Gear Mod', value: '12', variant: 'info' },
          { label: 'Motor Mod', value: '10', variant: 'info' },
          { label: 'Kits Ready', value: confirmedCount, variant: 'completed' },
          { label: 'Waiting', value: pendingCount, variant: 'pending' },
        ]}
        subStats={[
          { label: 'From WS-4/5', value: 'On schedule' },
        ]}
      />

      {/* Products Output Panel */}
      <DashboardPanel
        icon="🎁"
        title="Products"
        badge="Output"
        badgeVariant="info"
        themeClass="ws-theme-6"
        stats={[
          { label: 'Today', value: submittedCount, variant: 'completed' },
          { label: 'To WS-7', value: submittedCount, variant: 'success' },
          { label: 'Target', value: '20', variant: 'info' },
          { label: 'Rate', value: `${submittedCount > 0 ? Math.round(submittedCount / 20 * 100) : 0}%`, variant: 'total' },
        ]}
        alerts={submittedCount >= 20 ? [{ message: 'Target reached! 🎉', variant: 'success' }] : []}
      />

      {/* Quick Actions Panel */}
      <DashboardPanel
        icon="⚡"
        title="Quick Actions"
        themeClass="ws-theme-6"
        actions={[
          { label: 'Confirm', icon: '✓', variant: 'info', onClick: () => {
            const nextOrder = orders.find(o => o.status === 'PENDING');
            if (nextOrder) handleAction('CONFIRM', nextOrder.id);
          }},
          { label: 'Start', icon: '▶️', variant: 'primary', onClick: () => {
            const nextOrder = orders.find(o => o.status === 'CONFIRMED');
            if (nextOrder) handleAction('START', nextOrder.id);
          }},
          { label: 'Complete', icon: '✅', variant: 'success', onClick: () => {
            const activeOrder = orders.find(o => o.status === 'IN_PROGRESS');
            if (activeOrder) handleAction('COMPLETE', activeOrder.id);
          }},
          { label: 'Submit', icon: '📤', variant: 'success', onClick: () => {
            const readyOrder = orders.find(o => o.status === 'COMPLETED');
            if (readyOrder) handleAction('SUBMIT', readyOrder.id);
          }},
        ]}
      />
    </>
  );

  const ordersContent = (
    <OrdersGrid
      title={config?.ordersTitle || 'Final Assembly Orders'}
      icon={config?.icon || '🏭'}
      orders={orders}
      filterOptions={FILTER_OPTIONS}
      sortOptions={SORT_OPTIONS}
      searchPlaceholder="Search orders or products..."
      emptyMessage="No final assembly orders"
      emptySubtext="Orders will appear when product assembly is needed"
      renderCard={(order) => <GridCard order={order} onAction={handleAction} actions={ORDER_ACTIONS} renderItems={renderOrderItems} renderExtra={renderOrderExtra} isProcessing={processingOrderId === order.id} />}
      renderCompactCard={(order) => <CompactCard order={order} onAction={handleAction} actions={ORDER_ACTIONS} isProcessing={processingOrderId === order.id} />}
      renderListItem={(order) => <ListRow order={order} onAction={handleAction} actions={ORDER_ACTIONS} columns={['orderNumber', 'status', 'date', 'product', 'quantity']} isProcessing={processingOrderId === order.id} />}
    />
  );

  const activityContent = <ActivityPanel title="Activity" notifications={notifications} onClear={clearNotifications} maxItems={5} compact={true} />;

  return (
    <WorkstationDashboard
      workstationId={WORKSTATION_ID}
      title={config?.title || 'Final Assembly'}
      subtitle={config?.subtitle || 'WS-6 • Product Assembly'}
      icon={config?.icon || '🏭'}
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

export default FinalAssemblyDashboard;
