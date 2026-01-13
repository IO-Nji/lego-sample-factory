import PropTypes from 'prop-types';
import styles from './StatCard.module.css';

/**
 * StatCard Component - Design System Compliant
 * 
 * Displays a key metric with optional icon, trend indicator, and threshold alerts.
 * Used for dashboard statistics and KPI displays.
 * 
 * @component
 * @example
 * // Basic stat card
 * <StatCard
 *   value={142}
 *   label="Total Orders"
 *   variant="primary"
 * />
 * 
 * // With icon and trend
 * <StatCard
 *   value="$12,450"
 *   label="Revenue"
 *   icon="💰"
 *   trend={{ value: 12.5, direction: 'up' }}
 *   variant="success"
 * />
 * 
 * // With threshold alert
 * <StatCard
 *   value={5}
 *   label="Low Stock Items"
 *   variant="danger"
 *   threshold={10}
 *   thresholdType="low"
 * />
 * 
 * // Clickable stat card
 * <StatCard
 *   value={23}
 *   label="Pending Orders"
 *   onClick={() => navigateTo('/orders')}
 * />
 */
function StatCard({
  value,
  label,
  variant = 'primary',
  icon = null,
  trend = null,
  threshold = null,
  thresholdType = 'high',
  onClick,
  className = '',
  compact = false,
}) {
  // Build class names
  const cardClasses = [
    styles.statCard,
    styles[variant],
    compact && styles.compact,
    onClick && styles.clickable,
    className
  ].filter(Boolean).join(' ');

  // Determine value styling based on threshold
  let valueClass = styles.value;
  if (threshold !== null && typeof value === 'number') {
    if (thresholdType === 'low' && value <= threshold) {
      valueClass = `${styles.value} ${styles.valueDanger}`;
    } else if (thresholdType === 'high' && value >= threshold) {
      valueClass = `${styles.value} ${styles.valueWarning}`;
    }
  }

  // Determine trend styling
  let trendClass = styles.trend;
  if (trend) {
    if (trend.direction === 'up') {
      trendClass = `${styles.trend} ${styles.trendUp}`;
    } else if (trend.direction === 'down') {
      trendClass = `${styles.trend} ${styles.trendDown}`;
    } else {
      trendClass = `${styles.trend} ${styles.trendNeutral}`;
    }
  }

  const handleClick = () => {
    if (onClick && typeof onClick === 'function') {
      onClick();
    }
  };

  const handleKeyDown = (e) => {
    if (onClick && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      onClick();
    }
  };

  return (
    <div
      className={cardClasses}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      {icon && <div className={styles.icon}>{icon}</div>}
      
      <div className={valueClass}>{value}</div>
      <div className={styles.label}>{label}</div>
      
      {trend && (
        <div className={trendClass}>
          <span>{trend.direction === 'up' ? '↑' : trend.direction === 'down' ? '↓' : '→'}</span>
          <span>{Math.abs(trend.value)}%</span>
        </div>
      )}
    </div>
  );
}

StatCard.propTypes = {
  /** The numeric or string value to display */
  value: PropTypes.oneOfType([
    PropTypes.number,
    PropTypes.string
  ]).isRequired,
  
  /** Label describing the statistic */
  label: PropTypes.string.isRequired,
  
  /** Color variant */
  variant: PropTypes.oneOf([
    'primary',
    'secondary',
    'success',
    'warning',
    'danger',
    'info'
  ]),
  
  /** Optional icon (emoji or React element) */
  icon: PropTypes.node,
  
  /** Trend data: { value: number, direction: 'up' | 'down' | 'neutral' } */
  trend: PropTypes.shape({
    value: PropTypes.number.isRequired,
    direction: PropTypes.oneOf(['up', 'down', 'neutral']).isRequired,
  }),
  
  /** Threshold value for alerts */
  threshold: PropTypes.number,
  
  /** Threshold comparison type */
  thresholdType: PropTypes.oneOf(['low', 'high']),
  
  /** Click handler (makes card clickable) */
  onClick: PropTypes.func,
  
  /** Additional CSS classes */
  className: PropTypes.string,
  
  /** Use compact layout */
  compact: PropTypes.bool,
};

export default StatCard;
