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
 * PartFinishingDashboard - WS-3 Part Finishing Station
 * 
 * Handles part finishing orders for final parts processing and quality control.
 * Final step in the manufacturing workflow (WS-1 -> WS-2 -> WS-3).
 * Completed parts are credited to Parts Supply Warehouse (WS-9).
 */
function PartFinishingDashboard() {
  const {
    orders,
    error,
    processingOrderId,
    notifications,
    statsData,
    handleStartOrder,
    handleCompleteOrder,
    clearNotifications,
    clearError,
    config,
  } = useWorkstationOrders(3);

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
          orderType={ORDER_TYPES.WORKSTATION}
          order={order}
          onAction={(action, orderId) => {
            if (action === ACTION_TYPES.START) handleStartOrder(orderId);
            else if (action === ACTION_TYPES.COMPLETE) handleCompleteOrder(orderId);
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

export default PartFinishingDashboard;
