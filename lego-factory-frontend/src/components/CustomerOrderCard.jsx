import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/CustomerOrderCard.css';

/**
 * CustomerOrderCard Component
 * 
 * Displays a customer order with context-aware action buttons based on order status.
 * Implements smart button visibility logic (Scenario 1 & 2):
 * - PENDING: Only shows "Confirm" button
 * - CONFIRMED: Shows "Fulfill" (if enough stock) OR "Process" (if insufficient stock) + "Cancel"
 * - PROCESSING: Shows "Complete" button (after warehouse order fulfilled) + "Cancel"
 * - COMPLETED: No action buttons
 * 
 * Scenario 1 (Enough Stock): PENDING → CONFIRMED → (Fulfill) → COMPLETED
 * Scenario 2 (Low Stock): PENDING → CONFIRMED → (Process) → PROCESSING → (Complete) → COMPLETED
 * 
 * @param {Object} order - Order object with id, orderNumber, status, orderDate, orderItems
 * @param {Array} inventory - Current warehouse inventory for stock checking
 * @param {Function} onConfirm - Handler for confirming order
 * @param {Function} onFulfill - Handler for fulfilling order (Scenario 1: enough stock)
 * @param {Function} onProcess - Handler for processing order (Scenario 2: creates warehouse order)
 * @param {Function} onComplete - Handler for completing order (Scenario 2: after warehouse fulfilled)
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
    if (!order.orderItems || order.orderItems.length === 0) return false;
    
    return order.orderItems.every(item => {
      const inventoryItem = inventory.find(
        (inv) => inv.itemId === item.itemId || inv.itemId === item.id
      );
      const stockQuantity = inventoryItem?.quantity || 0;
      return stockQuantity >= item.quantity;
    });
  };
  
  // Determine which buttons to show based on order status and stock availability
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
        return { confirm: true };
      
      case 'CONFIRMED':
        // Scenario 1: Enough stock → Fulfill
        // Scenario 2: Insufficient stock → Process (creates warehouse order)
        if (hasAllStock()) {
          return { fulfill: true, cancel: true };
        } else {
          return { process: true, cancel: true };
        }
      
      case 'PROCESSING':
        // After warehouse order is fulfilled, allow completion
        return { complete: true, cancel: true };
      
      case 'COMPLETED':
      case 'CANCELLED':
        return {}; // No actions available
      
      default:
        return { cancel: true };
    }
  };

  const actions = getAvailableActions();

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

  return (
    <div className={`customer-order-card status-${getStatusClass(order.status)}`}>
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <span className="order-number">#{order.orderNumber}</span>
        <span className={`order-status-badge ${getStatusClass(order.status)}`}>
          {order.status}
        </span>
      </div>

      {/* Body with Order Items - Limited to 4 items (2 rows) */}
      <div className="order-card-body">
        {order.orderItems && order.orderItems.length > 0 ? (
          <div className="order-items-list">
            {order.orderItems.slice(0, 4).map((item, idx) => {
              const inventoryItem = inventory.find(
                (inv) => inv.itemId === item.itemId || inv.itemId === item.id
              );
              const stockQuantity = inventoryItem?.quantity || 0;
              const statusColor = getInventoryStatusColor ? 
                getInventoryStatusColor(stockQuantity) : 
                (stockQuantity >= item.quantity ? '#059669' : '#dc2626');
              
              const productName = getProductDisplayName ? 
                getProductDisplayName(item.itemId || item.id, item.itemType) : 
                `Product #${item.itemId || item.id}`;

              const hasStock = stockQuantity >= item.quantity;

              return (
                <div key={idx} className="order-item">
                  <div className="item-name">{productName}</div>
                  <div className="item-quantity-section">
                    <span className="item-quantity" style={{ color: statusColor }}>
                      {item.quantity}
                    </span>
                    {order.status === 'CONFIRMED' && (
                      <span className="stock-indicator" style={{ color: statusColor }}>
                        {hasStock ? '✓' : '⚠'}
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="no-items">No items in order</p>
        )}
        
        {/* Date and Notification Row */}
        <div className="order-date-notification-row">
          {notificationMessage && (
            <div className={`order-notification-message ${notificationMessage.type || 'info'}`}>
              {notificationMessage.text}
            </div>
          )}
          <div className="order-date">
            {new Date(order.createdAt).toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
              year: 'numeric'
            })}
          </div>
        </div>
      </div>

      {/* Footer with Action Buttons */}
      {Object.keys(actions).length > 0 && (
        <div className="order-card-footer">
          <div className="action-buttons">
            {actions.confirm && (
              <Button 
                variant="primary" 
                size="small" 
                onClick={() => onConfirm(order.id)}
                disabled={isProcessing}
              >
                Confirm Order
              </Button>
            )}
            
            {actions.fulfill && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onFulfill(order.id)}
                disabled={isProcessing}
                loading={isProcessing}
              >
                {isProcessing ? 'Fulfilling...' : 'Fulfill Order'}
              </Button>
            )}
            
            {actions.process && onProcess && (
              <Button 
                variant="warning" 
                size="small" 
                onClick={() => onProcess(order.id)}
                disabled={isProcessing}
                loading={isProcessing}
              >
                {isProcessing ? 'Processing...' : 'Process Order'}
              </Button>
            )}
            
            {actions.complete && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onComplete(order.id)}
                disabled={isProcessing}
              >
                Complete Order
              </Button>
            )}
            
            {actions.cancel && (
              <Button 
                variant="danger" 
                size="small" 
                onClick={() => onCancel(order.id)}
                disabled={isProcessing}
              >
                Cancel
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
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
