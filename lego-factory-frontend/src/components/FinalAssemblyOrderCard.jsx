import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';
import '../styles/CustomerOrderCard.css';

/**
 * FinalAssemblyOrderCard Component
 * 
 * Displays a final assembly order with context-aware action buttons.
 * Uses BaseOrderCard for consistent layout. Final Assembly (WS-6) assembles
 * products from modules and credits Plant Warehouse (WS-7) on submission.
 * 
 * 4-Step Workflow:
 * - PENDING → Confirm (acknowledge order)
 * - CONFIRMED → Start (begin assembly work, record start time)
 * - IN_PROGRESS → Complete (finish assembly, record completion time)
 * - COMPLETED → Submit (credit Plant Warehouse with finished products)
 * - SUBMITTED → No actions (order complete)
 * 
 * @param {Object} order - Final assembly order object
 * @param {Function} onConfirm - Handler for confirming order
 * @param {Function} onStart - Handler for starting assembly
 * @param {Function} onComplete - Handler for completing assembly
 * @param {Function} onSubmit - Handler for submitting finished products
 * @param {boolean} isProcessing - Whether an action is in progress
 * @param {Function} getProductDisplayName - Function to get product display name
 */
function FinalAssemblyOrderCard({ 
  order, 
  onConfirm,
  onStart,
  onComplete,
  onSubmit,
  isProcessing = false,
  getProductDisplayName
}) {
  
  // Get status CSS class
  const getStatusClass = (status) => {
    const statusMap = {
      'PENDING': 'pending',
      'IN_PROGRESS': 'in-progress',
      'COMPLETED': 'completed',
      'CANCELLED': 'cancelled',
      'HALTED': 'halted'
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
  const subtitle = order.warehouseOrderId ? 
    `Warehouse Order #${order.warehouseOrderId}` : null;

  // Transform to item format for BaseOrderCard
  const getProductName = () => {
    if (getProductDisplayName && order.outputProductId) {
      return getProductDisplayName(order.outputProductId, 'PRODUCT');
    }
    return order.outputProductVariantName || `Product ${order.outputProductId}`;
  };

  const items = [{
    name: getProductName(),
    quantity: `${order.outputQuantity || order.quantity || 0} units`
  }];

  // Build info sections for timing
  const infoSections = [];
  
  // Workstation info
  if (order.workstationId) {
    infoSections.push({
      rows: [{
        label: 'WS',
        value: `WS-${order.workstationId}`
      }]
    });
  }
  
  // Schedule information
  const timingRows = [];
  if (order.orderDate) {
    timingRows.push({ label: 'OD', value: formatDateTime(order.orderDate) });
  }
  if (order.startTime || order.actualStartTime) {
    timingRows.push({ label: 'ST', value: formatDateTime(order.startTime || order.actualStartTime) });
  }
  if (order.completionTime || order.actualFinishTime) {
    timingRows.push({ label: 'CT', value: formatDateTime(order.completionTime || order.actualFinishTime) });
  }
  if (order.submitTime) {
    timingRows.push({ label: 'SUB', value: formatDateTime(order.submitTime) });
  }
  if (timingRows.length > 0) {
    infoSections.push({ rows: timingRows });
  }

  // Format date text
  const dateText = order.createdAt ? `Created: ${new Date(order.createdAt).toLocaleDateString('en-US', { 
    month: 'short', 
    day: 'numeric',
    year: 'numeric'
  })}` : '';

  // Determine which actions to show based on order status (4-step workflow)
  const getActions = () => {
    const status = order.status;
    const actions = [];
    
    switch(status) {
      case 'PENDING':
        actions.push({
          label: isProcessing ? 'Confirming...' : '✓ Confirm Order',
          variant: 'info',
          size: 'small',
          onClick: () => onConfirm(order.id, order.orderNumber),
          show: !!onConfirm,
          disabled: isProcessing
        });
        break;
      
      case 'CONFIRMED':
        actions.push({
          label: isProcessing ? 'Starting...' : '▶️ Start Assembly',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id, order.orderNumber),
          show: !!onStart,
          disabled: isProcessing
        });
        break;
      
      case 'IN_PROGRESS':
        actions.push({
          label: isProcessing ? 'Completing...' : '✅ Complete Assembly',
          variant: 'primary',
          size: 'small',
          onClick: () => onComplete(order.id, order.orderNumber),
          show: !!onComplete,
          disabled: isProcessing
        });
        break;
      
      case 'COMPLETED':
        actions.push({
          label: isProcessing ? 'Submitting...' : '📦 Submit Product',
          variant: 'warning',
          size: 'small',
          onClick: () => onSubmit(order.id, order.orderNumber),
          show: !!onSubmit,
          disabled: isProcessing
        });
        break;
      
      case 'SUBMITTED':
        // No actions for submitted orders
        break;
      
      default:
        break;
    }
    
    return actions;
  };

  // Build notification for submitted status
  const notificationMessage = order.status === 'SUBMITTED' ? {
    text: '✅ Products credited to Plant Warehouse',
    type: 'success'
  } : order.status === 'COMPLETED' ? {
    text: '⏳ Ready to submit - click Submit to credit Plant Warehouse',
    type: 'info'
  } : null;

  return (
    <BaseOrderCard
      orderNumber={`#${order.orderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="final-assembly-order-card"
      subtitle={subtitle}
      items={items}
      infoSections={infoSections}
      notes={order.notes}
      dateText={dateText}
      notificationMessage={notificationMessage}
      actions={getActions()}
    />
  );
}

FinalAssemblyOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    orderNumber: PropTypes.string.isRequired,
    warehouseOrderId: PropTypes.number,
    assemblyControlOrderId: PropTypes.number,
    workstationId: PropTypes.number,
    outputProductId: PropTypes.number,
    outputProductVariantName: PropTypes.string,
    outputQuantity: PropTypes.number,
    quantity: PropTypes.number,
    orderDate: PropTypes.string,
    status: PropTypes.string.isRequired,
    startTime: PropTypes.string,
    actualStartTime: PropTypes.string,
    completionTime: PropTypes.string,
    actualFinishTime: PropTypes.string,
    submitTime: PropTypes.string,
    notes: PropTypes.string,
    createdAt: PropTypes.string,
  }).isRequired,
  onConfirm: PropTypes.func,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  onSubmit: PropTypes.func,
  isProcessing: PropTypes.bool,
  getProductDisplayName: PropTypes.func
};

FinalAssemblyOrderCard.defaultProps = {
  onConfirm: null,
  onStart: null,
  onComplete: null,
  onSubmit: null,
  isProcessing: false,
  getProductDisplayName: null
};

export default FinalAssemblyOrderCard;
