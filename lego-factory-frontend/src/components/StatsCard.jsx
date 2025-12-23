import React from 'react';
import PropTypes from 'prop-types';
import '../styles/StatsCard.css';

/**
 * StatsCard Component
 * 
 * A reusable statistics card component that displays a numerical value with a label.
 * Supports different variants for styling (default, pending, processing, completed).
 * 
 * @param {number|string} value - The statistic value to display
 * @param {string} label - The label describing the statistic
 * @param {string} variant - The card variant for styling (default, pending, processing, completed)
 * @param {function} onClick - Optional click handler for interactive cards
 * @param {string} className - Additional CSS classes
 */
function StatsCard({ value, label, variant = 'default', onClick, className = '' }) {
  const cardClasses = `stat-card ${variant !== 'default' ? variant : ''} ${className}`.trim();
  
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
      <div className="stat-value">{value}</div>
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
};

export default StatsCard;
