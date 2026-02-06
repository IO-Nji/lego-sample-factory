/**
 * StatCardProgress - Type 3: Progress/Gauge Visualization
 * 
 * A statistics card with a visual progress bar, ideal for showing
 * completion percentages, capacity utilization, or goal progress.
 * 
 * @example
 * <StatCardProgress
 *   value={75}
 *   max={100}
 *   label="Production Capacity"
 *   detail="6 of 8 units"
 *   variant="success"
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardProgress = ({
  value,
  max = 100,
  label,
  detail,
  subDetail,
  variant = 'primary', // 'primary' | 'success' | 'warning' | 'danger'
  showPercentage = true,
  onClick,
  loading = false,
  className = '',
  formatValue = (v, m) => `${v}/${m}`,
}) => {
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);
  
  // Auto-determine variant based on percentage if not explicitly set
  const getAutoVariant = () => {
    if (variant !== 'primary') return variant;
    if (percentage >= 90) return 'danger';
    if (percentage >= 70) return 'warning';
    return 'primary';
  };

  const effectiveVariant = getAutoVariant();

  return (
    <div 
      className={`${styles.progressCard} ${loading ? styles.loading : ''} ${onClick ? styles.clickable : ''} ${className}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.progressCardHeader}>
        <span className={styles.progressCardLabel}>{label}</span>
        <span className={styles.progressCardValue}>
          {showPercentage ? `${Math.round(percentage)}%` : formatValue(value, max)}
        </span>
      </div>
      
      <div className={styles.progressBarContainer}>
        <div 
          className={`${styles.progressBarFill} ${styles[effectiveVariant]}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      
      <div className={styles.progressCardDetail}>
        {detail && <span>{detail}</span>}
        {subDetail && <span>{subDetail}</span>}
      </div>
    </div>
  );
};

export default StatCardProgress;
