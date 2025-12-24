import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/WarehouseOrderCard.css';

/**
 * WarehouseOrderCard Component
 * 
 * Displays a warehouse order with context-aware action buttons based on order status.
 * Similar to CustomerOrderCard but with distinct styling for warehouse operations.
 * 
 * @param {Object} order - Warehouse order object
 * @param {Function} onFulfill - Handler for fulfilling order
 * @param {Function} onCancel - Handler for cancelling order
 * @param {boolean} isProcessing - Whether fulfillment is in progress
 */
function WarehouseOrderCard({ 
  order, 
  onFulfill,
  onCancel,
  isProcessing = false
}) {
  
  // Determine which buttons to show based on order status
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
      case 'PROCESSING':
        return { fulfill: true, cancel: true };
      
      case 'FULFILLED':
      case 'CANCELLED':
      case 'REJECTED':
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
      PROCESSING: 'processing',
      FULFILLED: 'fulfilled',
      REJECTED: 'rejected',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Get scenario badge styling
  const getScenarioBadge = (scenario) => {
    if (!scenario) return null;
    return (
      <span className={`scenario-badge ${scenario.toLowerCase()}`}>
        {scenario.replace('_', ' ')}
      </span>
    );
  };

  return (
    <div className={`warehouse-order-card status-${getStatusClass(order.status)}`}>
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <span className="order-number">#{order.warehouseOrderNumber}</span>
        <span className={`order-status-badge ${getStatusClass(order.status)}`}>
          {order.status}
        </span>
      </div>

      {/* Body with Order Details */}
      <div className="order-card-body">
        {/* Source Order Info */}
        <div className="order-meta">
          <span className="meta-label">Source:</span>
          <span className="meta-value">CO-{order.sourceCustomerOrderId}</span>
        </div>

        {/* Scenario Badge */}
        {order.triggerScenario && (
          <div className="order-scenario">
            {getScenarioBadge(order.triggerScenario)}
          </div>
        )}

        {/* Order Items */}
        {order.warehouseOrderItems && order.warehouseOrderItems.length > 0 ? (
          <div className="order-items-list">
            {order.warehouseOrderItems.map((item) => (
              <div key={item.id} className="order-item">
                <div className="item-name">{item.itemName || `Item ${item.itemId}`}</div>
                <div className="item-quantity-section">
                  <span className="item-quantity requested">{item.requestedQuantity}</span>
                  <span className="quantity-separator">/</span>
                  <span className="item-quantity fulfilled">{item.fulfilledQuantity || 0}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="no-items">No items in order</p>
        )}
        
        <div className="order-date">
          {new Date(order.createdAt).toLocaleDateString('en-US', { 
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
            {actions.fulfill && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onFulfill(order.id, order.warehouseOrderNumber)}
                disabled={isProcessing}
                loading={isProcessing}
              >
                {isProcessing ? 'Fulfilling...' : 'Fulfill'}
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

WarehouseOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    warehouseOrderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    createdAt: PropTypes.string.isRequired,
    sourceCustomerOrderId: PropTypes.number,
    triggerScenario: PropTypes.string,
    warehouseOrderItems: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.number,
      itemId: PropTypes.number,
      itemName: PropTypes.string,
      requestedQuantity: PropTypes.number.isRequired,
      fulfilledQuantity: PropTypes.number
    }))
  }).isRequired,
  onFulfill: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  isProcessing: PropTypes.bool
};

export default WarehouseOrderCard;
