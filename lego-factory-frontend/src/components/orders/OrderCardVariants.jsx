/**
 * Order Card Variants - Compact, Grid, and List Views
 * 
 * Provides different presentation modes for order cards:
 * - GridCard: Full-featured card for grid view (default)
 * - CompactCard: Smaller card for compact grid view
 * - ListRow: Horizontal row for list view
 * 
 * All variants use consistent styling from WorkstationDashboard.css
 */

import PropTypes from 'prop-types';
import './OrderCardStyles.css';

// Status badge colors
const STATUS_COLORS = {
  PENDING: { bg: 'rgba(245, 158, 11, 0.1)', color: '#d97706', border: '#f59e0b' },
  CONFIRMED: { bg: 'rgba(59, 130, 246, 0.1)', color: '#2563eb', border: '#3b82f6' },
  IN_PROGRESS: { bg: 'rgba(6, 182, 212, 0.1)', color: '#0891b2', border: '#06b6d4' },
  COMPLETED: { bg: 'rgba(16, 185, 129, 0.1)', color: '#059669', border: '#10b981' },
  SUBMITTED: { bg: 'rgba(139, 92, 246, 0.1)', color: '#7c3aed', border: '#8b5cf6' },
  FULFILLED: { bg: 'rgba(16, 185, 129, 0.1)', color: '#059669', border: '#10b981' },
  CANCELLED: { bg: 'rgba(239, 68, 68, 0.1)', color: '#dc2626', border: '#ef4444' },
  HALTED: { bg: 'rgba(239, 68, 68, 0.1)', color: '#dc2626', border: '#ef4444' },
  DEFAULT: { bg: 'rgba(100, 116, 139, 0.1)', color: '#475569', border: '#94a3b8' }
};

// Priority indicators
const PRIORITY_ICONS = {
  URGENT: '🔴',
  HIGH: '🟠',
  MEDIUM: '🟡',
  LOW: '🟢',
  NORMAL: '⚪'
};

// Get status colors with fallback
const getStatusColors = (status) => {
  const upperStatus = (status || '').toUpperCase();
  return STATUS_COLORS[upperStatus] || STATUS_COLORS.DEFAULT;
};

// Format date for display
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// Format order number for display
const formatOrderNumber = (order) => {
  return order.orderNumber || 
         order.controlOrderNumber || 
         order.productionOrderNumber ||
         order.supplyOrderNumber ||
         `#${order.id}`;
};

/**
 * GridCard - Standard order card for grid view
 */
export function GridCard({ 
  order, 
  onAction, 
  actions = [],
  renderItems,
  renderExtra,
  isProcessing = false 
}) {
  const statusColors = getStatusColors(order.status);
  const priorityIcon = PRIORITY_ICONS[order.priority] || '';

  return (
    <div className="order-card order-card--grid" style={{ borderTopColor: statusColors.border }}>
      {/* Header */}
      <div className="order-card__header">
        <div className="order-card__title">
          <span className="order-card__number">{formatOrderNumber(order)}</span>
          {priorityIcon && <span className="order-card__priority">{priorityIcon}</span>}
        </div>
        <span 
          className="order-card__status"
          style={{ 
            background: statusColors.bg, 
            color: statusColors.color 
          }}
        >
          {order.status}
        </span>
      </div>
      
      {/* Date */}
      <div className="order-card__date">
        {formatDate(order.orderDate || order.createdAt)}
      </div>
      
      {/* Items */}
      {renderItems && (
        <div className="order-card__items">
          {renderItems(order)}
        </div>
      )}
      
      {/* Extra content (quantity, notes, etc) */}
      {renderExtra && (
        <div className="order-card__extra">
          {renderExtra(order)}
        </div>
      )}
      
      {/* Actions */}
      {actions.length > 0 && (
        <div className="order-card__actions">
          {actions.map((action) => {
            // Check if action should be shown based on order status
            if (action.showFor && !action.showFor.includes(order.status)) {
              return null;
            }
            
            // Check if action has a condition function that must be satisfied
            if (action.condition && typeof action.condition === 'function') {
              if (!action.condition(order)) {
                return null;
              }
            }
            
            return (
              <button
                key={action.type}
                type="button"
                className={`order-card__btn order-card__btn--${action.variant || 'default'}`}
                onClick={(e) => { e.preventDefault(); onAction(action.type, order.id, order); }}
                disabled={isProcessing || action.disabled}
              >
                {action.icon && <span>{action.icon}</span>}
                {action.label}
              </button>
            );
          })}
        </div>
      )}
      
      {/* Processing indicator */}
      {isProcessing && (
        <div className="order-card__processing">
          <span className="order-card__spinner" />
          Processing...
        </div>
      )}
    </div>
  );
}

/**
 * CompactCard - Smaller card for compact grid view
 */
