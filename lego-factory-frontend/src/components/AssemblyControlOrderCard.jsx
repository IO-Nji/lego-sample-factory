import PropTypes from 'prop-types';
import Button from './Button';
import Badge from './Badge';
import Card from './Card';
import '../styles/OrderCard.css';

/**
 * AssemblyControlOrderCard Component
 * 
 * Displays an assembly control order with context-aware action buttons.
 * Used by Assembly Control dashboard for assembly workstations (WS-4, WS-5, WS-6).
 * 
 * @param {Object} order - Assembly control order object
 * @param {Function} onStart - Handler for starting assembly
 * @param {Function} onComplete - Handler for completing assembly
 * @param {Function} onHalt - Handler for halting assembly
 * @param {Function} onRequestParts - Handler for requesting parts supply
 * @param {Function} onViewDetails - Handler for viewing order details
 */
function AssemblyControlOrderCard({ 
  order, 
  onStart,
  onComplete,
  onHalt,
  onRequestParts,
  onViewDetails
}) {
  
  // Get status badge variant
  const getStatusVariant = (status) => {
    switch(status) {
      case 'COMPLETED':
        return 'success';
      case 'IN_PROGRESS':
        return 'warning';
      case 'HALTED':
      case 'ABANDONED':
        return 'danger';
      case 'ASSIGNED':
        return 'info';
      default:
        return 'default';
    }
  };

  // Format date/time for display
  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  // Determine which buttons to show based on order status
  const getAvailableActions = () => {
    const status = order.status;
    
    switch(status) {
      case 'ASSIGNED':
        return { start: true, requestParts: true, viewDetails: true };
      
      case 'IN_PROGRESS':
        return { complete: true, halt: true, viewDetails: true };
      
      case 'HALTED':
        return { start: true, viewDetails: true }; // Can resume halted orders
      
      case 'COMPLETED':
      case 'ABANDONED':
        return { viewDetails: true }; // View only
      
      default:
        return { viewDetails: true };
    }
  };

  const actions = getAvailableActions();

  return (
    <Card className="order-card assembly-control-order-card">
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <div className="order-card-title">
          <h3 className="order-number">{order.controlOrderNumber}</h3>
          <p className="order-subtitle">
            Order #{order.sourceProductionOrderId}
          </p>
        </div>
        <Badge variant={getStatusVariant(order.status)}>
          {order.status}
        </Badge>
      </div>

      {/* Body with Order Details */}
      <div className="order-card-body">
        {/* Item Information */}
        {order.itemName && (
          <div className="order-info-section">
            <div className="info-row">
              <span className="info-label">Item:</span>
              <span className="info-value">{order.itemName}</span>
            </div>
            {order.quantity && (
              <div className="info-row">
                <span className="info-label">Quantity:</span>
                <span className="info-value">{order.quantity} units</span>
              </div>
            )}
          </div>
        )}

        {/* Priority Badge */}
        {order.priority && (
          <div className="order-priority">
            <span className={`priority-badge priority-${order.priority.toLowerCase()}`}>
              {order.priority}
            </span>
          </div>
        )}

        {/* Workstation Information */}
        {order.workstationName && (
          <div className="order-info-section">
            <div className="info-row">
              <span className="info-label">Workstation:</span>
              <span className="info-value">{order.workstationName}</span>
            </div>
          </div>
        )}

        {/* Schedule Information */}
        <div className="order-info-section">
          <div className="info-row">
            <span className="info-label">Target Start:</span>
            <span className="info-value">{formatDateTime(order.targetStartTime)}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Target Completion:</span>
            <span className="info-value">{formatDateTime(order.targetCompletionTime)}</span>
          </div>
        </div>

        {/* Actual Times (if available) */}
        {(order.actualStartTime || order.actualFinishTime) && (
          <div className="order-info-section">
            {order.actualStartTime && (
              <div className="info-row">
                <span className="info-label">Actual Start:</span>
                <span className="info-value">{formatDateTime(order.actualStartTime)}</span>
              </div>
            )}
            {order.actualFinishTime && (
              <div className="info-row">
                <span className="info-label">Actual Finish:</span>
                <span className="info-value">{formatDateTime(order.actualFinishTime)}</span>
              </div>
            )}
          </div>
        )}

        {/* Notes */}
        {order.notes && (
          <div className="order-notes">
            <p>{order.notes}</p>
          </div>
        )}
      </div>

      {/* Footer with Action Buttons */}
      <div className="order-card-footer">
        <div className="order-card-actions">
          {actions.start && (
            <Button 
              variant="success" 
              size="small" 
              onClick={() => onStart(order.id)}
            >
              {order.status === 'HALTED' ? 'Resume' : 'Start Assembly'}
            </Button>
          )}
          
          {actions.complete && (
            <Button 
              variant="primary" 
              size="small" 
              onClick={() => onComplete(order.id)}
            >
              Complete
            </Button>
          )}
          
          {actions.halt && (
            <Button 
              variant="warning" 
              size="small" 
              onClick={() => onHalt(order.id)}
            >
              Halt
            </Button>
          )}
          
          {actions.requestParts && (
            <Button 
              variant="outline" 
              size="small" 
              onClick={() => onRequestParts(order)}
            >
              Request Parts
            </Button>
          )}
          
          {actions.viewDetails && (
            <Button 
              variant="ghost" 
              size="small" 
              onClick={() => onViewDetails(order)}
            >
              Details
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
}

AssemblyControlOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    controlOrderNumber: PropTypes.string.isRequired,
    sourceProductionOrderId: PropTypes.number,
    status: PropTypes.string.isRequired,
    priority: PropTypes.string,
    itemName: PropTypes.string,
    quantity: PropTypes.number,
    workstationName: PropTypes.string,
    targetStartTime: PropTypes.string,
    targetCompletionTime: PropTypes.string,
    actualStartTime: PropTypes.string,
    actualFinishTime: PropTypes.string,
    notes: PropTypes.string
  }).isRequired,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  onHalt: PropTypes.func,
  onRequestParts: PropTypes.func,
  onViewDetails: PropTypes.func
};

AssemblyControlOrderCard.defaultProps = {
  onStart: () => {},
  onComplete: () => {},
  onHalt: () => {},
  onRequestParts: () => {},
  onViewDetails: () => {}
};

export default AssemblyControlOrderCard;
