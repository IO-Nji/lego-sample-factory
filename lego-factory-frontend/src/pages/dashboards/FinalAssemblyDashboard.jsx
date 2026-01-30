import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  FinalAssemblyOrderCard,
  Card
} from "../../components";
import { useWorkstationOrders } from "../../hooks/useWorkstationOrders";
import { getProductDisplayName } from "../../utils/dashboardHelpers";
import { STANDARD_FILTER_OPTIONS, STANDARD_SORT_OPTIONS } from "../../config/workstationConfig";
import "../../styles/DashboardLayout.css";

/**
 * FinalAssemblyDashboard - WS-6 Final Assembly Station
 * 
 * Handles final assembly orders for assembling finished products from modules.
 * Final step in the assembly workflow (WS-4 -> WS-5 -> WS-6).
 * 
 * 4-Step Workflow:
 * 1. Confirm - Acknowledge the order
 * 2. Start - Begin assembly work (records startTime)
 * 3. Complete - Finish assembly work (records completionTime)
 * 4. Submit - Credit Plant Warehouse with finished products
 */
function FinalAssemblyDashboard() {
  const {
    orders,
    error,
    processingOrderId,
    notifications,
    statsData,
    handleConfirmOrder,
    handleStartOrder,
    handleCompleteOrder,
    handleSubmitOrder,
    clearNotifications,
    clearError,
    config,
  } = useWorkstationOrders(6);

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
        <FinalAssemblyOrderCard
          key={order.id}
          order={order}
          onConfirm={handleConfirmOrder}
          onStart={handleStartOrder}
          onComplete={handleCompleteOrder}
          onSubmit={handleSubmitOrder}
          isProcessing={processingOrderId === order.id}
          getProductDisplayName={getProductDisplayName}
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

export default FinalAssemblyDashboard;
