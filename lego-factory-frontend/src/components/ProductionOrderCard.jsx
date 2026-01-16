import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/ProductionOrderCard.css';

/**
 * ProductionOrderCard Component
 * 
 * Displays a production order with context-aware action buttons based on order status.
 * Follows the same structure as CustomerOrderCard and WarehouseOrderCard.
 * 
 * @param {Object} order - Production order object
 * @param {Function} onSchedule - Handler for scheduling with SimAL
 * @param {Function} onStart - Handler for starting production
 * @param {Function} onComplete - Handler for completing production
 * @param {Function} onCancel - Handler for cancelling order
 * @param {boolean} isScheduling - Whether scheduling is in progress
 * @param {Object} notificationMessage - Optional notification message {text, type}
 */
function ProductionOrderCard({ 
  order, 
  onSchedule,
  onStart,
  onComplete,
  onCancel,
  isScheduling = false,
  notificationMessage = null
}) {
  
  // Determine which buttons to show based on order status
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'CREATED':
        return { schedule: true, cancel: true };
      
      case 'SUBMITTED':
        return { schedule: true, cancel: true };
      
      case 'SCHEDULED':
        return { start: true, cancel: true };
      
      case 'IN_PRODUCTION':
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
      CREATED: 'created',
      SUBMITTED: 'submitted',
      SCHEDULED: 'scheduled',
      IN_PRODUCTION: 'in-production',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Get priority badge styling
  const getPriorityBadge = (priority) => {
    if (!priority) return null;
    return (
      <span className={`priority-badge ${priority.toLowerCase()}`}>
        {priority}
      </span>
    );
  };

  return (
    <div className={`production-order-card status-${getStatusClass(order.status)}`}>
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <span className="order-number">#{order.productionOrderNumber}</span>
        <span className={`order-status-badge ${getStatusClass(order.status)}`}>
          {order.status}
        </span>
      </div>

      {/* Body with Order Details */}
      <div className="order-card-body">
        {/* Order Metadata */}
        {order.sourceWarehouseOrderId && (
          <div className="order-meta">
            <span className="meta-label">Source:</span>
            <span className="meta-value">WO-{order.sourceWarehouseOrderId}</span>
          </div>
        )}

        {/* Priority Badge - Same position as Scenario in WarehouseOrderCard */}
        {order.priority && (
          <div className="order-priority">
            {getPriorityBadge(order.priority)}
          </div>
        )}

        {/* Additional Details Grid */}
        <div className="order-details-grid">
          {order.dueDate && (
            <div className="detail-item">
              <span className="detail-label">Due:</span>
              <span className="detail-value">{new Date(order.dueDate).toLocaleDateString()}</span>
            </div>
          )}
          
          {order.simalScheduleId && (
            <div className="detail-item">
              <span className="detail-label">Schedule:</span>
              <span className="detail-value">{order.simalScheduleId}</span>
            </div>
          )}
          
          {order.estimatedDuration && (
            <div className="detail-item">
              <span className="detail-label">Duration:</span>
              <span className="detail-value">{order.estimatedDuration} min</span>
            </div>
          )}
        </div>
        
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
            {actions.schedule && onSchedule && (
              <Button 
                variant="primary" 
                size="small" 
                onClick={() => onSchedule(order)}
                disabled={isScheduling}
                loading={isScheduling}
              >
                {isScheduling ? 'Scheduling...' : '📅 Schedule'}
              </Button>
            )}

            {actions.start && onStart && (
              <Button 
                variant="warning" 
                size="small" 
                onClick={() => onStart(order.id)}
              >
                ▶️ Start
              </Button>
            )}

            {actions.complete && onComplete && (
              <Button 
                variant="success" 
                size="small" 
                onClick={() => onComplete(order.id)}
              >
                ✅ Complete
              </Button>
            )}

            {actions.cancel && onCancel && (
              <Button 
                variant="danger" 
                size="small" 
                onClick={() => onCancel(order.id)}
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

ProductionOrderCard.propTypes = {
  order: PropTypes.object.isRequired,
  onSchedule: PropTypes.func,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  onCancel: PropTypes.func,
  isScheduling: PropTypes.bool,
  notificationMessage: PropTypes.shape({
    text: PropTypes.string,
    type: PropTypes.oneOf(['info', 'success', 'warning', 'error'])
  })
};

export default ProductionOrderCard;
