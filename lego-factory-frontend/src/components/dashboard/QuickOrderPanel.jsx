/**
 * QuickOrderPanel - Compact Order Form for Panel Row
 * 
 * A streamlined order creation form designed to fit within the
 * DashboardPanelRow. Shows top items with quantity selectors
 * and a single submit button.
 */

import { useState } from 'react';
import PropTypes from 'prop-types';
import '../../styles/DashboardPanels.css';

function QuickOrderPanel({
  icon = '➕',
  title = 'Quick Order',
  items = [],
  onSubmit,
  loading = false,
  themeClass = '',
  getItemName,
  getItemStock,
  maxDisplayItems = 4,
  submitLabel = 'Create',
  itemLabel = 'Product'
}) {
  const [quantities, setQuantities] = useState({});

  // Get total items selected
  const getTotalItems = () => {
    return Object.values(quantities).reduce((sum, qty) => sum + (qty || 0), 0);
  };

  // Handle quantity change
  const handleQuantityChange = (itemId, delta) => {
    setQuantities(prev => {
      const current = prev[itemId] || 0;
      const newQty = Math.max(0, current + delta);
      return { ...prev, [itemId]: newQty };
    });
  };

  // Handle submit
  const handleSubmit = () => {
    if (getTotalItems() > 0 && onSubmit) {
      onSubmit(quantities);
      setQuantities({}); // Reset after submit
    }
  };

  // Display items (limited)
  const displayItems = items.slice(0, maxDisplayItems);
  const hasMore = items.length > maxDisplayItems;

  return (
    <div className={`dashboard-panel dashboard-panel--quick-order ${themeClass}`}>
      {/* Panel Header */}
      <div className="panel-header">
        <div className="panel-header__left">
          <span className="panel-icon">{icon}</span>
          <h3 className="panel-title">{title}</h3>
        </div>
        {getTotalItems() > 0 && (
          <span className="panel-badge panel-badge--info">
            {getTotalItems()} items
          </span>
        )}
      </div>

      {/* Compact Item List */}
      <div className="quick-order-items">
        {displayItems.map(item => {
          const itemId = item.id;
          const name = getItemName ? getItemName(item) : item.name || `${itemLabel} #${itemId}`;
          const stock = getItemStock ? getItemStock(itemId) : null;
          const qty = quantities[itemId] || 0;

          return (
            <div key={itemId} className="quick-order-row">
              <span className="quick-order-name" title={name}>
                {name}
                {stock !== null && (
                  <span className={`quick-order-stock ${stock < 5 ? 'quick-order-stock--low' : ''}`}>
                    ({stock})
                  </span>
                )}
              </span>
              <div className="quick-order-qty">
                <button
                  type="button"
                  className="quick-order-btn"
                  onClick={() => handleQuantityChange(itemId, -1)}
                  disabled={qty === 0 || loading}
                >
                  −
                </button>
                <span className={`quick-order-value ${qty > 0 ? 'quick-order-value--active' : ''}`}>
                  {qty}
                </span>
                <button
                  type="button"
                  className="quick-order-btn"
                  onClick={() => handleQuantityChange(itemId, 1)}
                  disabled={loading}
                >
                  +
                </button>
              </div>
            </div>
          );
        })}
        {hasMore && (
          <div className="quick-order-more">
            +{items.length - maxDisplayItems} more items...
          </div>
        )}
      </div>

      {/* Submit Button */}
      <button
        type="button"
        className={`quick-order-submit ${getTotalItems() > 0 ? 'quick-order-submit--active' : ''}`}
        onClick={handleSubmit}
        disabled={getTotalItems() === 0 || loading}
      >
        {loading ? '...' : `${submitLabel} ${getTotalItems() > 0 ? `(${getTotalItems()})` : ''}`}
      </button>
    </div>
  );
}

QuickOrderPanel.propTypes = {
  icon: PropTypes.string,
  title: PropTypes.string,
  items: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    name: PropTypes.string
  })),
  onSubmit: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  themeClass: PropTypes.string,
  getItemName: PropTypes.func,
  getItemStock: PropTypes.func,
  maxDisplayItems: PropTypes.number,
  submitLabel: PropTypes.string,
  itemLabel: PropTypes.string
};

export default QuickOrderPanel;
