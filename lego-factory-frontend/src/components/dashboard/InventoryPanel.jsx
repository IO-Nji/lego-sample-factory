/**
 * Inventory Panel - Sidebar Component
 * 
 * Compact inventory table for workstation dashboards.
 * Uses the unified WorkstationDashboard CSS styling.
 */

import PropTypes from 'prop-types';
import '../../styles/WorkstationDashboard.css';

function InventoryPanel({ 
  title = 'Inventory',
  inventory = [],
  getItemName,
  lowStockThreshold = 10,
  criticalStockThreshold = 5,
  showActions = false,
  onAdjustStock
}) {
  // Determine stock status
  const getStockStatus = (quantity) => {
    if (quantity <= criticalStockThreshold) return 'critical';
    if (quantity <= lowStockThreshold) return 'low';
    return 'ok';
  };

  // Get display name for item
  // Priority: 1) item.itemName from API, 2) getItemName from masterdata, 3) fallback
  const getDisplayName = (item) => {
    // First, check if the API already provided itemName
    if (item.itemName) {
      return item.itemName;
    }
    if (item.name) {
      return item.name;
    }
    // Fall back to masterdata lookup if provided
    if (getItemName) {
      const name = getItemName(item.itemId, item.itemType);
      // Only use if it's not a placeholder (doesn't match pattern "#N")
      if (name && !name.match(/^(Product|Module|Part|Item) #\d+$/)) {
        return name;
      }
    }
    return `${item.itemType || 'Item'} #${item.itemId}`;
  };

  // Sort inventory: critical first, then low, then ok
  const sortedInventory = [...inventory].sort((a, b) => {
    const statusA = getStockStatus(a.quantity);
    const statusB = getStockStatus(b.quantity);
    const order = { critical: 0, low: 1, ok: 2 };
    return order[statusA] - order[statusB];
  });

  return (
    <>
      <div className="ws-sidebar-header">
        <h4 className="ws-sidebar-title">
          <span>📦</span>
          {title}
        </h4>
      </div>
      
      <div className="ws-sidebar-content">
        {sortedInventory.length > 0 ? (
          <table className="ws-inventory-table">
            <thead>
              <tr>
                <th>Item</th>
                <th style={{ textAlign: 'right' }}>Qty</th>
                <th>Status</th>
                {showActions && <th></th>}
              </tr>
            </thead>
            <tbody>
              {sortedInventory.map((item, index) => {
                const status = getStockStatus(item.quantity);
                return (
                  <tr key={item.id || `${item.itemType}-${item.itemId}` || index}>
                    <td>
                      <span className="ws-inventory-name" title={getDisplayName(item)}>
                        {getDisplayName(item)}
                      </span>
                    </td>
                    <td className={`ws-inventory-qty ws-inventory-qty--${status}`}>
                      {item.quantity}
                    </td>
                    <td>
                      <span className={`ws-inventory-status ws-inventory-status--${status}`}>
                        {status === 'critical' ? '⚠ Critical' : status === 'low' ? '⚠ Low' : '✓ OK'}
                      </span>
                    </td>
                    {showActions && onAdjustStock && (
                      <td>
                        <button 
                          onClick={() => onAdjustStock(item)}
                          style={{
                            padding: '0.125rem 0.375rem',
                            fontSize: '0.5625rem',
                            background: 'rgba(59, 130, 246, 0.1)',
                            color: '#2563eb',
                            border: 'none',
                            borderRadius: '3px',
                            cursor: 'pointer'
                          }}
                        >
                          Adjust
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        ) : (
          <div style={{ 
            padding: '1rem', 
            textAlign: 'center', 
            color: '#94a3b8',
            fontSize: '0.75rem'
          }}>
            No inventory data
          </div>
        )}
      </div>
    </>
  );
}

InventoryPanel.propTypes = {
  title: PropTypes.string,
  inventory: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    itemId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    itemType: PropTypes.string,
    itemName: PropTypes.string,
    quantity: PropTypes.number.isRequired
  })),
  getItemName: PropTypes.func,
  lowStockThreshold: PropTypes.number,
  criticalStockThreshold: PropTypes.number,
  showActions: PropTypes.bool,
  onAdjustStock: PropTypes.func
};

export default InventoryPanel;
