import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';

/**
 * CustomerOrderCard Component
 * 
 * Displays a customer order with context-aware action buttons based on order status.
 * Implements smart button visibility logic (Scenario 1 & 2):
 * - PENDING: Only shows "Confirm" button
 * - CONFIRMED: Shows "Fulfill" (if enough stock) OR "Process" (if insufficient stock) + "Cancel"
 * - PROCESSING: No action buttons (waiting for Final Assembly to complete and update status)
 * - COMPLETED: No action buttons
 * 
 * Scenario 1 (Enough Stock): PENDING → CONFIRMED → (Fulfill) → COMPLETED
 * Scenario 2 (Low Stock): PENDING → CONFIRMED → (Process) → PROCESSING → (Final Assembly completes) → CONFIRMED → (Fulfill) → COMPLETED
 * 
 * Important: When order is PROCESSING, it means a warehouse order was created and Final Assembly
 * is working on it. Once Final Assembly completes, it updates the customer order status back to
 * CONFIRMED, at which point Plant Warehouse can fulfill it.
 * 
 * @param {Object} order - Order object with id, orderNumber, status, orderDate, orderItems
 * @param {Array} inventory - Current warehouse inventory for stock checking
 * @param {Function} onConfirm - Handler for confirming order
 * @param {Function} onFulfill - Handler for fulfilling order (Scenario 1: enough stock, or after Final Assembly)
 * @param {Function} onProcess - Handler for processing order (Scenario 2: creates warehouse order)
 * @param {Function} onComplete - Handler for completing order (DEPRECATED - not used in current flow)
 * @param {Function} onCancel - Handler for cancelling order
 * @param {boolean} isProcessing - Whether any action is in progress
 * @param {Function} getProductDisplayName - Function to format product names
 * @param {Function} getInventoryStatusColor - Function to get color based on stock level
 */
function CustomerOrderCard({ 
  order, 
  inventory = [],
  onConfirm,
  onFulfill,
  onProcess,
  onComplete,
  onCancel,
  isProcessing = false,
  getProductDisplayName,
  getInventoryStatusColor,
  notificationMessage = null
}) {
  
  // Check if all items have sufficient stock
  const hasAllStock = () => {
    if (!order.orderItems || order.orderItems.length === 0) {
      console.log('[CustomerOrderCard] No order items to check');
      return false;
    }
    
    console.log('[CustomerOrderCard] Checking stock for order', order.orderNumber, 'Inventory items:', inventory.length);
    
    return order.orderItems.every(item => {
      // Handle both PRODUCT and PRODUCT_VARIANT as synonyms
      const inventoryItem = inventory.find((inv) => {
        const itemTypeMatch = inv.itemType === item.itemType ||
          ((inv.itemType === 'PRODUCT' || inv.itemType === 'PRODUCT_VARIANT') && 
           (item.itemType === 'PRODUCT' || item.itemType === 'PRODUCT_VARIANT'));
        return itemTypeMatch && inv.itemId === item.itemId;
      });
      const stockQuantity = inventoryItem?.quantity || 0;
      
      console.log(`[CustomerOrderCard] Stock check for item ${item.itemId}:`, {
        itemType: item.itemType,
        itemId: item.itemId,
        requestedQty: item.quantity,
        availableStock: stockQuantity,
        hasStock: stockQuantity >= item.quantity,
        inventoryItem: inventoryItem ? `Found: ${inventoryItem.itemType}` : 'Not found'
      });
      
      return stockQuantity >= item.quantity;
    });
  };
  
  // Determine which buttons to show based on order status and stock availability
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
        return [
          { label: 'Confirm Order', variant: 'primary', onClick: () => onConfirm(order.id), show: true }
        ];
      
      case 'CONFIRMED':
        // Scenario 1: Enough stock → Fulfill
        // Scenario 2: Insufficient stock → Process (creates warehouse order)
        if (hasAllStock()) {
          return [
            { 
              label: isProcessing ? 'Fulfilling...' : 'Fulfill Order', 
              variant: 'success', 
              onClick: () => onFulfill(order.id), 
              show: true 
            },
            { label: 'Cancel', variant: 'danger', onClick: () => onCancel(order.id), show: true }
          ];
        } else {
          return [
            { 
              label: isProcessing ? 'Processing...' : 'Process Order', 
              variant: 'warning', 
              onClick: () => onProcess(order.id), 
              show: !!onProcess 
            },
            { label: 'Cancel', variant: 'danger', onClick: () => onCancel(order.id), show: true }
          ];
        }
      
      case 'PROCESSING':
        // Order is being processed (warehouse order created, waiting for Final Assembly)
        // No actions available - waiting for Final Assembly to complete and update status to CONFIRMED
        return [];
      
      case 'COMPLETED':
      case 'CANCELLED':
        return []; // No actions available
      
      default:
        return [
          { label: 'Cancel', variant: 'danger', onClick: () => onCancel(order.id), show: true }
        ];
    }
  };

  // Get status badge styling
  const getStatusClass = (status) => {
    const statusMap = {
      PENDING: 'pending',
      CONFIRMED: 'confirmed',
      PROCESSING: 'processing',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Transform order items to BaseOrderCard format
  const transformedItems = order.orderItems?.map(item => {
    // Handle both PRODUCT and PRODUCT_VARIANT as synonyms
    const inventoryItem = inventory.find((inv) => {
      const itemTypeMatch = inv.itemType === item.itemType ||
        ((inv.itemType === 'PRODUCT' || inv.itemType === 'PRODUCT_VARIANT') && 
         (item.itemType === 'PRODUCT' || item.itemType === 'PRODUCT_VARIANT'));
      return itemTypeMatch && inv.itemId === item.itemId;
    });
    const stockQuantity = inventoryItem?.quantity || 0;
    const statusColor = getInventoryStatusColor ? 
      getInventoryStatusColor(stockQuantity) : 
      (stockQuantity >= item.quantity ? '#059669' : '#dc2626');
    
    const productName = getProductDisplayName ? 
      getProductDisplayName(item.itemId, item.itemType) : 
      `Product #${item.itemId}`;

    const hasStock = stockQuantity >= item.quantity;

    return {
      name: productName,
      quantity: item.quantity,
      statusColor: order.status === 'CONFIRMED' ? statusColor : undefined,
      hasStock: order.status === 'CONFIRMED' ? hasStock : undefined
    };
  }) || [];

  // Additional notes section with date and notification
  const dateText = new Date(order.createdAt).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });

  return (
    <BaseOrderCard
      orderNumber={`#${order.orderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="customer-order"
      items={transformedItems}
      dateText={dateText}
      notificationMessage={notificationMessage}
      actions={getAvailableActions()}
      maxItems={4}
    />
  );
}

CustomerOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    orderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    orderDate: PropTypes.string.isRequired,
    orderItems: PropTypes.arrayOf(PropTypes.shape({
      itemId: PropTypes.number,
      id: PropTypes.number,
      itemType: PropTypes.string,
      quantity: PropTypes.number.isRequired
    }))
  }).isRequired,
  inventory: PropTypes.array,
  onConfirm: PropTypes.func.isRequired,
  onFulfill: PropTypes.func.isRequired,
  onProcess: PropTypes.func, // Optional: for Scenario 2 (low stock)
  onComplete: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  isProcessing: PropTypes.bool,
  getProductDisplayName: PropTypes.func,
  getInventoryStatusColor: PropTypes.func,
  notificationMessage: PropTypes.shape({
    text: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['success', 'info', 'warning', 'error'])
  })
};

export default CustomerOrderCard;
