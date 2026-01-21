import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';
import '../styles/CustomerOrderCard.css';

/**
 * ProductionControlOrderCard Component
 * 
 * Displays a production control order with context-aware action buttons.
 * Uses BaseOrderCard for consistent layout with manufacturing workstations (WS-1, WS-2, WS-3).
 * 
 * Features:
 * - Priority badge display
 * - Item and quantity display
 * - Target and actual timing (TS, TC, AS, AF)
 * - Workstation information
 * - Status-aware actions (Start, Complete, Halt, Request Parts)
 * 
 * @param {Object} order - Production control order object
 * @param {Function} onStart - Handler for starting production
 * @param {Function} onComplete - Handler for completing production
 * @param {Function} onHalt - Handler for halting production
 * @param {Function} onRequestParts - Handler for requesting parts supply
 * @param {Function} onViewDetails - Handler for viewing order details
 */
function ProductionControlOrderCard({ 
  order, 
  onStart,
  onComplete,
  onHalt,
  onRequestParts,
  onViewDetails
}) {
  
  // Get status CSS class
  const getStatusClass = (status) => {
    const statusMap = {
      'COMPLETED': 'completed',
      'IN_PROGRESS': 'in-progress',
      'HALTED': 'halted',
      'ABANDONED': 'abandoned',
      'ASSIGNED': 'assigned',
      'PENDING': 'pending'
    };
    return statusMap[status] || 'default';
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

  // Build subtitle with source order reference
  const subtitle = order.sourceProductionOrderId ? 
    `Production Order #${order.sourceProductionOrderId}` : null;

  // Transform item to BaseOrderCard format
  const items = order.itemName ? [{
    name: order.itemName,
    quantity: `${order.quantity} units`
  }] : [];

  // Build info sections for workstation and timing
  const infoSections = [];
  
  // Workstation info
  if (order.workstationName) {
    infoSections.push({
      rows: [{
        label: 'WS',
        value: order.workstationName
      }]
    });
  }
  
  // Schedule information
  infoSections.push({
    rows: [
      { label: 'TS', value: formatDateTime(order.targetStartTime) },
      { label: 'TC', value: formatDateTime(order.targetCompletionTime) }
    ]
  });
  
  // Actual times (if available)
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
      case 'ASSIGNED':
        actions.push({
          label: 'Start Production',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id),
          show: !!onStart
        });
        actions.push({
          label: 'Request Parts',
          variant: 'outline',
          size: 'small',
          onClick: () => onRequestParts(order),
          show: !!onRequestParts
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'IN_PROGRESS':
        actions.push({
          label: 'Complete',
          variant: 'primary',
          size: 'small',
          onClick: () => onComplete(order.id),
          show: !!onComplete
        });
        actions.push({
          label: 'Halt',
          variant: 'warning',
          size: 'small',
          onClick: () => onHalt(order.id),
          show: !!onHalt
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'HALTED':
        actions.push({
          label: 'Resume',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id),
          show: !!onStart
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'COMPLETED':
      case 'ABANDONED':
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      default:
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.controlOrderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="production-control-order-card"
      subtitle={subtitle}
      items={items}
      infoSections={infoSections}
      priority={order.priority}
      notes={order.notes}
      actions={getActions()}
    />
  );
}

ProductionControlOrderCard.propTypes = {
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

ProductionControlOrderCard.defaultProps = {
  onStart: () => {},
  onComplete: () => {},
  onHalt: () => {},
  onRequestParts: () => {},
  onViewDetails: () => {}
};

export default ProductionControlOrderCard;
