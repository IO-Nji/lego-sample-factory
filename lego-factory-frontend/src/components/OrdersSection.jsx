import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { Input } from './index';
import styles from './StandardDashboardLayout.module.css';

/**
 * OrdersSection - Generic component for displaying orders with filter/search/sort
 * 
 * Features:
 * - Configurable title and icon
 * - Status filtering with customizable options
 * - Search by order number or notes
 * - Sorting by date, order number, or status
 * - Grid layout for order cards
 * - Supports any order card component
 * 
 * Usage:
 * <OrdersSection
 *   title="Customer Orders"
 *   icon="📋"
 *   orders={orders}
 *   filterOptions={[
 *     { value: 'ALL', label: 'All Orders' },
 *     { value: 'PENDING', label: 'Pending' }
 *   ]}
 *   sortOptions={[
 *     { value: 'orderDate', label: 'Order Date' },
 *     { value: 'orderNumber', label: 'Order Number' }
 *   ]}
 *   renderCard={(order) => <CustomerOrderCard order={order} />}
 * />
 */
function OrdersSection({
  title = 'Orders',
  icon = '📋',
  orders = [],
  filterOptions = [
    { value: 'ALL', label: 'All Orders' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ],
  sortOptions = [
    { value: 'orderDate', label: 'Order Date' },
    { value: 'orderNumber', label: 'Order Number' },
    { value: 'status', label: 'Status' }
  ],
  renderCard,
  searchPlaceholder = 'Search...',
  emptyMessage = 'No orders found'
}) {
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState(sortOptions[0]?.value || 'orderDate');

  useEffect(() => {
    let result = orders;
    
    // Apply status filter
    if (filterStatus !== 'ALL') {
      result = result.filter(order => order.status === filterStatus);
    }
    
    // Apply search
    if (searchTerm) {
      result = result.filter(order => 
        order.orderNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        order.notes?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    // Apply sorting
    result = [...result].sort((a, b) => {
      switch(sortBy) {
        case 'orderDate':
          return new Date(b.orderDate) - new Date(a.orderDate);
        case 'orderNumber':
          return a.orderNumber.localeCompare(b.orderNumber);
        case 'status':
          return a.status.localeCompare(b.status);
        default:
          return 0;
      }
    });
    
    setFilteredOrders(result);
  }, [filterStatus, orders, searchTerm, sortBy]);

  return (
    <>
      <div className={styles.cardHeader} style={{ overflow: 'visible' }}>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 'var(--spacing-3)',
          flexWrap: 'wrap'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-3)' }}>
            <h2 className={styles.cardTitle}>{icon} {title}</h2>
            <span style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)' }}>
              {filteredOrders.length} {filteredOrders.length === 1 ? 'order' : 'orders'}
            </span>
          </div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-3)' }}>
            {/* Filter Dropdown */}
            {filterOptions.length > 1 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-2)' }}>
                <label style={{ 
                  fontSize: '0.75rem',
                  fontWeight: '700',
                  color: 'var(--color-text-primary)'
                }}>
                  Filter Orders:
                </label>
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  style={{ 
                    minWidth: '130px', 
                    fontSize: '0.875rem',
                    padding: '0.375rem 0.5rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--color-bg-primary)',
                    color: 'var(--color-text-primary)'
                  }}
                >
                  {filterOptions.map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            )}
            
            {/* Sort Dropdown */}
            {sortOptions.length > 1 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-2)' }}>
                <label style={{ 
                  fontSize: '0.75rem',
                  fontWeight: '700',
                  color: 'var(--color-text-primary)',
                  whiteSpace: 'nowrap'
                }}>
                  Sort By:
                </label>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  style={{ 
                    minWidth: '130px', 
                    fontSize: '0.875rem',
                    padding: '0.375rem 0.5rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--color-bg-primary)',
                    color: 'var(--color-text-primary)'
                  }}
                >
                  {sortOptions.map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            )}
            
            {/* Search Input */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-2)' }}>
              <label style={{ 
                fontSize: '0.75rem',
                fontWeight: '700',
                color: 'var(--color-text-primary)',
                whiteSpace: 'nowrap'
              }}>
                Search:
              </label>
              <Input
                type="text"
                placeholder={searchPlaceholder}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ width: '120px', fontSize: '0.875rem' }}
              />
            </div>
          </div>
        </div>
      </div>
      <div className={styles.cardContent}>
        {Array.isArray(filteredOrders) && filteredOrders.length > 0 ? (
          <div className={styles.ordersGrid}>
            {filteredOrders.map((order) => renderCard(order))}
          </div>
        ) : (
          <div style={{ 
            textAlign: 'center', 
            padding: 'var(--spacing-12)', 
            color: 'var(--color-text-secondary)',
            fontSize: '1rem'
          }}>
            {emptyMessage}
          </div>
        )}
      </div>
    </>
  );
}

OrdersSection.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  orders: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    orderNumber: PropTypes.string,
    orderDate: PropTypes.string,
    status: PropTypes.string.isRequired,
    notes: PropTypes.string
  })).isRequired,
  filterOptions: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })),
  sortOptions: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })),
  renderCard: PropTypes.func.isRequired,
  searchPlaceholder: PropTypes.string,
  emptyMessage: PropTypes.string
};

export default OrdersSection;
