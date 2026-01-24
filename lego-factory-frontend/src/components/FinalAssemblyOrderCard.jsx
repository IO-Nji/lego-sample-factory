import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/FinalAssemblyOrderCard.css';

/**
 * FinalAssemblyOrderCard Component
 * 
 * Displays a Final Assembly order (WS-6) with order lineage and action buttons.
 * Shows the full order flow: CustomerOrder → WarehouseOrder → FinalAssemblyOrder
 * 
 * Status Lifecycle: PENDING → IN_PROGRESS → COMPLETED
 * Actions:
 * - PENDING: Start button (begins assembly work)
 * - IN_PROGRESS: Complete button (finishes assembly, credits Plant Warehouse)
 * - COMPLETED: No actions
 * 
 * @param {Object} order - Final Assembly order object
 * @param {Function} onStart - Handler for starting assembly work
 * @param {Function} onComplete - Handler for completing assembly (credits Plant Warehouse)
 * @param {boolean} isProcessing - Whether an action is in progress
 * @param {Function} getProductDisplayName - Function to format product names
 */
function FinalAssemblyOrderCard({ 
  order, 
  onStart,
  onComplete,
  isProcessing = false,
  getProductDisplayName
}) {
  
  // Determine which buttons to show based on order status
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'PENDING':
        return { start: true };
      
      case 'IN_PROGRESS':
        return { complete: true };
      
      case 'COMPLETED':
      case 'CANCELLED':
        return {}; // No actions available
      
      default:
        return {};
    }
  };

  const actions = getAvailableActions();

  // Get status badge styling
  const getStatusClass = (status) => {
    const statusMap = {
      PENDING: 'pending',
      IN_PROGRESS: 'in-progress',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Format timestamp
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className={`final-assembly-order-card status-${getStatusClass(order.status)}`}>
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <span className="order-number">#{order.orderNumber}</span>
        <span className={`order-status-badge ${getStatusClass(order.status)}`}>
          {order.status.replace('_', ' ')}
        </span>
      </div>

      {/* Body with Order Details */}
      <div className="order-card-body">
        {/* Order Lineage - Show parent orders */}
        <div className="order-lineage">
          <div className="lineage-title">Order Flow:</div>
          <div className="lineage-chain">
            {order.customerOrderId && (
              <span className="lineage-item customer">
                CO-{order.customerOrderId}
              </span>
            )}
            {order.warehouseOrderId && (
              <>
                <span className="lineage-arrow">→</span>
                <span className="lineage-item warehouse">
                  WO-{order.warehouseOrderId}
                </span>
              </>
            )}
            <span className="lineage-arrow">→</span>
            <span className="lineage-item final active">
              FA-{order.id}
            </span>
          </div>
        </div>

        {/* Output Product Information */}
        <div className="output-section">
          <div className="output-label">Output Product:</div>
          <div className="output-details">
            <span className="output-product">
              {getProductDisplayName ? 
                getProductDisplayName(order.outputProductId, 'PRODUCT') : 
                `Product #${order.outputProductId}`}
            </span>
            <span className="output-quantity">× {order.outputQuantity}</span>
          </div>
        </div>

        {/* Workstation */}
        <div className="workstation-info">
          <span className="workstation-label">Workstation:</span>
          <span className="workstation-value">WS-{order.workstationId} (Final Assembly)</span>
        </div>

        {/* Timestamps */}
        <div className="timestamps">
          <div className="timestamp-row">
            <span className="timestamp-label">Created:</span>
            <span className="timestamp-value">{formatTimestamp(order.createdAt)}</span>
          </div>
          {order.startTime && (
            <div className="timestamp-row">
              <span className="timestamp-label">Started:</span>
              <span className="timestamp-value">{formatTimestamp(order.startTime)}</span>
            </div>
          )}
          {order.completionTime && (
            <div className="timestamp-row">
              <span className="timestamp-label">Completed:</span>
              <span className="timestamp-value">{formatTimestamp(order.completionTime)}</span>
            </div>
          )}
        </div>
      </div>

      {/* Footer with Action Buttons */}
      {Object.keys(actions).length > 0 && (
        <div className="order-card-footer">
          <div className="action-buttons">
            {actions.start && (
              <Button 
                variant="primary" 
                size="small" 
                onClick={() => onStart(order.id, order.orderNumber)}
                disabled={isProcessing}
                loading={isProcessing}
              >
                {isProcessing ? 'Starting...' : '▶️ Start Assembly'}
              </Button>
            )}

            {actions.complete && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onComplete(order.id, order.orderNumber)}
                disabled={isProcessing}
                loading={isProcessing}
              >
                {isProcessing ? 'Completing...' : '✓ Complete Assembly'}
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

FinalAssemblyOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    orderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    workstationId: PropTypes.number,
    warehouseOrderId: PropTypes.number,
    customerOrderId: PropTypes.number,
    outputProductId: PropTypes.number,
    outputQuantity: PropTypes.number,
    startTime: PropTypes.string,
    completionTime: PropTypes.string,
    createdAt: PropTypes.string
  }).isRequired,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  isProcessing: PropTypes.bool,
  getProductDisplayName: PropTypes.func
};

export default FinalAssemblyOrderCard;
