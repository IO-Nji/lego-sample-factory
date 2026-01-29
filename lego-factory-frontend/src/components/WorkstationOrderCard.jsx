import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';

/**
 * WorkstationOrderCard Component
 * 
 * Generic order card for workstation-specific orders (InjectionMoldingOrder, 
 * PartPreProductionOrder, PartFinishingOrder, GearAssemblyOrder, MotorAssemblyOrder).
 * 
 * These orders are created when control orders are dispatched to workstations.
 * They have a simple workflow: PENDING → IN_PROGRESS → COMPLETED
 * 
 * Features:
 * - Order number and status display
 * - Priority badge
 * - Target/actual timing display
 * - Simple Start/Complete workflow (no confirm/dispatch steps)
 * 
 * Button Sequence:
 * - PENDING: Show "▶️ Start" button
 * - IN_PROGRESS: Show "✅ Complete" and "⏸️ Halt" buttons
 * - HALTED: Show "▶️ Resume" button
 * - COMPLETED: No action buttons
 * 
 * @param {Object} order - Workstation order object
 * @param {Function} onStart - Handler for starting work
 * @param {Function} onComplete - Handler for completing work
 * @param {Function} onHalt - Handler for halting work (optional)
 * @param {Function} onResume - Handler for resuming work (optional)
 * @param {Function} onViewDetails - Handler for viewing order details (optional)
 * @param {boolean} isProcessing - Whether the order is currently being processed
 */
function WorkstationOrderCard({ 
  order, 
  onStart,
  onComplete,
  onHalt,
  onResume,
  onViewDetails,
  isProcessing = false
}) {
  
  // Format date/time for display
  const formatDateTime = (dateStr) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Get status CSS class
  const getStatusClass = (status) => {
    const statusMap = {
      'PENDING': 'pending',
      'WAITING_FOR_PARTS': 'pending',
      'IN_PROGRESS': 'in-progress',
      'COMPLETED': 'completed',
      'HALTED': 'warning',
      'ABANDONED': 'danger'
    };
    return statusMap[status] || 'default';
  };

  // Build subtitle with output info
  const subtitle = order.outputPartName || order.outputModuleName || null;

  // Transform order data to BaseOrderCard format
  const items = [];
  if (order.quantity) {
    items.push({
      name: order.outputPartName || order.outputModuleName || 'Output',
      quantity: `${order.quantity} units`
    });
  }

  // Build info sections
  const infoSections = [];
  
  // Timing section
  const timingRows = [];
  if (order.targetStartTime) {
    timingRows.push({ label: 'TS', value: formatDateTime(order.targetStartTime) });
  }
  if (order.targetCompletionTime) {
    timingRows.push({ label: 'TC', value: formatDateTime(order.targetCompletionTime) });
  }
  if (timingRows.length > 0) {
    infoSections.push({ rows: timingRows });
  }
  
  // Actual times section (if available)
  const actualTimeRows = [];
  if (order.actualStartTime) {
    actualTimeRows.push({ label: 'AS', value: formatDateTime(order.actualStartTime) });
  }
  if (order.actualFinishTime) {
    actualTimeRows.push({ label: 'AF', value: formatDateTime(order.actualFinishTime) });
  }
  if (actualTimeRows.length > 0) {
    infoSections.push({ rows: actualTimeRows });
  }

  // Determine which actions to show based on order status
  const getActions = () => {
    const status = order.status;
    const actions = [];
    
    switch(status) {
      case 'PENDING':
      case 'WAITING_FOR_PARTS':
        // Ready to start work
        actions.push({
          label: isProcessing ? '⏳ Processing...' : '▶️ Start',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id, order.orderNumber),
          disabled: isProcessing,
          show: !!onStart
        });
        break;

      case 'IN_PROGRESS':
        // Work in progress - can complete or halt
        actions.push({
          label: isProcessing ? '⏳ Processing...' : '✅ Complete',
          variant: 'primary',
          size: 'small',
          onClick: () => onComplete(order.id, order.orderNumber),
          disabled: isProcessing,
          show: !!onComplete
        });
        if (onHalt) {
          actions.push({
            label: '⏸️ Halt',
            variant: 'warning',
            size: 'small',
            onClick: () => onHalt(order.id),
            disabled: isProcessing,
            show: true
          });
        }
        break;
      
      case 'HALTED':
        // Halted - can resume
        actions.push({
          label: '▶️ Resume',
          variant: 'success',
          size: 'small',
          onClick: () => (onResume || onStart)(order.id, order.orderNumber),
          disabled: isProcessing,
          show: !!(onResume || onStart)
        });
        break;
      
      case 'COMPLETED':
      case 'ABANDONED':
        // No actions for completed orders
        break;
      
      default:
        break;
    }
    
    // Always add Details button if handler provided
    if (onViewDetails) {
      actions.push({
        label: 'Details',
        variant: 'ghost',
        size: 'small',
        onClick: () => onViewDetails(order),
        show: true
      });
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.orderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="workstation-order-card"
      subtitle={subtitle}
      items={items}
      infoSections={infoSections}
      priority={order.priority}
      notes={order.operatorNotes || order.notes}
      actions={getActions()}
    />
  );
}

WorkstationOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    orderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    priority: PropTypes.string,
    quantity: PropTypes.number,
    outputPartName: PropTypes.string,
    outputModuleName: PropTypes.string,
    targetStartTime: PropTypes.string,
    targetCompletionTime: PropTypes.string,
    actualStartTime: PropTypes.string,
    actualFinishTime: PropTypes.string,
    operatorNotes: PropTypes.string,
    notes: PropTypes.string
  }).isRequired,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  onHalt: PropTypes.func,
  onResume: PropTypes.func,
  onViewDetails: PropTypes.func,
  isProcessing: PropTypes.bool
};

WorkstationOrderCard.defaultProps = {
  onStart: null,
  onComplete: null,
  onHalt: null,
  onResume: null,
  onViewDetails: null,
  isProcessing: false
};

export default WorkstationOrderCard;
