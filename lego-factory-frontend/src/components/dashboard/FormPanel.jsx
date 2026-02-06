/**
 * FormPanel - Order Creation Form Component
 * 
 * Compact form for creating orders in warehouse dashboards.
 * Displays items with quantity inputs and submit button.
 */

import PropTypes from 'prop-types';
import '../../styles/WorkstationDashboard.css';

function FormPanel({
  title = 'New Order',
  icon = '➕',
  items = [],
  selectedItems = {},
  onItemChange,
  onSubmit,
  loading = false,
  submitText = 'Create Order',
  loadingText = 'Creating...',
  emptyMessage = 'No items available',
  getItemName,
  getItemStock,
  showStock = true
}) {
  const handleQuantityChange = (itemId, value) => {
    const quantity = parseInt(value, 10) || 0;
    onItemChange?.(itemId, quantity);
  };

  const getTotalItems = () => {
    return Object.values(selectedItems).reduce((sum, qty) => sum + qty, 0);
  };

  const hasSelection = getTotalItems() > 0;

  return (
    <>
      <div className="ws-form-header">
        <h4 className="ws-form-title">
          <span>{icon}</span>
          {title}
        </h4>
        {hasSelection && (
          <span style={{
            fontSize: '0.6875rem',
            fontWeight: 600,
            padding: '0.125rem 0.375rem',
            background: 'rgba(59, 130, 246, 0.1)',
            color: '#2563eb',
            borderRadius: '4px'
          }}>
            {getTotalItems()} items
          </span>
        )}
      </div>
      
      <div className="ws-form-content">
        {items.length > 0 ? (
          items.map((item) => {
            const itemId = item.id;
            const displayName = getItemName ? getItemName(item) : item.name || `Item #${itemId}`;
            const stockLevel = getItemStock ? getItemStock(itemId) : null;
            
            return (
              <div key={itemId} className="ws-form-row">
                <span className="ws-form-item-name" title={displayName}>
                  {displayName}
                </span>
                {showStock && stockLevel !== null && (
                  <span style={{
                    fontSize: '0.625rem',
                    padding: '0.125rem 0.375rem',
                    background: stockLevel > 10 ? 'rgba(16, 185, 129, 0.1)' : stockLevel > 5 ? 'rgba(245, 158, 11, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                    color: stockLevel > 10 ? '#059669' : stockLevel > 5 ? '#d97706' : '#dc2626',
                    borderRadius: '3px',
                    marginRight: '0.5rem',
                    fontWeight: 600
                  }}>
                    {stockLevel}
                  </span>
                )}
                <input
                  type="number"
                  min="0"
                  value={selectedItems[itemId] || 0}
                  onChange={(e) => handleQuantityChange(itemId, e.target.value)}
                  className="ws-form-qty-input"
                  aria-label={`Quantity for ${displayName}`}
                />
              </div>
            );
          })
        ) : (
          <div style={{
            padding: '1.5rem 1rem',
            textAlign: 'center',
            color: '#94a3b8',
            fontSize: '0.75rem'
          }}>
            {emptyMessage}
          </div>
        )}
      </div>

      <div className="ws-form-footer">
        <button
          onClick={onSubmit}
          disabled={loading || !hasSelection}
          className="ws-form-submit"
        >
          {loading ? loadingText : submitText}
        </button>
      </div>
    </>
  );
}

FormPanel.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  items: PropTypes.array,
  selectedItems: PropTypes.object,
  onItemChange: PropTypes.func,
  onSubmit: PropTypes.func,
  loading: PropTypes.bool,
  submitText: PropTypes.string,
  loadingText: PropTypes.string,
  emptyMessage: PropTypes.string,
  getItemName: PropTypes.func,
  getItemStock: PropTypes.func,
  showStock: PropTypes.bool
};

export default FormPanel;
