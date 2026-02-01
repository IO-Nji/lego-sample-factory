import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';
import Button from './Button';
import '../styles/WarehouseOrderCard.css';

/**
 * WarehouseOrderCard Component
 * 
 * Displays a warehouse (Modules Supermarket - WS-8) order with context-aware action buttons.
 * Uses BaseOrderCard for consistent layout with items grid display and scenario badges.
 * 
 * ARCHITECTURE:
 * - Uses CardBase.css for structural layout (display, flexbox, grid)
 * - WarehouseOrderCard.css provides orange color theme and status badges
 * - Semantic button colors via OrderCardButtons.css and Button component variants
 * - Items display: ACRONYM + QUANTITY (simplified format matching customer orders)
 * 
 * FEATURES:
 * - Simple quantity display (requested quantity)
 * - Scenario badge (DIRECT_FULFILLMENT, PRODUCTION_REQUIRED)
 * - Source customer order reference
 * - Status-specific action buttons with semantic coloring
 * 
 * @param {Object} order - Warehouse order from API
 * @param {number} order.id - Order ID
 * @param {string} order.status - Order status (PENDING, CONFIRMED, PROCESSING, FULFILLED, etc.)
 * @param {Array} order.orderItems - Array of order items
 * @param {Function} onConfirm - Handler for confirming order
 * @param {Function} onFulfill - Handler for fulfilling order
 * @param {Function} onCancel - Handler for cancelling order
 * @param {Function} onCreateProductionOrder - Handler for creating production order
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
  
  // Get status CSS class for badge styling
  const getStatusClass = (status) => {
    const statusMap = {
      PENDING: 'pending',
      CONFIRMED: 'confirmed',
      PROCESSING: 'processing',
      AWAITING_PRODUCTION: 'processing',
      MODULES_READY: 'ready',
      FULFILLED: 'fulfilled',
      REJECTED: 'rejected',
      CANCELLED: 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  // Get display-friendly status label
  const getStatusLabel = (status) => {
    const labelMap = {
      PENDING: 'PENDING',
      CONFIRMED: 'CONFIRMED',
      PROCESSING: 'PROCESSING',
      AWAITING_PRODUCTION: 'PRODUCTION',
      MODULES_READY: 'READY',
      FULFILLED: 'FULFILLED',
      REJECTED: 'REJECTED',
      CANCELLED: 'CANCELLED'
    };
    return labelMap[status] || status;
  };

  // Map action labels to semantic button variants
  // TAXONOMY: Confirm=acknowledge, Fulfill=release from stock (debits), Request=ask for production
  const getActionVariant = (actionLabel) => {
    const variantMap = {
      '✓ Confirm': 'confirm',
      'Confirming...': 'confirm',
      '✓ Fulfill': 'fulfill',
      'Fulfilling...': 'fulfill',
      '↓ Request': 'process',
      'Cancel': 'danger',
      '🟢 LOW': 'success',
      '🔵 NORMAL': 'primary',
      '🟠 HIGH': 'warning',
      '🔴 URGENT': 'danger'
    };
    return variantMap[actionLabel] || 'primary';  // Fallback to primary
  };

  // Transform order items to BaseOrderCard format (ACRONYM + QUANTITY)
  // API returns 'orderItems'
  const items = order.orderItems || [];
  const transformedItems = items.map(item => {
    const itemName = getProductDisplayName ? 
      getProductDisplayName(item.itemId, item.itemType) : 
      (item.itemName || `Item ${item.itemId}`);
    
    return {
      name: itemName,
      quantity: item.requestedQuantity || 0,
      statusColor: (item.fulfilledQuantity >= item.requestedQuantity) ? '#059669' : '#dc2626'
    };
  }) || [];

  // Transform scenario to badge format
  const scenarioBadge = order.triggerScenario ? {
    text: order.triggerScenario.replace('_', ' '),
    variant: order.triggerScenario.includes('PRODUCTION') ? 'warning' : 'info'
  } : null;

  // Build subtitle with source order reference
  const customerOrderRef = order.customerOrderId;
  const subtitle = customerOrderRef ? 
    `Source: CO-${customerOrderRef}` : null;

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
          label: isConfirming ? 'Confirming...' : '✓ Confirm',
          variant: getActionVariant('✓ Confirm'),
          size: 'small',
          onClick: () => onConfirm(order.id, order.orderNumber || order.warehouseOrderNumber),
          show: !!onConfirm
        });
        actions.push({
          label: 'Cancel',
          variant: getActionVariant('Cancel'),
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
        break;
      
      case 'CONFIRMED':
        // After confirmation, show actions based on triggerScenario
        // Priority selection mode for production order creation
        if (showPrioritySelection && onSelectPriority) {
          actions.push({
            label: creatingOrder ? '...' : '🟢 LOW',
            variant: getActionVariant('🟢 LOW'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'LOW'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔵 NORMAL',
            variant: getActionVariant('🔵 NORMAL'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'NORMAL'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🟠 HIGH',
            variant: getActionVariant('🟠 HIGH'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'HIGH'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔴 URGENT',
            variant: getActionVariant('🔴 URGENT'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'URGENT'),
            show: true
          });
        } else {
          // Check triggerScenario from order data (set by backend after production)
          // DIRECT_FULFILLMENT = modules available, show Fulfill button
          // PRODUCTION_REQUIRED = modules not available, show Request button
          if (order.triggerScenario === 'PRODUCTION_REQUIRED' && onCreateProductionOrder) {
            actions.push({
              label: '↓ Request',
              variant: getActionVariant('↓ Request'),
              size: 'small',
              onClick: () => onCreateProductionOrder(order),
              show: true
            });
          } else if (order.triggerScenario === 'DIRECT_FULFILLMENT' && onFulfill) {
            actions.push({
              label: isProcessing ? 'Fulfilling...' : '✓ Fulfill',
              variant: getActionVariant('✓ Fulfill'),
              size: 'small',
              onClick: () => onFulfill(order.id, order.warehouseOrderNumber),
              show: true
            });
          }
          actions.push({
            label: 'Cancel',
            variant: getActionVariant('Cancel'),
            size: 'small',
            onClick: () => onCancel(order.id),
            show: !!onCancel
          });
        }
        break;
      
      case 'PROCESSING':
        // Priority selection mode
        if (showPrioritySelection && onSelectPriority) {
          actions.push({
            label: creatingOrder ? '...' : '🟢 LOW',
            variant: getActionVariant('🟢 LOW'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'LOW'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔵 NORMAL',
            variant: getActionVariant('🔵 NORMAL'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'NORMAL'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🟠 HIGH',
            variant: getActionVariant('🟠 HIGH'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'HIGH'),
            show: true
          });
          actions.push({
            label: creatingOrder ? '...' : '🔴 URGENT',
            variant: getActionVariant('🔴 URGENT'),
            size: 'small',
            onClick: () => onSelectPriority(order, 'URGENT'),
            show: true
          });
        } else {
          // Check triggerScenario from order data
          if (order.triggerScenario === 'PRODUCTION_REQUIRED' && onCreateProductionOrder) {
            actions.push({
              label: '↓ Request',
              variant: getActionVariant('↓ Request'),
              size: 'small',
              onClick: () => onCreateProductionOrder(order),
              show: true
            });
          } else if (order.triggerScenario === 'DIRECT_FULFILLMENT' && onFulfill) {
            actions.push({
              label: isProcessing ? 'Fulfilling...' : '✓ Fulfill',
              variant: getActionVariant('✓ Fulfill'),
              size: 'small',
              onClick: () => onFulfill(order.id, order.warehouseOrderNumber),
              show: true
            });
          }
          actions.push({
            label: 'Cancel',
            variant: getActionVariant('Cancel'),
            size: 'small',
            onClick: () => onCancel(order.id),
            show: !!onCancel
          });
        }
        break;
      
      case 'AWAITING_PRODUCTION':
        // Production order has been created, waiting for production to complete
        // Show informational message - no actions available
        actions.push({
          label: '⏳ Awaiting Production',
          variant: 'secondary',
          size: 'small',
          disabled: true,
          show: true
        });
        break;
      
      case 'MODULES_READY':
        // Production completed, modules are now available in Modules Supermarket
        // Show Fulfill button to proceed with creating Final Assembly orders
        actions.push({
          label: isProcessing ? 'Fulfilling...' : '✓ Fulfill',
          variant: getActionVariant('✓ Fulfill'),
          size: 'small',
          onClick: () => onFulfill(order.id, order.warehouseOrderNumber),
          show: !!onFulfill
        });
        break;
      
      case 'FULFILLED':
      case 'CANCELLED':
      case 'REJECTED':
        // No actions available
        break;
      
      default:
        actions.push({
          label: 'Cancel',
          variant: getActionVariant('Cancel'),
          size: 'small',
          onClick: () => onCancel(order.id),
          show: !!onCancel
        });
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.orderNumber}`}
      status={getStatusLabel(order.status)}
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
    orderNumber: PropTypes.string.isRequired,
    status: PropTypes.string.isRequired,
    customerOrderId: PropTypes.number,
    triggerScenario: PropTypes.string,
    createdAt: PropTypes.string,
    orderItems: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.number,
        itemId: PropTypes.number,
        itemName: PropTypes.string,
        itemType: PropTypes.string,
        requestedQuantity: PropTypes.number,
        fulfilledQuantity: PropTypes.number
      })
    )
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