export function CompactCard({ 
  order, 
  onAction, 
  actions = [],
  isProcessing = false 
}) {
  const statusColors = getStatusColors(order.status);
  const priorityIcon = PRIORITY_ICONS[order.priority] || '';

  return (
    <div className="order-card order-card--compact" style={{ borderLeftColor: statusColors.border }}>
      <div className="order-card__compact-header">
        <span className="order-card__number order-card__number--small">
          {formatOrderNumber(order)}
        </span>
        {priorityIcon && <span className="order-card__priority">{priorityIcon}</span>}
        <span 
          className="order-card__status order-card__status--small"
          style={{ background: statusColors.bg, color: statusColors.color }}
        >
          {order.status}
        </span>
      </div>
      
      <div className="order-card__compact-footer">
        <span className="order-card__date order-card__date--small">
          {formatDate(order.orderDate || order.createdAt)}
        </span>
        {actions.length > 0 && !isProcessing && (
          <div className="order-card__actions order-card__actions--compact">
            {actions.filter((action) => {
              // Filter by status
              if (action.showFor && !action.showFor.includes(order.status)) {
                return false;
              }
              // Filter by condition function
              if (action.condition && typeof action.condition === 'function') {
                return action.condition(order);
              }
              return true;
            }).slice(0, 2).map((action) => {
              return (
                <button
                  key={action.type}
                  type="button"
                  className={`order-card__btn order-card__btn--compact order-card__btn--${action.variant || 'default'}`}
                  onClick={(e) => { e.preventDefault(); onAction(action.type, order.id, order); }}
                  disabled={isProcessing || action.disabled}
                  title={action.label}
                >
                  {action.icon || action.label.charAt(0)}
                </button>
              );
            })}
          </div>
        )}
        {isProcessing && <span className="order-card__spinner order-card__spinner--small" />}
      </div>
    </div>
  );
}

/**
 * ListRow - Horizontal row for list view
 */
export function ListRow({ 
  order, 
  onAction, 
  actions = [],
  columns = ['orderNumber', 'status', 'date', 'priority'],
  isProcessing = false 
}) {
  const statusColors = getStatusColors(order.status);
  const priorityIcon = PRIORITY_ICONS[order.priority] || '';

  return (
    <div className="order-card order-card--list" style={{ borderLeftColor: statusColors.border }}>
      <div className="order-card__list-content">
        {/* Order Number */}
        {columns.includes('orderNumber') && (
          <span className="order-card__number order-card__number--list">
            {formatOrderNumber(order)}
          </span>
        )}
        
        {/* Status */}
        {columns.includes('status') && (
          <span 
            className="order-card__status order-card__status--list"
            style={{ background: statusColors.bg, color: statusColors.color }}
          >
            {order.status}
          </span>
        )}
        
        {/* Date */}
        {columns.includes('date') && (
          <span className="order-card__date order-card__date--list">
            {formatDate(order.orderDate || order.createdAt)}
          </span>
        )}
        
        {/* Priority */}
        {columns.includes('priority') && order.priority && (
          <span className="order-card__priority order-card__priority--list">
            {priorityIcon} {order.priority}
          </span>
        )}
        
        {/* Quantity if available */}
        {columns.includes('quantity') && order.quantity != null && (
          <span className="order-card__quantity">
            Qty: {order.quantity}
          </span>
        )}
      </div>
      
      {/* Actions */}
      <div className="order-card__list-actions">
        {isProcessing ? (
          <span className="order-card__spinner order-card__spinner--small" />
        ) : (
          actions.map((action) => {
            // Filter by status
            if (action.showFor && !action.showFor.includes(order.status)) {
              return null;
            }
            // Filter by condition function
            if (action.condition && typeof action.condition === 'function') {
              if (!action.condition(order)) {
                return null;
              }
            }
            return (
              <button
                key={action.type}
                type="button"
                className={`order-card__btn order-card__btn--list order-card__btn--${action.variant || 'default'}`}
                onClick={(e) => { e.preventDefault(); onAction(action.type, order.id, order); }}
                disabled={action.disabled}
              >
                {action.icon && <span>{action.icon}</span>}
                {action.label}
              </button>
            );
          })
        )}
      </div>
    </div>
  );
}

// PropTypes
const orderShape = PropTypes.shape({
  id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  orderNumber: PropTypes.string,
  controlOrderNumber: PropTypes.string,
  productionOrderNumber: PropTypes.string,
  status: PropTypes.string.isRequired,
  orderDate: PropTypes.string,
  createdAt: PropTypes.string,
  priority: PropTypes.string,
  quantity: PropTypes.number
});

const actionShape = PropTypes.shape({
  type: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  icon: PropTypes.string,
  variant: PropTypes.oneOf(['primary', 'success', 'warning', 'danger', 'default']),
  showFor: PropTypes.arrayOf(PropTypes.string),
  disabled: PropTypes.bool
});

GridCard.propTypes = {
  order: orderShape.isRequired,
  onAction: PropTypes.func.isRequired,
  actions: PropTypes.arrayOf(actionShape),
  renderItems: PropTypes.func,
  renderExtra: PropTypes.func,
  isProcessing: PropTypes.bool
};

CompactCard.propTypes = {
  order: orderShape.isRequired,
  onAction: PropTypes.func.isRequired,
  actions: PropTypes.arrayOf(actionShape),
  isProcessing: PropTypes.bool
};

ListRow.propTypes = {
  order: orderShape.isRequired,
  onAction: PropTypes.func.isRequired,
  actions: PropTypes.arrayOf(actionShape),
  columns: PropTypes.arrayOf(PropTypes.string),
  isProcessing: PropTypes.bool
};

export default { GridCard, CompactCard, ListRow };
