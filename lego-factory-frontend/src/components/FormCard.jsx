import PropTypes from 'prop-types';
import { Button } from './index';
import styles from './StandardDashboardLayout.module.css';

/**
 * FormCard - Generic component for order/item creation forms
 * 
 * Features:
 * - Compact table-based form layout
 * - Configurable title and icon
 * - Support for quantity-based item selection
 * - Customizable submit button
 * - Empty state handling
 * - Standardized styling for all dashboards
 * 
 * Usage:
 * <FormCard
 *   title="New Order"
 *   icon="➕"
 *   items={products}
 *   selectedItems={selectedProducts}
 *   onItemChange={(itemId, quantity) => setSelectedProducts({...selectedProducts, [itemId]: quantity})}
 *   onSubmit={handleCreateOrder}
 *   loading={isCreating}
 *   buttonText="Create Order"
 *   getItemDisplayName={(item) => item.name}
 * />
 */
function FormCard({
  title = 'New Order',
  icon = '➕',
  items = [],
  selectedItems = {},
  onItemChange,
  onSubmit,
  loading = false,
  buttonText = 'Submit',
  getItemDisplayName = (item) => item.name || `Item ${item.id}`,
  emptyMessage = 'No items available',
  columnHeaders = { item: 'Product', quantity: 'Qty' }
}) {
  
  const handleQuantityChange = (itemId, value) => {
    const quantity = parseInt(value, 10) || 0;
    if (onItemChange) {
      onItemChange(itemId, quantity);
    }
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
              <th style={{ width: '60px', padding: '0.375rem 0.5rem' }}>{columnHeaders.quantity}</th>
            </tr>
          </thead>
          <tbody>
            {items.length > 0 ? (
              items.map((item) => (
                <tr key={item.id}>
                  <td style={{ fontSize: '0.75rem', padding: '0.375rem 0.5rem' }}>
                    {getItemDisplayName(item)}
                  </td>
                  <td style={{ padding: '0.25rem' }}>
                    <input
                      type="number"
                      min="0"
                      value={selectedItems[item.id] || ''}
                      onChange={(e) => handleQuantityChange(item.id, e.target.value)}
                      style={{
                        width: '100%',
                        padding: '0.25rem 0.375rem',
                        border: '1px solid #d1d5db',
                        borderRadius: '0.25rem',
                        fontSize: '0.75rem'
                      }}
                    />
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="2" style={{ 
                  textAlign: 'center', 
                  padding: '0.75rem', 
                  color: '#9ca3af', 
                  fontSize: '0.75rem' 
                }}>
                  {emptyMessage}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div style={{ 
        marginTop: 'var(--spacing-2)', 
        paddingTop: 'var(--spacing-2)', 
        borderTop: '1px solid var(--color-border)' 
      }}>
        <Button 
          onClick={onSubmit} 
          disabled={loading}
          variant="primary"
          size="small"
          style={{ width: '100%', padding: '0.5rem', fontSize: '0.75rem' }}
        >
          {loading ? 'Processing...' : buttonText}
        </Button>
      </div>
    </>
  );
}

FormCard.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  items: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired
  })).isRequired,
  selectedItems: PropTypes.object.isRequired,
  onItemChange: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  loading: PropTypes.bool,
  buttonText: PropTypes.string,
  getItemDisplayName: PropTypes.func,
  emptyMessage: PropTypes.string,
  columnHeaders: PropTypes.shape({
    item: PropTypes.string,
    quantity: PropTypes.string
  })
};

export default FormCard;
