/**
 * StatCardCompact - Type 6: Inline Compact Stats
 * 
 * A minimal, inline statistics display for dense layouts.
 * Ideal for header bars, sidebars, or compact summaries.
 * 
 * @example
 * <StatCardCompact icon="📦" label="Total" value={156} variant="primary" />
 * <StatCardCompact icon="✓" label="Done" value={89} variant="success" />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardCompact = ({
  value,
  label,
  icon,
  variant = 'primary', // 'primary' | 'success' | 'warning' | 'danger' | 'info'
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
}) => {
  return (
    <div 
      className={`
        ${styles.compactCard} 
        ${styles[variant]} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {icon && (
        <div className={styles.compactCardIcon}>
          {icon}
        </div>
      )}
      
      <div className={styles.compactCardContent}>
        <div className={styles.compactCardLabel}>{label}</div>
        <div className={styles.compactCardValue}>{formatValue(value)}</div>
      </div>
    </div>
  );
};

export default StatCardCompact;
