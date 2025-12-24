import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/CustomerOrderCard.css';

/**
 * CustomerOrderCard Component
 * 
 * Displays a customer order with context-aware action buttons based on order status.
 * Implements smart button visibility logic:
 * - PENDING: Only shows "Confirm" button
 * - CONFIRMED: Shows "Fulfill" button (checks inventory and processes)
 * - PROCESSING: Shows "Complete" button
 * - COMPLETED: No action buttons
 * 
 * @param {Object} order - Order object with id, orderNumber, status, orderDate, orderItems
 * @param {Array} inventory - Current warehouse inventory for stock checking
 * @param {Function} onConfirm - Handler for confirming order
 * @param {Function} onFulfill - Handler for fulfilling order (checks stock)
 * @param {Function} onComplete - Handler for completing order
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
  onComplete,
  onCancel,
  isProcessing = false,
  getProductDisplayName,
  getInventoryStatusColor
}) {
  
  // Determine which buttons to show based on order status
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
        return { confirm: true };
      
      case 'CONFIRMED':
        return { fulfill: true, cancel: true };
      
      case 'PROCESSING':
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

      {/* Body with Order Items */}
      <div className="order-card-body">
        {order.orderItems && order.orderItems.length > 0 ? (
          <div className="order-items-list">
            {order.orderItems.map((item, idx) => {
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
        
        <div className="order-date">
          {new Date(order.orderDate).toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric',
            year: 'numeric'
          })}
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
                fullWidth
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
                fullWidth
              >
                {isProcessing ? 'Fulfilling...' : 'Fulfill Order'}
              </Button>
            )}
            
            {actions.complete && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onComplete(order.id)}
                disabled={isProcessing}
                fullWidth
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
                fullWidth
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
  onComplete: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  isProcessing: PropTypes.bool,
  getProductDisplayName: PropTypes.func,
  getInventoryStatusColor: PropTypes.func
};

export default CustomerOrderCard;
