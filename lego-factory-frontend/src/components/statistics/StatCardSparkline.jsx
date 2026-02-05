/**
 * StatCardSparkline - Type 4: Mini Chart/Sparkline
 * 
 * A statistics card with a small bar chart visualization showing
 * data trends over time (e.g., last 7 days, hourly distribution).
 * 
 * @example
 * <StatCardSparkline
 *   value={42}
 *   label="Orders This Week"
 *   data={[5, 8, 3, 12, 6, 4, 4]}
 *   trend={{ direction: 'up', value: 8 }}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardSparkline = ({
  value,
  label,
  data = [], // Array of numbers for sparkline bars
  trend,
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
  barColor = 'primary', // 'primary' | 'success' | 'warning' | 'danger'
}) => {
  // Normalize data for bar heights (0-100%)
  const maxValue = Math.max(...data, 1);
  const normalizedData = data.map(d => (d / maxValue) * 100);

  const getTrendBadgeClass = () => {
    if (!trend) return '';
    switch (trend.direction) {
      case 'up': return styles.trendBadgeUp;
      case 'down': return styles.trendBadgeDown;
      default: return styles.trendBadgeNeutral;
    }
  };

  const getTrendIcon = () => {
    if (!trend) return '';
    switch (trend.direction) {
      case 'up': return '↑';
      case 'down': return '↓';
      default: return '→';
    }
  };

  return (
    <div 
      className={`${styles.sparklineCard} ${loading ? styles.loading : ''} ${onClick ? styles.clickable : ''} ${className}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.sparklineCardHeader}>
        <span className={styles.sparklineCardLabel}>{label}</span>
        {trend && (
          <span className={`${styles.trendBadge} ${getTrendBadgeClass()}`}>
            {getTrendIcon()} {typeof trend.value === 'number' ? `${Math.abs(trend.value)}%` : trend.value}
          </span>
        )}
      </div>
      
      <div className={styles.sparklineCardValue}>
        {formatValue(value)}
      </div>
      
      {data.length > 0 && (
        <div className={styles.sparklineContainer}>
          {normalizedData.map((height, index) => (
            <div
              key={index}
              className={styles.sparklineBar}
              style={{ height: `${Math.max(height, 4)}%` }}
              title={`${data[index]}`}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default StatCardSparkline;
