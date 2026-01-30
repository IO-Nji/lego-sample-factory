import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  WorkstationOrderCard
} from "../../components";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";
import { STANDARD_FILTER_OPTIONS, STANDARD_SORT_OPTIONS } from "../../config/workstationConfig";
import "../../styles/DashboardLayout.css";

/**
 * MotorAssemblyDashboard - WS-5 Motor Assembly Station
 * 
 * Handles motor assembly orders for assembling motor modules.
 * Second step in the assembly workflow (WS-4 -> WS-5 -> WS-6).
 * Completed modules are credited to Modules Supermarket (WS-8).
 */
function MotorAssemblyDashboard() {
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
  } = useWorkstationOrders(5);

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
        <WorkstationOrderCard
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

export default MotorAssemblyDashboard;
