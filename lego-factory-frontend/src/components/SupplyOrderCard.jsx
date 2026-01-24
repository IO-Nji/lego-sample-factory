import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';

/**
 * SupplyOrderCard Component
 * 
 * Displays a supply order for parts from Parts Supply Warehouse to production/assembly workstations.
 * Uses BaseOrderCard for consistent layout with from/to workstation display and parts list.
 * 
 * Features:
 * - From/To workstation information
 * - Parts list with quantities
 * - Priority badge display
 * - Source control order reference
 * - Status-aware actions (Start, Fulfill, Reject)
 * 
 * @param {Object} order - Supply order object
 * @param {Function} onStart - Handler for starting fulfillment (PENDING → IN_PROGRESS)
 * @param {Function} onFulfill - Handler for completing fulfillment
 * @param {Function} onReject - Handler for rejecting order (insufficient stock)
 * @param {Function} onCancel - Handler for cancelling order
 * @param {Function} getWorkstationName - Function to get workstation name from ID
 * @param {Function} getPartName - Function to get part name from ID
 */
function SupplyOrderCard({ 
  order, 
  onStart,
  onFulfill,
  onReject,
  onCancel,
  getWorkstationName,
  getPartName
}) {
  
  // Get status CSS class
  const getStatusClass = (status) => {
    const statusMap = {
      'PENDING': 'pending',
      'IN_PROGRESS': 'in-progress',
      'FULFILLED': 'fulfilled',
      'REJECTED': 'rejected',
      'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Build subtitle with source control order
  const subtitle = order.sourceControlOrderId ? 
    `${order.sourceControlOrderType || 'Control'} Order #${order.sourceControlOrderId}` : null;

  // Transform items to BaseOrderCard format
  const items = order.supplyOrderItems?.map(item => ({
    name: getPartName ? getPartName(item.partId) : `Part ${item.partId}`,
    quantity: `${item.quantity} units`
  })) || [];

  // Build info sections for from/to workstations
  const infoSections = [];
  
  if (getWorkstationName) {
    const fromWorkstation = getWorkstationName(order.supplyWarehouseWorkstationId);
    const toWorkstation = getWorkstationName(order.requestingWorkstationId);
    
    infoSections.push({
      rows: [
        { label: 'FR', value: fromWorkstation || `WS-${order.supplyWarehouseWorkstationId}` },
        { label: 'TO', value: toWorkstation || `WS-${order.requestingWorkstationId}` }
      ]
    });
  }

  // Add timing info if available
  if (order.requestedByTime || order.fulfilledAt) {
    const timingRows = [];
    if (order.requestedByTime) {
      timingRows.push({
        label: 'ND',
        value: new Date(order.requestedByTime).toLocaleString('en-US', {
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      });
    }
    if (order.fulfilledAt) {
      timingRows.push({
        label: 'FF',
        value: new Date(order.fulfilledAt).toLocaleString('en-US', {
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        })
      });
    }
    if (timingRows.length > 0) {
      infoSections.push({ rows: timingRows });
    }
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
      case 'PENDING':
        actions.push({
          label: '✅ Fulfill Order',
          variant: 'success',
          size: 'small',
          onClick: () => onFulfill(order.id),
          show: !!onFulfill
        });
        actions.push({
          label: '❌ Reject',
          variant: 'danger',
          size: 'small',
          onClick: () => onReject(order.id),
          show: !!onReject
        });
        break;
      
      case 'FULFILLED':
      case 'REJECTED':
      case 'CANCELLED':
        // No actions for completed orders
        break;
      
      default:
        break;
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.supplyOrderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="supply-order-card"
      subtitle={subtitle}
      items={items}
      infoSections={infoSections}
      priority={order.priority}
      notes={order.notes}
      dateText={dateText}
      actions={getActions()}
    />
  );
}

SupplyOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    supplyOrderNumber: PropTypes.string.isRequired,
    sourceControlOrderId: PropTypes.number,
    sourceControlOrderType: PropTypes.string,
    requestingWorkstationId: PropTypes.number.isRequired,
    supplyWarehouseWorkstationId: PropTypes.number.isRequired,
    status: PropTypes.string.isRequired,
    priority: PropTypes.string,
    supplyOrderItems: PropTypes.arrayOf(
      PropTypes.shape({
        partId: PropTypes.number,
        quantity: PropTypes.number
      })
    ),
    createdAt: PropTypes.string,
    requestedByTime: PropTypes.string,
    fulfilledAt: PropTypes.string,
    notes: PropTypes.string
  }).isRequired,
  onStart: PropTypes.func,
  onFulfill: PropTypes.func,
  onReject: PropTypes.func,
  onCancel: PropTypes.func,
  getWorkstationName: PropTypes.func,
  getPartName: PropTypes.func
};

SupplyOrderCard.defaultProps = {
  onStart: () => {},
  onFulfill: () => {},
  onReject: () => {},
  onCancel: () => {}
};

export default SupplyOrderCard;
