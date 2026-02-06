/**
 * OrdersGrid - Enhanced Orders Display Component
 * 
 * Features:
 * - Three view modes: Grid (default), Compact, List
 * - Filter by status
 * - Sort by date, order number, status, priority
 * - Search functionality
 * - Consistent 3-column grid layout
 * 
 * Used across all workstation dashboards for uniform order display.
 */

import { useState, useEffect, useMemo, useCallback } from 'react';
import PropTypes from 'prop-types';
import '../../styles/WorkstationDashboard.css';

// View mode constants
export const VIEW_MODES = {
  GRID: 'grid',
  COMPACT: 'compact',
  LIST: 'list'
};

// Default filter options
export const DEFAULT_FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' }
];

// Default sort options
export const DEFAULT_SORT_OPTIONS = [
  { value: 'orderDate', label: 'Date (Newest)' },
  { value: 'orderDateAsc', label: 'Date (Oldest)' },
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
  { value: 'priority', label: 'Priority' }
];

function OrdersGrid({
  title = 'Orders',
  icon = '📋',
  orders = [],
  filterOptions = DEFAULT_FILTER_OPTIONS,
  sortOptions = DEFAULT_SORT_OPTIONS,
  renderCard,
  renderCompactCard,
  renderListItem,
  searchPlaceholder = 'Search orders...',
  emptyMessage = 'No orders found',
  emptySubtext = 'Orders will appear here when available',
  searchKeys = ['orderNumber', 'notes'],
  defaultView = VIEW_MODES.GRID,
  showViewToggle = true,
  showFilters = true,
  showSearch = true,
  showSort = true,
  onViewChange
}) {
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState(sortOptions[0]?.value || 'orderDate');
  const [viewMode, setViewMode] = useState(defaultView);

  // Notify parent of view changes
  useEffect(() => {
    if (onViewChange) {
      onViewChange(viewMode);
    }
  }, [viewMode, onViewChange]);

  // Memoized filtering and sorting
  const filteredOrders = useMemo(() => {
    let result = [...orders];
    
    // Apply status filter
    if (filterStatus !== 'ALL') {
      result = result.filter(order => {
        const status = order.status?.toUpperCase() || '';
        const filter = filterStatus.toUpperCase();
        // Handle variations like IN_PROGRESS vs PROGRESS
        return status === filter || status.includes(filter);
      });
    }
    
    // Apply search
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      result = result.filter(order => 
        searchKeys.some(key => {
          const value = order[key];
          return value?.toString().toLowerCase().includes(term);
        })
      );
    }
    
    // Apply sorting
    result.sort((a, b) => {
      switch(sortBy) {
        case 'orderDate':
          return new Date(b.orderDate || b.createdAt || 0) - new Date(a.orderDate || a.createdAt || 0);
        case 'orderDateAsc':
          return new Date(a.orderDate || a.createdAt || 0) - new Date(b.orderDate || b.createdAt || 0);
        case 'orderNumber': {
          const aNum = a.orderNumber || a.controlOrderNumber || a.productionOrderNumber || '';
          const bNum = b.orderNumber || b.controlOrderNumber || b.productionOrderNumber || '';
          return aNum.toString().localeCompare(bNum.toString());
        }
        case 'status':
          return (a.status || '').localeCompare(b.status || '');
        case 'priority': {
          const priorityOrder = { URGENT: 0, HIGH: 1, MEDIUM: 2, LOW: 3, NORMAL: 2 };
          return (priorityOrder[a.priority] ?? 999) - (priorityOrder[b.priority] ?? 999);
        }
        default:
          return 0;
      }
    });
    
    return result;
  }, [orders, filterStatus, searchTerm, sortBy, searchKeys]);

  // Determine which render function to use
  const renderOrderItem = useCallback((order) => {
    switch (viewMode) {
      case VIEW_MODES.COMPACT:
        return renderCompactCard ? renderCompactCard(order) : renderCard(order, true);
      case VIEW_MODES.LIST:
        return renderListItem ? renderListItem(order) : renderCard(order, false, true);
      default:
        return renderCard(order);
    }
  }, [viewMode, renderCard, renderCompactCard, renderListItem]);

  const gridClassName = useMemo(() => {
    switch (viewMode) {
      case VIEW_MODES.COMPACT:
        return 'ws-orders-grid ws-orders-grid--compact';
      case VIEW_MODES.LIST:
        return 'ws-orders-grid ws-orders-grid--list';
      default:
        return 'ws-orders-grid';
    }
  }, [viewMode]);

  return (
    <div className="ws-orders-panel ws-glass">
      {/* Header with Controls */}
      <div className="ws-orders-header">
        <div className="ws-orders-title-group">
          <h3 className="ws-orders-title">
            <span>{icon}</span>
            {title}
          </h3>
          <span className="ws-orders-count">
            {filteredOrders.length} {filteredOrders.length === 1 ? 'order' : 'orders'}
          </span>
        </div>
        
        <div className="ws-orders-controls">
          {/* Filter Dropdown */}
          {showFilters && filterOptions.length > 1 && (
            <div className="ws-control-group">
              <label className="ws-control-label">Filter:</label>
              <select
                className="ws-control-select"
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
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
          {showSort && sortOptions.length > 1 && (
            <div className="ws-control-group">
              <label className="ws-control-label">Sort:</label>
              <select
                className="ws-control-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
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
          {showSearch && (
            <div className="ws-control-group">
              <label className="ws-control-label">Search:</label>
              <input
                type="text"
                className="ws-control-search"
                placeholder={searchPlaceholder}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
          )}
          
          {/* View Toggle */}
          {showViewToggle && (
            <div className="ws-view-toggle">
              <button
                className={`ws-view-btn ${viewMode === VIEW_MODES.GRID ? 'ws-view-btn--active' : ''}`}
                onClick={() => setViewMode(VIEW_MODES.GRID)}
                title="Grid view"
              >
                ▦
              </button>
              <button
                className={`ws-view-btn ${viewMode === VIEW_MODES.COMPACT ? 'ws-view-btn--active' : ''}`}
                onClick={() => setViewMode(VIEW_MODES.COMPACT)}
                title="Compact view"
              >
                ▤
              </button>
              <button
                className={`ws-view-btn ${viewMode === VIEW_MODES.LIST ? 'ws-view-btn--active' : ''}`}
                onClick={() => setViewMode(VIEW_MODES.LIST)}
                title="List view"
              >
                ☰
              </button>
            </div>
          )}
        </div>
      </div>
      
      {/* Orders Content */}
      <div className="ws-orders-content">
        {filteredOrders.length > 0 ? (
          <div className={gridClassName}>
            {filteredOrders.map((order) => (
              <div key={order.id}>
                {renderOrderItem(order)}
              </div>
            ))}
          </div>
        ) : (
          <div className="ws-orders-empty">
            <div className="ws-orders-empty__icon">📭</div>
            <div className="ws-orders-empty__text">{emptyMessage}</div>
            <div className="ws-orders-empty__sub">{emptySubtext}</div>
          </div>
        )}
      </div>
    </div>
  );
}

OrdersGrid.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  orders: PropTypes.array.isRequired,
  filterOptions: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })),
  sortOptions: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })),
  renderCard: PropTypes.func.isRequired,
  renderCompactCard: PropTypes.func,
  renderListItem: PropTypes.func,
  searchPlaceholder: PropTypes.string,
  emptyMessage: PropTypes.string,
  emptySubtext: PropTypes.string,
  searchKeys: PropTypes.arrayOf(PropTypes.string),
  defaultView: PropTypes.oneOf(Object.values(VIEW_MODES)),
  showViewToggle: PropTypes.bool,
  showFilters: PropTypes.bool,
  showSearch: PropTypes.bool,
  showSort: PropTypes.bool,
  onViewChange: PropTypes.func
};

export default OrdersGrid;
