import PropTypes from 'prop-types';
import styles from './WorkstationCard.module.css';

/**
 * WorkstationCard (Small) - Reusable component for displaying workstation information
 * 
 * Features:
 * - Displays workstation icon, name, and tooltip
 * - Hover effects with smooth animations
 * - Status indication with color coding (blue = idle, yellow = active)
 * - Customizable tooltip content
 * - Click handler support for navigation/actions
 * 
 * @param {string} icon - Emoji icon for the workstation
 * @param {string} name - Display name of the workstation
 * @param {string} tooltip - Descriptive text shown on hover
 * @param {string} status - 'idle' (blue, no orders) or 'active' (yellow, orders present)
 * @param {function} onClick - Optional click handler
 * @param {string} className - Additional CSS classes
 * @param {object} style - Inline styles
 * @param {string} layout - 'vertical' (default) or 'horizontal' (icon left, text right)
 */
function WorkstationCard({ icon, name, tooltip, status = 'idle', onClick, className = '', style = {}, layout = 'vertical' }) {
  const statusClass = status === 'active' ? styles.statusActive : styles.statusIdle;
  const layoutClass = layout === 'horizontal' ? styles.horizontalLayout : '';
  
  return (
    <div 
      className={`${styles.stationCard} ${statusClass} ${layoutClass} ${className}`}
      data-tooltip={tooltip}
      onClick={onClick}
      style={style}
      role={onClick ? 'button' : 'presentation'}
      tabIndex={onClick ? 0 : -1}
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
  );
}

WorkstationCard.propTypes = {
  icon: PropTypes.string.isRequired,
  name: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  tooltip: PropTypes.string.isRequired,
  status: PropTypes.oneOf(['idle', 'active']),
  onClick: PropTypes.func,
  className: PropTypes.string,
  style: PropTypes.object,
  layout: PropTypes.oneOf(['vertical', 'horizontal']),
};

export default WorkstationCard;

