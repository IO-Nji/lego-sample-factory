import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';
import Button from './Button';
import '../styles/WarehouseOrderCard.css';

/**
 * WarehouseOrderCard Component
 * 
 * Displays a warehouse order with context-aware action buttons based on order status.
 * Uses BaseOrderCard for consistent layout with dual quantity display and scenario badges.
 * 
 * Features:
 * - Dual quantity display (fulfilled/requested)
 * - Scenario badge (DIRECT_FULFILLMENT, PRODUCTION_REQUIRED, etc.)
 * - Source customer order reference
 * - Priority selection for production orders
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

  // Transform order items to BaseOrderCard format with dual quantity display
  const transformedItems = order.warehouseOrderItems?.map(item => {
    const itemName = getProductDisplayName ? 
      getProductDisplayName(item.itemId, item.itemType) : 
      (item.itemName || `Item ${item.itemId}`);
    
    return {
      name: itemName,
      quantity: item.fulfilledQuantity || 0,      // Primary: fulfilled
      secondaryQuantity: item.requestedQuantity,   // Secondary: requested
      quantityLabel: 'FF',
      secondaryLabel: 'RQ',
      statusColor: (item.fulfilledQuantity >= item.requestedQuantity) ? '#059669' : '#dc2626'
    };
  }) || [];

  // Transform scenario to badge format
  const scenarioBadge = order.triggerScenario ? {
    text: order.triggerScenario.replace('_', ' '),
    variant: order.triggerScenario.includes('PRODUCTION') ? 'warning' : 'info'
  } : null;

  // Build subtitle with source order reference
  const subtitle = order.sourceCustomerOrderId ? 
    `Source: CO-${order.sourceCustomerOrderId}` : null;

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
          label: isConfirming ? 'Confirming...' : 'Confirm',
          variant: 'primary',
          size: 'small',
          onClick: () => onConfirm(order.id, order.warehouseOrderNumber),
          show: !!onConfirm
        });
        actions.push({
          label: 'Cancel',
          variant: 'danger',
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
        break;
      
      case 'PROCESSING':
        // Priority selection mode
        if (showPrioritySelection && onSelectPriority) {
          actions.push({
            label: creatingOrder ? '...' : '🟢 LOW',
            variant: 'success',
            size: 'small',
            onClick: () => onSelectPriority(order, 'LOW'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔵 NORMAL',
            variant: 'primary',
            size: 'small',
            onClick: () => onSelectPriority(order, 'NORMAL'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🟠 HIGH',
            variant: 'warning',
            size: 'small',
            onClick: () => onSelectPriority(order, 'HIGH'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔴 URGENT',
            variant: 'danger',
            size: 'small',
            onClick: () => onSelectPriority(order, 'URGENT'),
            show: true
          });
        } else {
          // Normal fulfillment or production mode
          if (needsProduction && onCreateProductionOrder) {
            actions.push({
              label: '🏭 Order Production',
              variant: 'warning',
              size: 'small',
              onClick: () => onCreateProductionOrder(order),
              show: true
            });
          } else {
            actions.push({
              label: isProcessing ? 'Fulfilling...' : 'Fulfill',
              variant: 'success',
              size: 'small',
              onClick: () => onFulfill(order.id, order.warehouseOrderNumber),
              show: !!onFulfill
            });
          }
          actions.push({
            label: 'Cancel',
            variant: 'danger',
            size: 'small',
            onClick: () => onCancel(order.id),
            show: !!onCancel
          });
        }
        break;
      
      case 'FULFILLED':
      case 'CANCELLED':
      case 'REJECTED':
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
      orderNumber={`#${order.warehouseOrderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="warehouse-order-card"
      subtitle={subtitle}
      items={transformedItems}
      scenarioBadge={scenarioBadge}
      dateText={dateText}
      notificationMessage={notificationMessage}
      actions={getActions()}
      maxItems={4}
    />
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
