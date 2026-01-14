import PropTypes from 'prop-types';

/**
 * InventoryTable - Reusable inventory/stock display component
 * 
 * Displays stock items with quantity and status indicators
 * Supports different item types (products, modules, parts)
 * 
 * @param {Object} props
 * @param {string} props.title - Table header title (e.g., "Module Inventory", "Parts Stock")
 * @param {Array} props.inventory - Array of inventory items with {id, itemId, itemType, quantity}
 * @param {Array} props.items - Array of item definitions (products, modules, parts) to map names
 * @param {Function} props.getStatusColor - Function to determine status color based on quantity
 * @param {Function} props.getItemName - Function to get display name for item
 * @param {string} props.headerColor - Header color variant: 'blue' | 'green' | 'orange' | 'purple'
 */
function InventoryTable({
  title = "Current Inventory",
  inventory = [],
  items = [],
  getStatusColor,
  getItemName,
  headerColor = 'green'
}) {
  
  const getItemDisplayName = (item) => {
    if (getItemName) {
      return getItemName(item);
    }
    
    // Default: try to find matching item by ID
    const matchedItem = items.find(i => i.id === item.itemId);
    return matchedItem?.name || `Item #${item.itemId}`;
  };

  const getItemStatus = (quantity) => {
    if (quantity > 10) return 'In Stock';
    if (quantity > 0) return 'Low Stock';
    return 'Out of Stock';
  };

  const getDefaultStatusColor = (quantity) => {
    if (getStatusColor) {
      return getStatusColor(quantity);
    }
    
    // Default color logic
    if (quantity > 10) return '#16a34a'; // green
    if (quantity > 0) return '#ea580c'; // orange
    return '#dc2626'; // red
  };

  return (
    <>
      <div className={`dashboard-box-header dashboard-box-header-${headerColor}`}>
        <h2 className="dashboard-box-header-title">📦 {title}</h2>
      </div>
      <div style={{ overflowX: 'auto', maxHeight: '400px' }}>
        <table className="dashboard-table">
          <thead>
            <tr>
              <th>Item</th>
              <th>Quantity</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {inventory.length > 0 ? (
              inventory.map((item) => {
                const statusColor = getDefaultStatusColor(item.quantity || 0);
                
                return (
                  <tr key={item.id}>
                    <td>{getItemDisplayName(item)}</td>
                    <td style={{ fontWeight: 'bold' }}>{item.quantity || 0}</td>
                    <td style={{ color: statusColor, fontWeight: 'bold' }}>
                      {getItemStatus(item.quantity || 0)}
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="3" style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>
                  No inventory data available
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
  inventory: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.number,
    itemId: PropTypes.number,
    itemType: PropTypes.string,
    quantity: PropTypes.number
  })),
  items: PropTypes.array,
  getStatusColor: PropTypes.func,
  getItemName: PropTypes.func,
  headerColor: PropTypes.oneOf(['blue', 'green', 'orange', 'purple'])
};

export default InventoryTable;
