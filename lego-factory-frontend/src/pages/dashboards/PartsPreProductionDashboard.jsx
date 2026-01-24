import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  ProductionControlOrderCard
} from "../../components";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";
import { STANDARD_FILTER_OPTIONS, STANDARD_SORT_OPTIONS } from "../../config/workstationConfig";
import "../../styles/DashboardLayout.css";

/**
 * PartsPreProductionDashboard - WS-2 Parts Pre-Production Station
 * 
 * Handles parts pre-production orders for parts preparation and processing.
 * Second step in the manufacturing workflow (WS-1 -> WS-2 -> WS-3).
 */
function PartsPreProductionDashboard() {
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
  } = useWorkstationOrders(2);

  const renderActivity = () => (
    <ActivityLog 
      notifications={notifications}
      onClear={clearNotifications}
    />
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
        <ProductionControlOrderCard
          key={order.id}
          order={order}
          onStart={handleStartOrder}
          onComplete={handleCompleteOrder}
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

export default PartsPreProductionDashboard;
