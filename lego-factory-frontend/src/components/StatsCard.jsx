import React from 'react';
import PropTypes from 'prop-types';
import '../styles/StatsCard.css';

/**
 * StatsCard Component
 * 
 * A reusable statistics card component that displays a numerical value with a label.
 * Supports different variants for styling (default, pending, processing, completed).
 * Supports threshold-based color alerts (e.g., low stock in red)
 * 
 * @param {number|string} value - The statistic value to display
 * @param {string} label - The label describing the statistic
 * @param {string} variant - The card variant for styling (default, pending, processing, completed)
 * @param {function} onClick - Optional click handler for interactive cards
 * @param {string} className - Additional CSS classes
 * @param {number} threshold - Optional threshold for dynamic coloring
 * @param {string} thresholdType - Type of threshold check ('high' or 'low')
 */
function StatsCard({ value, label, variant = 'default', onClick, className = '', threshold, thresholdType = 'high' }) {
  const cardClasses = `stat-card ${variant !== 'default' ? variant : ''} ${className}`.trim();
  
  // Dynamic value coloring based on threshold
  let valueColorClass = '';
  if (threshold !== undefined && value !== undefined && value !== null) {
    // For 'low' threshold: trigger danger when value is AT OR BELOW threshold
    // For 'high' threshold: trigger warning when value is AT OR ABOVE threshold
    if (thresholdType === 'low' && value <= threshold) {
      valueColorClass = 'stat-value-danger'; // Low stock alert (red pulse)
    } else if (thresholdType === 'high' && value >= threshold) {
      valueColorClass = 'stat-value-warning'; // High pending orders alert (orange)
    }
  }
  
  const handleClick = () => {
    if (onClick && typeof onClick === 'function') {
      onClick();
    }
  };

  return (
    <div 
      className={cardClasses}
      onClick={handleClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyPress={onClick ? (e) => e.key === 'Enter' && handleClick() : undefined}
    >
      <div className={`stat-value ${valueColorClass}`}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

StatsCard.propTypes = {
  value: PropTypes.oneOfType([
    PropTypes.number,
    PropTypes.string
  ]).isRequired,
  label: PropTypes.string.isRequired,
  variant: PropTypes.oneOf(['default', 'pending', 'processing', 'completed']),
  onClick: PropTypes.func,
  className: PropTypes.string,
  threshold: PropTypes.number,
  thresholdType: PropTypes.oneOf(['high', 'low']),
};

export default StatsCard;
