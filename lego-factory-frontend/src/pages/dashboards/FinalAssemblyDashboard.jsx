import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  Card
} from "../../components";
import UnifiedOrderCard, { ORDER_TYPES, ACTION_TYPES } from "../../components/orders/UnifiedOrderCard";
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
    handleHaltOrder,
    handleResumeOrder,
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
        <UnifiedOrderCard
          key={order.id}
          orderType={ORDER_TYPES.FINAL_ASSEMBLY_ORDER}
          order={order}
          onAction={(action, orderId) => {
            if (action === ACTION_TYPES.CONFIRM) handleConfirmOrder(orderId);
            else if (action === ACTION_TYPES.START) handleStartOrder(orderId);
            else if (action === ACTION_TYPES.COMPLETE) handleCompleteOrder(orderId);
            else if (action === ACTION_TYPES.SUBMIT) handleSubmitOrder(orderId);
            else if (action === ACTION_TYPES.HALT) handleHaltOrder(orderId, 'Operator initiated halt');
            else if (action === ACTION_TYPES.RESUME) handleResumeOrder(orderId);
          }}
          isProcessing={processingOrderId === order.id}
          getItemName={getProductDisplayName}
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
