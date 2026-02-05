/**
 * StatCardMulti - Type 7: Grouped Multi-Metric Card
 * 
 * A card that displays multiple related metrics in a single container.
 * Ideal for showing related KPIs together (e.g., order status breakdown).
 * 
 * @example
 * <StatCardMulti
 *   title="Order Breakdown"
 *   icon="📊"
 *   metrics={[
 *     { label: 'Pending', value: 12 },
 *     { label: 'In Progress', value: 8 },
 *     { label: 'Completed', value: 45 },
 *     { label: 'Cancelled', value: 2 },
 *   ]}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardMulti = ({
  title,
  icon,
  metrics = [], // Array of { label, value, color? }
  columns = 2,  // Grid columns for metrics (2 or 4)
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
}) => {
  // Support 2 or 4 column layouts
  const gridColumns = columns === 4 ? 4 : 2;
  
  return (
    <div 
      className={`
        ${styles.multiCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.multiCardHeader}>
        {icon && <div className={styles.multiCardIcon}>{icon}</div>}
        <span className={styles.multiCardTitle}>{title}</span>
      </div>
      
      <div 
        className={styles.multiCardGrid}
        style={{ gridTemplateColumns: `repeat(${gridColumns}, 1fr)` }}
      >
        {metrics.map((metric, index) => (
          <div key={index} className={styles.multiCardItem}>
            <div 
              className={styles.multiCardItemValue}
              style={metric.color ? { color: metric.color } : undefined}
            >
              {formatValue(metric.value)}
            </div>
            <div className={styles.multiCardItemLabel}>{metric.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default StatCardMulti;
