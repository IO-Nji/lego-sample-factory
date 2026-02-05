import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  Card
} from "../../components";
import UnifiedOrderCard, { ORDER_TYPES, ACTION_TYPES } from "../../components/orders/UnifiedOrderCard";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";
import { STANDARD_FILTER_OPTIONS, STANDARD_SORT_OPTIONS } from "../../config/workstationConfig";
import "../../styles/DashboardLayout.css";

/**
 * InjectionMoldingDashboard - WS-1 Injection Molding Station
 * 
 * Handles injection molding orders for plastic parts production.
 * First step in the manufacturing workflow (WS-1 -> WS-2 -> WS-3).
 */
function InjectionMoldingDashboard() {
  const {
    orders,
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
    config,
  } = useWorkstationOrders(1);

  const renderActivity = () => (
    <Card variant="framed" title="STATION ACTIVITY" style={{ height: '100%' }}>
      <ActivityLog 
        notifications={notifications}
        onClear={clearNotifications}
        showTitle={false}
      />
    </Card>
  );

  const renderOrders = () => (
    <OrdersSection
      title={config.ordersTitle}
      icon={config.icon}
      orders={orders}
      filterOptions={STANDARD_FILTER_OPTIONS}
      sortOptions={STANDARD_SORT_OPTIONS}
      searchKeys={['orderNumber']}
      sortKey="orderNumber"
      renderCard={(order) => (
        <UnifiedOrderCard
          key={order.id}
          orderType={ORDER_TYPES.WORKSTATION_ORDER}
          order={order}
          onAction={(action, orderId) => {
            if (action === ACTION_TYPES.START) handleStartOrder(orderId);
            else if (action === ACTION_TYPES.COMPLETE) handleCompleteOrder(orderId);
            else if (action === ACTION_TYPES.HALT) handleHaltOrder(orderId, 'Operator initiated halt');
            else if (action === ACTION_TYPES.RESUME) handleResumeOrder(orderId);
          }}
          isProcessing={processingOrderId === order.id}
        />
      )}
      emptyMessage={config.emptyMessage}
      searchPlaceholder="Search by order number..."
    />
  );

  return (
    <StandardDashboardLayout
      title={config.title}
      subtitle={config.subtitle}
      icon={config.icon}
      activityContent={renderActivity()}
      statsContent={<StatisticsGrid stats={statsData} />}
      formContent={null}
      contentGrid={renderOrders()}
      inventoryContent={null}
      messages={{ error, success: null }}
      onDismissError={clearError}
      onDismissSuccess={() => {}}
    />
  );
}

export default InjectionMoldingDashboard;
