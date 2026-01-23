import PropTypes from 'prop-types';
import styles from './StandardDashboardLayout.module.css';

/**
 * InventoryTable - Generic component for displaying inventory items
 * 
 * Features:
 * - Compact table layout with custom styling
 * - Color-coded quantity display based on stock levels
 * - Configurable item type filtering
 * - Custom display name function support
 * - Empty state message
 * - Standardized for use across all dashboard pages
 * 
 * Usage:
 * <InventoryTable
 *   title="Inventory"
 *   icon="📦"
 *   inventory={inventoryItems}
 *   itemType="PRODUCT"
 *   getItemDisplayName={(itemId) => products.find(p => p.id === itemId)?.name}
 *   getStatusColor={(quantity) => quantity < 5 ? 'red' : 'green'}
 * />
 */
function InventoryTable({
  title = 'Inventory',
  icon = '📦',
  inventory = [],
  itemType = null, // Filter by item type if specified
  getItemDisplayName = (itemId) => `Item ${itemId}`,
  getStatusColor = (quantity) => {
    if (quantity === 0) return '#ef4444';
    if (quantity < 5) return '#f59e0b';
    if (quantity < 10) return '#eab308';
    return '#10b981';
  },
  emptyMessage = 'No inventory items',
  columnHeaders = { item: 'Product', quantity: 'Qty' },
  // Legacy props for backward compatibility
  items = [],
  getItemName,
  headerColor = 'green'
}) {
  // Filter inventory by item type if specified
  const filteredInventory = itemType 
    ? inventory.filter(item => item.itemType === itemType)
    : inventory;

  // Handle legacy getItemName prop
  const displayNameFunction = (itemIdOrItem) => {
    // Support both itemId (number) and item (object) for flexibility
    const itemId = typeof itemIdOrItem === 'object' ? itemIdOrItem.itemId : itemIdOrItem;
    
    if (getItemName && typeof itemIdOrItem === 'object') {
      return getItemName(itemIdOrItem);
    }
    if (getItemDisplayName) {
      return getItemDisplayName(itemId);
    }
    // Fallback to legacy items array lookup
    const matchedItem = items.find(i => i.id === itemId);
    return matchedItem?.name || `Item #${itemId}`;
  };

  return (
    <>
      <div className={styles.cardHeader} style={{ marginBottom: 'var(--spacing-2)', paddingBottom: 'var(--spacing-2)' }}>
        <h2 className={styles.cardTitle} style={{ fontSize: '1rem' }}>{icon} {title}</h2>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>
        <table className="dashboard-table" style={{ fontSize: '0.75rem' }}>
          <thead>
            <tr>
              <th style={{ padding: '0.375rem 0.5rem' }}>{columnHeaders.item}</th>
              <th style={{ width: '60px', padding: '0.375rem 0.5rem', textAlign: 'center' }}>
                {columnHeaders.quantity}
              </th>
            </tr>
          </thead>
          <tbody>
            {filteredInventory.length > 0 ? (
              filteredInventory.map((item) => (
                <tr key={item.id}>
                  <td style={{ fontSize: '0.75rem', padding: '0.375rem 0.5rem' }}>
                    {displayNameFunction(item.itemId)}
                  </td>
                  <td style={{ 
                    fontSize: '0.75rem', 
                    padding: '0.375rem 0.5rem', 
                    textAlign: 'center',
                    color: getStatusColor(item.quantity),
                    fontWeight: item.quantity < 5 ? '600' : 'normal'
                  }}>
                    {item.quantity}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="2" style={{ 
                  textAlign: 'center', 
                  padding: '1rem', 
                  color: 'var(--color-text-secondary)', 
                  fontSize: '0.75rem' 
                }}>
                  {emptyMessage}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

InventoryTable.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  inventory: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    itemId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    itemType: PropTypes.string.isRequired,
    quantity: PropTypes.number.isRequired
  })).isRequired,
  itemType: PropTypes.string,
  getItemDisplayName: PropTypes.func,
  getStatusColor: PropTypes.func,
  emptyMessage: PropTypes.string,
  columnHeaders: PropTypes.shape({
    item: PropTypes.string,
    quantity: PropTypes.string
  }),
  // Legacy props for backward compatibility
  items: PropTypes.array,
  getItemName: PropTypes.func,
  headerColor: PropTypes.string
};

export default InventoryTable;
