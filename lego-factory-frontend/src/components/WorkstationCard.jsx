import { useState } from 'react';
import { createPortal } from 'react-dom';
import PropTypes from 'prop-types';
import styles from './WorkstationCard.module.css';

/**
 * WorkstationCard (Small) - Reusable component for displaying workstation information
 * 
 * Features:
 * - Displays workstation icon, name, and tooltip
 * - Hover effects with smooth animations
 * - Status indication with color coding (blue = idle, yellow = active)
 * - Structured tooltips with role description and username
 * - Click handler support for navigation/actions
 * 
 * @param {string} icon - Emoji icon for the workstation
 * @param {string} name - Display name of the workstation
 * @param {string|object} tooltip - Descriptive text or structured tooltip object
 *                                  Structured format: { description, username } or string
 * @param {string} status - 'idle' (blue, no orders) or 'active' (yellow, orders present)
 * @param {function} onClick - Optional click handler
 * @param {string} className - Additional CSS classes
 * @param {object} style - Inline styles
 * @param {string} layout - 'vertical' (default) or 'horizontal' (icon left, text right)
 */
function WorkstationCard({ icon, name, tooltip, status = 'idle', onClick, className = '', style = {}, layout = 'vertical' }) {
  const [isHovered, setIsHovered] = useState(false);
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 });
  
  const statusClass = status === 'active' ? styles.statusActive : styles.statusIdle;
  const layoutClass = layout === 'horizontal' ? styles.horizontalLayout : '';
  
  const handleMouseEnter = (e) => {
    setIsHovered(true);
    setTooltipPos({ x: e.clientX, y: e.clientY });
  };
  
  const handleMouseMove = (e) => {
    setTooltipPos({ x: e.clientX, y: e.clientY });
  };
  
  const handleMouseLeave = () => {
    setIsHovered(false);
  };

  // Parse tooltip - can be string or structured object
  const parseTooltip = (tooltip) => {
    if (!tooltip) return null;
    
    // If it's already an object, return as is
    if (typeof tooltip === 'object' && tooltip.description) {
      return tooltip;
    }
    
    // If it's a string with pipe delimiter, parse it
    if (typeof tooltip === 'string' && tooltip.includes('|')) {
      const parts = tooltip.split('|').map(p => p.trim());
      return {
        description: parts[0] || '',
        username: parts[1] || null
      };
    }
    
    // Default: treat as simple string description
    return { description: tooltip, username: null };
  };

  const tooltipData = parseTooltip(tooltip);

  // Tooltip rendered via Portal to escape stacking contexts
  const tooltipElement = isHovered && tooltipData ? createPortal(
    <div 
      className={styles.tooltip}
      style={{
        top: tooltipPos.y + 15,
        left: tooltipPos.x + 10,
      }}
    >
      {tooltipData.description && (
        <div className={styles.tooltipDescription}>{tooltipData.description}</div>
      )}
      {tooltipData.username && (
        <div className={styles.tooltipUserSection}>
          <div className={styles.tooltipUserLabel}>username:</div>
          <div className={styles.tooltipUsername}>{tooltipData.username}</div>
        </div>
      )}
    </div>,
    document.body
  ) : null;
  
  return (
    <>
      <div 
        className={`${styles.stationCard} ${statusClass} ${layoutClass} ${className}`}
        onClick={onClick}
        style={style}
        role={onClick ? 'button' : 'presentation'}
        tabIndex={onClick ? 0 : -1}
        onMouseEnter={handleMouseEnter}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        onKeyDown={onClick ? (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick(e);
          }
        } : undefined}
      >
        <div className={styles.stationIcon}>{icon}</div>
        <div className={styles.stationName}>{name}</div>
      </div>
      {tooltipElement}
    </>
  );
}

WorkstationCard.propTypes = {
  icon: PropTypes.string.isRequired,
  name: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  tooltip: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.shape({
      description: PropTypes.string,
      username: PropTypes.string
    })
  ]).isRequired,
  status: PropTypes.oneOf(['idle', 'active']),
  onClick: PropTypes.func,
  className: PropTypes.string,
  style: PropTypes.object,
  layout: PropTypes.oneOf(['vertical', 'horizontal']),
};

export default WorkstationCard;

