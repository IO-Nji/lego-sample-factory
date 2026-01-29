import '../styles/CustomerOrderCard.css';

function CustomerOrderCard({
  order,
  orderNumber,
  status,
  items,
  dateText,
  actions,
  inventory,
  onConfirm,
  onFulfill,
  onProcess,
  onComplete,
  onCancel,
  isProcessing,
  canComplete,
  getProductDisplayName,
  getInventoryStatusColor
}) {
  // Support both direct props and order object (for backward compatibility)
  const resolvedOrder = order || { orderNumber, status, dateText };
  const resolvedStatus = resolvedOrder.status || status;
  const resolvedOrderNumber = resolvedOrder.orderNumber || orderNumber;
  // API returns 'orderItems' (from backend), also support 'items' for backward compatibility
  const resolvedItems = resolvedOrder.orderItems || resolvedOrder.items || items || [];
  const resolvedDateText = resolvedOrder.createdAt || dateText;

  // Status class for styling
  const statusClass = resolvedStatus ? `status-${resolvedStatus.toLowerCase()}` : '';

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  // Render action buttons based on status and scenario
  const renderActions = () => {
    if (actions && actions.length > 0) {
      return actions.map((action, idx) => (
        action.show !== false && (
          <button
            key={idx}
            type="button"
            className={action.className || ''}
            onClick={action.onClick}
            disabled={action.disabled}
            aria-busy={action.disabled}
          >
            <span>{action.label}</span>
          </button>
        )
      ));
    }

    // Default action buttons based on order status
    // TAXONOMY: Confirm=acknowledge, Fulfill=release stock, Request=create warehouse order, Complete=finish
    switch (resolvedStatus) {
      case 'PENDING':
        return (
          <button 
            type="button" 
            className="btn btn-confirm btn-sm"
            onClick={() => onConfirm && onConfirm(resolvedOrder.id)}
            disabled={isProcessing}
            data-action="confirm"
          >
            <span>✓ Confirm</span>
          </button>
        );
      case 'CONFIRMED':
        const triggerScenario = resolvedOrder.triggerScenario;
        if (triggerScenario === 'DIRECT_FULFILLMENT') {
          return (
            <button 
              type="button" 
              className="btn btn-fulfill btn-sm"
              onClick={() => onFulfill && onFulfill(resolvedOrder.id)}
              disabled={isProcessing}
              data-action="fulfill"
            >
              <span>✓ Fulfill</span>
            </button>
          );
        } else {
          return (
            <button 
              type="button" 
              className="btn btn-process btn-sm"
              onClick={() => onProcess && onProcess(resolvedOrder.id)}
              disabled={isProcessing}
              data-action="process"
            >
              <span>↓ Request</span>
            </button>
          );
        }
      case 'PROCESSING':
        return (
          <button 
            type="button" 
            className={`btn btn-complete btn-sm ${canComplete ? '' : 'btn-disabled'}`}
            onClick={() => onComplete && onComplete(resolvedOrder.id)}
            disabled={!canComplete || isProcessing}
            data-action="complete"
          >
            <span>{canComplete ? '✓ Complete' : 'Pending...'}</span>
          </button>
        );
      case 'COMPLETED':
        return (
          <span className="order-status-badge status-completed">✓ Completed</span>
        );
      case 'DELIVERED':
        return (
          <span className="order-status-badge status-delivered">✓ Delivered</span>
        );
      case 'CANCELLED':
        return (
          <span className="order-status-badge status-cancelled">✗ Cancelled</span>
        );
      default:
        return null;
    }
  };

  return (
    <div className={`customer-order-card ${statusClass}`}>
      <div className="order-card-header">
        <span className="order-number">{resolvedOrderNumber}</span>
        <span className={`order-status-badge ${statusClass}`}>
          {resolvedStatus}
        </span>
      </div>
      <div className="order-card-body">
        <div className="order-items-list">
          {resolvedItems && resolvedItems.length > 0 ? resolvedItems.map((item, idx) => (
            <div className="order-item" key={idx}>
              <div className="item-name">
                {getProductDisplayName 
                  ? getProductDisplayName(item.itemId, item.itemType) 
                  : (item.itemName || `Item #${item.itemId}` || 'Unknown')}
              </div>
              <div className="item-quantity-section">
                <span className="item-quantity">{item.quantity}</span>
              </div>
            </div>
          )) : <div className="no-items">No items</div>}
        </div>
        <div className="order-date-notification-row">
          {resolvedDateText && <div className="order-date">{formatDate(resolvedDateText)}</div>}
        </div>
      </div>
      <div className="order-card-footer">
        {renderActions()}
      </div>
    </div>
  );
}

export default CustomerOrderCard;
