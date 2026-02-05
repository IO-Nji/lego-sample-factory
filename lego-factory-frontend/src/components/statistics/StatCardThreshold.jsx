/**
 * StatCardThreshold - Type 5: Stock/Threshold Alert Card
 * 
 * A statistics card designed for inventory monitoring with visual
 * threshold indicators. Shows critical/low/ok status with alerts.
 * 
 * @example
 * <StatCardThreshold
 *   value={3}
 *   label="Gear Modules"
 *   threshold={10}
 *   criticalThreshold={5}
 *   max={50}
 *   icon="⚙️"
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardThreshold = ({
  value,
  label,
  threshold = 10,        // Warning threshold
  criticalThreshold = 5, // Critical threshold
  max = 100,            // Maximum for progress indicator
  icon,
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
  thresholdType = 'min', // 'min' (alert when below) | 'max' (alert when above)
}) => {
  // Determine status based on thresholds
  const getStatus = () => {
    if (thresholdType === 'min') {
      if (value <= criticalThreshold) return 'critical';
      if (value <= threshold) return 'low';
      return 'ok';
    } else {
      if (value >= criticalThreshold) return 'critical';
      if (value >= threshold) return 'low';
      return 'ok';
    }
  };

  const status = getStatus();
  const isAlert = status === 'critical';
  const isWarning = status === 'low';

  const getStatusLabel = () => {
    switch (status) {
      case 'critical': return 'Critical';
      case 'low': return 'Low Stock';
      default: return 'In Stock';
    }
  };

  const getStatusIcon = () => {
    switch (status) {
      case 'critical': return '⚠️';
      case 'low': return '⚡';
      default: return '✓';
    }
  };

  // Calculate percentage for visual indicator
  const percentage = Math.min((value / max) * 100, 100);
  const thresholdPercentage = (threshold / max) * 100;
  const criticalPercentage = (criticalThreshold / max) * 100;

  // Get fill color based on status
  const getFillColor = () => {
    switch (status) {
      case 'critical': return 'var(--stat-color-danger)';
      case 'low': return 'var(--stat-color-warning)';
      default: return 'var(--stat-color-success)';
    }
  };

  return (
    <div 
      className={`
        ${styles.thresholdCard} 
        ${isAlert ? styles.alert : ''} 
        ${isWarning ? styles.warningState : ''} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.thresholdCardHeader}>
        {icon && <span className={styles.thresholdCardIcon}>{icon}</span>}
        <span className={`${styles.thresholdStatusBadge} ${styles[status]}`}>
          {getStatusIcon()} {getStatusLabel()}
        </span>
      </div>
      
      <div className={`${styles.thresholdCardValue} ${styles[status]}`}>
        {formatValue(value)}
      </div>
      
      <div className={styles.thresholdCardLabel}>
        {label}
      </div>
      
      <div className={styles.thresholdIndicator}>
        <span>0</span>
        <div className={styles.thresholdIndicatorLine}>
          <div 
            className={styles.thresholdIndicatorFill}
            style={{ 
              width: `${percentage}%`,
              background: getFillColor()
            }}
          />
          {/* Critical threshold marker */}
          <div 
            className={styles.thresholdIndicatorMark}
            style={{ 
              left: `${criticalPercentage}%`,
              background: 'var(--stat-color-danger)'
            }}
            title={`Critical: ${criticalThreshold}`}
          />
          {/* Warning threshold marker */}
          <div 
            className={styles.thresholdIndicatorMark}
            style={{ 
              left: `${thresholdPercentage}%`,
              background: 'var(--stat-color-warning)'
            }}
            title={`Warning: ${threshold}`}
          />
        </div>
        <span>{max}</span>
      </div>
    </div>
  );
};

export default StatCardThreshold;
