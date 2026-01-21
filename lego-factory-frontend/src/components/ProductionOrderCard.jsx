import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';

/**
 * ProductionOrderCard Component
 * 
 * Displays a production order with context-aware action buttons based on order status.
 * Uses BaseOrderCard for consistent layout with info sections for metadata.
 * 
 * Features:
 * - Priority badge display (HIGH, NORMAL, LOW, URGENT)
 * - Source warehouse order reference
 * - Metadata display (Due date, Schedule ID, Duration)
 * - Status-aware actions (Schedule, Submit, Complete, Cancel)
 * 
 * @param {Object} order - Production order object
 * @param {Function} onSchedule - Handler for scheduling with SimAL
 * @param {Function} onStart - Handler for starting/submitting production
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
  
  // Get status badge styling
  const getStatusClass = (status) => {
    const statusMap = {
      CREATED: 'created',
      SUBMITTED: 'submitted',
      SCHEDULED: 'scheduled',
      DISPATCHED: 'dispatched',
      IN_PRODUCTION: 'in-production',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Build subtitle with source order reference
  const subtitle = order.sourceWarehouseOrderId ? 
    `Source: WO-${order.sourceWarehouseOrderId}` : null;

  // Build info sections for metadata (Due date, Schedule ID, Duration)
  const infoSections = [];
  const metadataRows = [];
  
  if (order.dueDate) {
    metadataRows.push({
      label: 'DD',
      value: new Date(order.dueDate).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      })
    });
  }
  
  if (order.simalScheduleId) {
    metadataRows.push({
      label: 'SCH',
      value: order.simalScheduleId
    });
  }
  
  if (order.estimatedDuration) {
    metadataRows.push({
      label: 'DUR',
      value: `${order.estimatedDuration} min`
    });
  }
  
  if (metadataRows.length > 0) {
    infoSections.push({ rows: metadataRows });
  }

  // Format date text
  const dateText = `OD: ${new Date(order.createdAt).toLocaleDateString('en-US', { 
    month: 'short', 
    day: 'numeric',
    year: 'numeric'
  })}`;

  // Determine which actions to show based on order status
  const getActions = () => {
    const status = order.status;
    const actions = [];
    
    switch(status) {
      case 'CREATED':
      case 'SUBMITTED':
        actions.push({
          label: isScheduling ? 'Scheduling...' : '📅 Schedule',
          variant: 'primary',
          size: 'small',
          onClick: () => onSchedule(order),
          show: !!onSchedule
        });
        actions.push({
          label: 'Cancel',
          variant: 'danger',
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
        break;
      
      case 'SCHEDULED':
        // Production Planning submits/dispatches (creates control orders)
        actions.push({
          label: '📤 Submit',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id),
          show: !!onStart
        });
        actions.push({
          label: 'Cancel',
          variant: 'danger',
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
        break;
      
      case 'DISPATCHED':
        // Control orders created, waiting for workstations to start
        // No actions for Production Planning
        break;
      
      case 'IN_PRODUCTION':
        actions.push({
          label: '✅ Complete',
          variant: 'success',
          size: 'small',
          onClick: () => onComplete(order.id),
          show: !!onComplete
        });
        actions.push({
          label: 'Cancel',
          variant: 'danger',
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
        break;
      
      case 'COMPLETED':
      case 'CANCELLED':
        // No actions available
        break;
      
      default:
        actions.push({
          label: 'Cancel',
          variant: 'danger',
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.productionOrderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="production-order-card"
      subtitle={subtitle}
      infoSections={infoSections}
      priority={order.priority}
      dateText={dateText}
      notificationMessage={notificationMessage}
      actions={getActions()}
    />
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
