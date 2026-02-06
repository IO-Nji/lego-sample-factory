/**
 * StatCardTrend - Type 2: Metric with Trend Indicator
 * 
 * A statistics card that displays a primary value with trend comparison.
 * Features a trend badge showing percentage change (up/down/neutral).
 * 
 * @example
 * <StatCardTrend
 *   value={156}
 *   label="Orders Today"
 *   trend={12.5}
 *   trendDirection="up"
 *   comparison="vs yesterday"
 *   icon="📦"
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardTrend = ({
  value,
  label,
  trend,
  trendDirection = 'neutral', // 'up' | 'down' | 'neutral'
  comparison,
  icon,
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
}) => {
  const getTrendBadgeClass = () => {
    switch (trendDirection) {
      case 'up': return styles.trendBadgeUp;
      case 'down': return styles.trendBadgeDown;
      default: return styles.trendBadgeNeutral;
    }
  };

  const getTrendIcon = () => {
    switch (trendDirection) {
      case 'up': return '↑';
      case 'down': return '↓';
      default: return '→';
    }
  };

  return (
    <div 
      className={`${styles.trendCard} ${loading ? styles.loading : ''} ${onClick ? styles.clickable : ''} ${className}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.trendCardHeader}>
        {icon && (
          <div className={styles.trendCardIcon}>
            {icon}
          </div>
        )}
        {(trend !== undefined && trend !== null) && (
          <span className={`${styles.trendBadge} ${getTrendBadgeClass()}`}>
            {getTrendIcon()} {typeof trend === 'number' ? `${Math.abs(trend)}%` : trend}
          </span>
        )}
      </div>
      
      <div className={styles.trendCardValue}>
        {formatValue(value)}
      </div>
      
      <div className={styles.trendCardLabel}>
        {label}
      </div>
      
      {comparison && (
        <div className={styles.trendCardComparison}>
          {comparison}
        </div>
      )}
    </div>
  );
};

export default StatCardTrend;
