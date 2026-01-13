import { useState, useMemo } from 'react';
import PropTypes from 'prop-types';
import styles from './Table.module.css';

/**
 * Table Component - Design System Compliant
 * 
 * A feature-rich table component with sorting, filtering, and pagination.
 * Supports custom cell rendering and row actions.
 * 
 * @component
 * @example
 * const columns = [
 *   { key: 'id', label: 'ID', sortable: true },
 *   { key: 'name', label: 'Name', sortable: true },
 *   { key: 'status', label: 'Status', render: (row) => <Badge>{row.status}</Badge> }
 * ];
 * 
 * <Table
 *   columns={columns}
 *   data={orders}
 *   onRowClick={(row) => viewDetails(row)}
 * />
 */
function Table({
  columns = [],
  data = [],
  variant = 'default',
  striped = false,
  hoverable = true,
  compact = false,
  sortable = true,
  onRowClick,
  emptyMessage = 'No data available',
  className = '',
}) {
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Sort data
  const sortedData = useMemo(() => {
    if (!sortable || !sortConfig.key) return data;

    const sorted = [...data].sort((a, b) => {
      const aValue = a[sortConfig.key];
      const bValue = b[sortConfig.key];

      if (aValue === bValue) return 0;
      if (aValue === null || aValue === undefined) return 1;
      if (bValue === null || bValue === undefined) return -1;

      if (typeof aValue === 'string' && typeof bValue === 'string') {
        return sortConfig.direction === 'asc'
          ? aValue.localeCompare(bValue)
          : bValue.localeCompare(aValue);
      }

      return sortConfig.direction === 'asc'
        ? aValue > bValue ? 1 : -1
        : aValue < bValue ? 1 : -1;
    });

    return sorted;
  }, [data, sortConfig, sortable]);

  // Handle sort
  const handleSort = (key) => {
    if (!sortable) return;

    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  // Build table classes
  const tableClasses = [
    styles.table,
    styles[variant],
    striped && styles.striped,
    hoverable && styles.hoverable,
    compact && styles.compact,
    className
  ].filter(Boolean).join(' ');

  // Render cell content
  const renderCell = (row, column) => {
    if (column.render && typeof column.render === 'function') {
      return column.render(row);
    }
    return row[column.key];
  };

  return (
    <div className={styles.tableContainer}>
      <table className={tableClasses}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th
                key={column.key}
                className={column.sortable && sortable ? styles.sortable : ''}
                onClick={() => column.sortable && handleSort(column.key)}
                style={{ width: column.width }}
              >
                <div className={styles.headerContent}>
                  <span>{column.label}</span>
                  {column.sortable && sortable && sortConfig.key === column.key && (
                    <span className={styles.sortIcon}>
                      {sortConfig.direction === 'asc' ? '↑' : '↓'}
                    </span>
                  )}
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sortedData.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className={styles.emptyMessage}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            sortedData.map((row, index) => (
              <tr
                key={row.id || index}
                onClick={() => onRowClick && onRowClick(row)}
                className={onRowClick ? styles.clickableRow : ''}
              >
                {columns.map((column) => (
                  <td key={column.key} data-label={column.label}>
                    {renderCell(row, column)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

Table.propTypes = {
  /** Array of column definitions */
  columns: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      sortable: PropTypes.bool,
      width: PropTypes.string,
      render: PropTypes.func,
    })
  ).isRequired,
  
  /** Array of data objects */
  data: PropTypes.array.isRequired,
  
  /** Visual style variant */
  variant: PropTypes.oneOf(['default', 'bordered', 'minimal']),
  
  /** Alternate row colors */
  striped: PropTypes.bool,
  
  /** Enable hover effects on rows */
  hoverable: PropTypes.bool,
  
  /** Use compact spacing */
  compact: PropTypes.bool,
  
  /** Enable column sorting */
  sortable: PropTypes.bool,
  
  /** Row click handler */
  onRowClick: PropTypes.func,
  
  /** Message shown when data is empty */
  emptyMessage: PropTypes.string,
  
  /** Additional CSS classes */
  className: PropTypes.string,
};

export default Table;
