import React from 'react';
import PropTypes from 'prop-types';
import styles from './Tooltip.module.css';

/**
 * Tooltip Component - Standardized hover tooltip for consistent UX
 * 
 * Design matches WorkstationCard tooltip styling with:
 * - Landscape-oriented shape (min-width 200px, max-width 400px)
 * - Gradient background (blue by default, with variants)
 * - Smooth hover animations
 * - Arrow pointing to trigger element
 * 
 * Usage:
 * 1. Simple text tooltip:
 *    <Tooltip content="This is a tooltip">
 *      <button>Hover me</button>
 *    </Tooltip>
 * 
 * 2. Rich content with header and grid:
 *    <Tooltip
 *      header="Task Details"
 *      icon="📋"
 *      content={[
 *        { label: 'Status', value: 'SCHEDULED' },
 *        { label: 'Duration', value: '30 min' }
 *      ]}
 *    >
 *      <div>Task element</div>
 *    </Tooltip>
 * 
 * @param {ReactNode} children - The trigger element
 * @param {string|Array} content - Tooltip content (string or array of {label, value} objects)
 * @param {string} header - Optional header text
 * @param {string} icon - Optional emoji icon for header
 * @param {string} position - 'top' | 'bottom' | 'left' | 'right'
 * @param {string} variant - 'default' | 'warning' | 'success' | 'info' | 'danger'
 * @param {string} status - Optional status for badge display
 * @param {string} className - Additional CSS classes
 */
function Tooltip({ 
  children, 
  content, 
  header, 
  icon,
  position = 'top', 
  variant = 'default',
  status,
  className = ''
}) {
  // Get position class
  const positionClass = {
    top: styles.positionTop,
    bottom: styles.positionBottom,
    left: styles.positionLeft,
    right: styles.positionRight
  }[position] || styles.positionTop;

  // Get variant class
  const variantClass = {
    warning: styles.variantWarning,
    success: styles.variantSuccess,
    info: styles.variantInfo,
    danger: styles.variantDanger
  }[variant] || '';

  // Get status badge class
  const getStatusBadgeClass = (statusValue) => {
    const statusClasses = {
      PENDING: styles.statusPending,
      SCHEDULED: styles.statusScheduled,
      IN_PROGRESS: styles.statusInProgress,
      COMPLETED: styles.statusCompleted,
      CANCELLED: styles.statusCancelled
    };
    return statusClasses[statusValue] || styles.statusPending;
  };

  // Render content based on type
  const renderContent = () => {
    // If content is an array, render as grid
    if (Array.isArray(content)) {
      return (
        <div className={styles.tooltipGrid}>
          {content.map((item, index) => (
            <React.Fragment key={index}>
              <span className={styles.tooltipLabel}>{item.label}:</span>
              <span className={styles.tooltipValue}>{item.value}</span>
            </React.Fragment>
          ))}
        </div>
      );
    }
    
    // Otherwise render as plain text
    return <div className={styles.tooltipBody}>{content}</div>;
  };

  return (
    <div className={`${styles.tooltipContainer} ${className}`}>
      {children}
      <div className={`${styles.tooltip} ${positionClass} ${variantClass}`}>
        {/* Header */}
        {header && (
          <div className={styles.tooltipHeader}>
            {icon && <span className={styles.tooltipIcon}>{icon}</span>}
            <span>{header}</span>
            {status && (
              <span className={`${styles.statusBadge} ${getStatusBadgeClass(status)}`}>
                {status.replace('_', ' ')}
              </span>
            )}
          </div>
        )}
        
        {/* Content */}
        {renderContent()}
      </div>
    </div>
  );
}

Tooltip.propTypes = {
  children: PropTypes.node.isRequired,
  content: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
    PropTypes.arrayOf(PropTypes.shape({
      label: PropTypes.string.isRequired,
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number, PropTypes.node]).isRequired
    }))
  ]).isRequired,
  header: PropTypes.string,
  icon: PropTypes.string,
  position: PropTypes.oneOf(['top', 'bottom', 'left', 'right']),
  variant: PropTypes.oneOf(['default', 'warning', 'success', 'info', 'danger']),
  status: PropTypes.string,
  className: PropTypes.string
};

export default Tooltip;
