/**
 * StatCardHero - Type 10: Featured/Hero Stat
 * 
 * A prominent, visually striking card for the most important KPI.
 * Features gradient backgrounds, larger typography, and animations.
 * 
 * @example
 * <StatCardHero
 *   value={1247}
 *   label="Total Orders"
 *   subtitle="Last 30 days"
 *   icon="📦"
 *   trend={{ direction: 'up', value: 23 }}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardHero = ({
  value,
  label,
  subtitle,
  icon,
  trend,
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
}) => {
  return (
    <div 
      className={`
        ${styles.heroCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.heroCardHeader}>
        {icon && <div className={styles.heroCardIcon}>{icon}</div>}
        <span className={styles.heroCardTitle}>{label}</span>
      </div>
      
      <div className={styles.heroCardValue}>
        {formatValue(value)}
      </div>
      
      <div className={styles.heroCardSubtitle}>
        {subtitle}
        {trend && (
          <span className={`${styles.heroCardTrend} ${styles[trend.direction]}`}>
            {trend.direction === 'up' ? '↑' : trend.direction === 'down' ? '↓' : '→'}
            {typeof trend.value === 'number' ? `${Math.abs(trend.value)}%` : trend.value}
          </span>
        )}
      </div>
    </div>
  );
};

export default StatCardHero;
