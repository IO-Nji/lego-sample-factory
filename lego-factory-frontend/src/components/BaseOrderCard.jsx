import PropTypes from 'prop-types';
import Button from './Button';
import '../styles/CustomerOrderCard.css';

/**
 * BaseOrderCard Component
 * 
 * A reusable base component that provides a consistent layout structure for all order card types.
 * Ensures uniform card dimensions, spacing, and field positioning across the application.
 * 
 * Layout Structure:
 * - Header: Order number + Status badge
 * - Subtitle (optional): Secondary order reference
 * - Body:
 *   - Items list (for orders with line items)
 *   - Info sections (label-value pairs for metadata)
 *   - Priority badge (optional)
 *   - Notes (optional)
 *   - Date and notification row
 * - Footer: Action buttons
 * 
 * @param {string} orderNumber - Display order number (e.g., "CUST-001", "#12345")
 * @param {string} status - Order status text
 * @param {string} statusClass - CSS class for status styling
 * @param {string} cardType - Additional CSS class for card type (e.g., "customer-order", "production-control")
 * @param {string} subtitle - Optional subtitle text (e.g., "Production Order #123")
 * @param {Array} items - Array of order items {name, quantity, secondaryQuantity, quantityLabel, secondaryLabel, statusColor, hasStock}
 * @param {Array} infoSections - Array of info section objects {rows: [{label, value}]}
 * @param {string} priority - Optional priority level (HIGH, MEDIUM, LOW)
 * @param {Object} scenarioBadge - Optional scenario badge {text, variant}
 * @param {string} notes - Optional notes text
 * @param {string} dateText - Formatted date string to display
 * @param {Object} notificationMessage - Optional notification {text, type}
 * @param {Array} actions - Array of action buttons {label, variant, size, onClick, show}
 * @param {number} maxItems - Maximum number of items to display (default: 4)
 */
function BaseOrderCard({
  orderNumber,
  status,
  statusClass,
  cardType = '',
  subtitle,
  items = [],
  infoSections = [],
  priority,
  scenarioBadge,
  notes,
  dateText,
  notificationMessage,
  actions = [],
  maxItems = 4
}) {

  return (
    <div className={`customer-order-card ${cardType} status-${statusClass}`}>
      {/* Header with Order Number and Status Badge */}
      <div className="order-card-header">
        <span className="order-number">{orderNumber}</span>
        <span className={`order-status-badge ${statusClass}`}>
          {status}
        </span>
      </div>

      {/* Optional Subtitle */}
      {subtitle && (
        <div className="order-subtitle">
          {subtitle}
        </div>
      )}

      {/* Body with Order Details */}
      <div className="order-card-body">
        {/* Items List (for orders with line items) */}
        {items && items.length > 0 && (
          <div className="order-items-list">
            {items.slice(0, maxItems).map((item, idx) => (
              <div key={idx} className="order-item">
                <div className="item-name">{item.name}</div>
                <div className="item-quantity-section">
                  {item.secondaryQuantity !== undefined ? (
                    // Dual quantity display (e.g., "3 / 5" for fulfilled/requested)
                    <>
                      {item.quantityLabel && <span className="quantity-label">{item.quantityLabel}: </span>}
                      <span 
                        className="item-quantity primary" 
                        style={item.statusColor ? { color: item.statusColor } : {}}
                      >
                        {item.quantity}
                      </span>
                      <span className="quantity-separator"> / </span>
                      {item.secondaryLabel && <span className="quantity-label">{item.secondaryLabel}: </span>}
                      <span className="item-quantity secondary">
                        {item.secondaryQuantity}
                      </span>
                    </>
                  ) : (
                    // Single quantity display
                    <span 
                      className="item-quantity" 
                      style={item.statusColor ? { color: item.statusColor } : {}}
                    >
                      {item.quantity}
                    </span>
                  )}
                  {item.hasStock !== undefined && (
                    <span className="stock-indicator">
                      {item.hasStock ? '✓' : '⚠'}
                    </span>
                  )}
                </div>
              </div>
            ))}
            {items.length > maxItems && (
              <div className="order-item">
                <div className="item-name more-items">
                  +{items.length - maxItems} more item{items.length - maxItems > 1 ? 's' : ''}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Priority and Scenario Badges */}
        {(priority || scenarioBadge) && (
          <div className="order-badges">
            {priority && (
              <span className={`priority-badge priority-${priority.toLowerCase()}`}>
                {priority}
              </span>
            )}
            {scenarioBadge && (
              <span className={`scenario-badge scenario-${scenarioBadge.variant || 'info'}`}>
                {scenarioBadge.text}
              </span>
            )}
          </div>
        )}

        {/* Info Sections (label-value pairs) */}
        {infoSections && infoSections.length > 0 && infoSections.map((section, sectionIdx) => (
          <div key={sectionIdx} className="order-info-section">
            {section.rows && section.rows.map((row, rowIdx) => (
              <div key={rowIdx} className="info-row">
                <span className="info-label">{row.label}:</span>
                <span className="info-value">{row.value}</span>
              </div>
            ))}
          </div>
        ))}

        {/* Notes */}
        {notes && (
          <div className="order-notes">
            <p>{notes}</p>
          </div>
        )}

        {/* Date and Notification Row */}
        {(dateText || notificationMessage) && (
          <div className="order-date-notification-row">
            {notificationMessage && (
              <div className={`order-notification-message ${notificationMessage.type || 'info'}`}>
                {notificationMessage.text}
              </div>
            )}
            {dateText && (
              <div className="order-date">
                {dateText}
              </div>
            )}
          </div>
        )}

        {/* Empty state message */}
        {!items?.length && !infoSections?.length && !notes && !dateText && (
          <p className="no-items">No details available</p>
        )}
      </div>

      {/* Footer with Action Buttons */}
      {actions && actions.length > 0 && (
        <div className="order-card-footer">
          {actions.map((action, idx) => (
            action.show && (
              <Button
                key={idx}
                variant={action.variant || 'primary'}
                size={action.size || 'small'}
                onClick={action.onClick}
              >
                {action.label}
              </Button>
            )
          ))}
        </div>
      )}
    </div>
  );
}

BaseOrderCard.propTypes = {
  orderNumber: PropTypes.string.isRequired,
  status: PropTypes.string.isRequired,
  statusClass: PropTypes.string.isRequired,
  cardType: PropTypes.string,
  subtitle: PropTypes.string,
  items: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    quantity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    secondaryQuantity: PropTypes.number,
    quantityLabel: PropTypes.string,
    secondaryLabel: PropTypes.string,
    statusColor: PropTypes.string,
    hasStock: PropTypes.bool
  })),
  infoSections: PropTypes.arrayOf(PropTypes.shape({
    rows: PropTypes.arrayOf(PropTypes.shape({
      label: PropTypes.string.isRequired,
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired
    }))
  })),
  priority: PropTypes.string,
  scenarioBadge: PropTypes.shape({
    text: PropTypes.string.isRequired,
    variant: PropTypes.string
  }),
  notes: PropTypes.string,
  dateText: PropTypes.string,
  notificationMessage: PropTypes.shape({
    text: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['success', 'info', 'warning', 'error'])
  }),
  actions: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string.isRequired,
    variant: PropTypes.string,
    size: PropTypes.string,
    onClick: PropTypes.func.isRequired,
    show: PropTypes.bool.isRequired
  })),
  maxItems: PropTypes.number
};

export default BaseOrderCard;
