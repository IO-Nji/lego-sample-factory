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
 * @param {Function} onConfirm - Handler for confirming order (PENDING → PROCESSING)
 * @param {Function} onFulfill - Handler for fulfilling order
 * @param {Function} onCancel - Handler for cancelling order
 * @param {Function} onCreateProductionOrder - Handler for creating production order
 * @param {boolean} needsProduction - Whether production order is needed (stock insufficient)
 * @param {boolean} isProcessing - Whether fulfillment is in progress
 * @param {boolean} isConfirming - Whether confirmation is in progress
 * @param {Function} getProductDisplayName - Function to format product names with acronyms
 * @param {Object} notificationMessage - Optional notification message {text, type}
 */
function WarehouseOrderCard({ 
  order, 
  onConfirm,
  onFulfill,
  onCancel,
  onCreateProductionOrder,
  onSelectPriority,
  needsProduction = false,
  isProcessing = false,
  isConfirming = false,
  showPrioritySelection = false,
  creatingOrder = false,
  getProductDisplayName,
  notificationMessage = null
}) {
  
  // Determine which buttons to show based on order status and stock availability
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
        // Only Confirm and Cancel buttons available
        return { confirm: true, cancel: true };
      
      case 'PROCESSING':
        // After confirmation, show EITHER Fulfill OR Production Order based on stock
        // If already showing priority selection, don't show other buttons
        if (showPrioritySelection) {
          return { prioritySelection: true };
        }
        // Show fulfill if sufficient stock, otherwise show production order button
        if (needsProduction) {
          return { createProduction: true, cancel: true };
        } else {
          return { fulfill: true, cancel: true };
        }
      
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

        {/* Order Items - Limited to 4 items (2 rows) */}
        {order.warehouseOrderItems && order.warehouseOrderItems.length > 0 ? (
          <div className="order-items-list">
            {order.warehouseOrderItems.slice(0, 4).map((item) => {
              const itemName = getProductDisplayName ? 
                getProductDisplayName(item.itemId, item.itemType) : 
                (item.itemName || `Item ${item.itemId}`);
              
              return (
                <div key={item.id} className="order-item">
                  <div className="item-name">{itemName}</div>
                  <div className="item-quantity-section">
                    <span className="item-quantity requested">{item.requestedQuantity}</span>
                    <span className="quantity-separator">/</span>
                    <span className="item-quantity fulfilled">{item.fulfilledQuantity || 0}</span>
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
                onClick={() => onConfirm(order.id, order.warehouseOrderNumber)}
                disabled={isConfirming || isProcessing}
                loading={isConfirming}
              >
                {isConfirming ? 'Confirming...' : 'Confirm'}
              </Button>
            )}

            {actions.fulfill && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onFulfill(order.id, order.warehouseOrderNumber)}
                disabled={isProcessing || isConfirming}
                loading={isProcessing}
              >
                {isProcessing ? 'Fulfilling...' : 'Fulfill'}
              </Button>
            )}

            {actions.createProduction && onCreateProductionOrder && (
              <Button 
                variant="warning" 
                size="small" 
                onClick={() => onCreateProductionOrder(order)}
                disabled={isProcessing || isConfirming || creatingOrder}
                title="Module stock insufficient - create production order"
              >
                🏭 Order Production
              </Button>
            )}

            {/* Priority Selection Buttons - shown after clicking Production Order */}
            {actions.prioritySelection && onSelectPriority && (
              <>
                <Button 
                  variant="success" 
                  size="small" 
                  onClick={() => onSelectPriority(order, 'LOW')}
                  disabled={creatingOrder}
                  title="Low priority"
                >
                  {creatingOrder ? '...' : '🟢 LOW'}
                </Button>
                <Button 
                  variant="primary" 
                  size="small" 
                  onClick={() => onSelectPriority(order, 'NORMAL')}
                  disabled={creatingOrder}
                  title="Normal priority"
                >
                  {creatingOrder ? '...' : '🔵 NORMAL'}
                </Button>
                <Button 
                  variant="warning" 
                  size="small" 
                  onClick={() => onSelectPriority(order, 'HIGH')}
                  disabled={creatingOrder}
                  title="High priority"
                >
                  {creatingOrder ? '...' : '🟠 HIGH'}
                </Button>
                <Button 
                  variant="danger" 
                  size="small" 
                  onClick={() => onSelectPriority(order, 'URGENT')}
                  disabled={creatingOrder}
                  title="Urgent priority"
                >
                  {creatingOrder ? '...' : '🔴 URGENT'}
                </Button>
              </>
            )}
            
            {actions.cancel && (
              <Button 
                variant="danger" 
                size="small" 
                onClick={() => onCancel(order.id)}
                disabled={isProcessing || isConfirming}
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
    sourceCustomerOrderId: PropTypes.number,
    triggerScenario: PropTypes.string,
    warehouseOrderItems: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.number,
        itemId: PropTypes.number,
        itemName: PropTypes.string,
        itemType: PropTypes.string,
        requestedQuantity: PropTypes.number,
        fulfilledQuantity: PropTypes.number
      })
    ),
    createdAt: PropTypes.string
  }).isRequired,
  onConfirm: PropTypes.func,
  onFulfill: PropTypes.func,
  onCancel: PropTypes.func,
  onCreateProductionOrder: PropTypes.func,
  onSelectPriority: PropTypes.func,
  needsProduction: PropTypes.bool,
  isProcessing: PropTypes.bool,
  isConfirming: PropTypes.bool,
  showPrioritySelection: PropTypes.bool,
  creatingOrder: PropTypes.bool,
  getProductDisplayName: PropTypes.func,
  notificationMessage: PropTypes.shape({
    text: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['success', 'info', 'warning', 'error'])
  })
};

export default WarehouseOrderCard;
