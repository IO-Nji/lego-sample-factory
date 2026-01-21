import PropTypes from 'prop-types';
import { Notification } from './index';
import styles from './StandardDashboardLayout.module.css';

/**
 * ActivityLog - Generic component for displaying activity notifications/logs
 * 
 * Features:
 * - Standardized activity log display
 * - Configurable title and icon
 * - Auto-scrolling notification list
 * - Clear all functionality
 * - Compact layout for dashboard sidebar
 * 
 * Usage:
 * <ActivityLog
 *   title="Warehouse Activity"
 *   icon="📢"
 *   notifications={notifications}
 *   onClear={clearNotifications}
 *   maxVisible={50}
 * />
 */
function ActivityLog({
  title = 'Activity Log',
  icon = '📢',
  notifications = [],
  onClear,
  maxVisible = 50,
  emptyMessage = 'No activity yet'
}) {
  return (
    <>
      <div className={styles.cardHeader} style={{ marginBottom: 'var(--spacing-2)', paddingBottom: 'var(--spacing-2)' }}>
        <h2 className={styles.cardTitle} style={{ fontSize: '1rem' }}>{icon} {title}</h2>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>
        {notifications.length > 0 ? (
          <Notification 
            notifications={notifications}
            title=""
            maxVisible={maxVisible}
            onClear={onClear}
            hideHeader={true}
          />
        ) : (
          <div style={{ 
            textAlign: 'center', 
            padding: 'var(--spacing-8)', 
            color: 'var(--color-text-secondary)',
            fontSize: '0.875rem'
          }}>
            {emptyMessage}
          </div>
        )}
      </div>
    </>
  );
}

ActivityLog.propTypes = {
  title: PropTypes.string,
  icon: PropTypes.string,
  notifications: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    message: PropTypes.string.isRequired,
    type: PropTypes.string,
    timestamp: PropTypes.string,
    station: PropTypes.string
  })).isRequired,
  onClear: PropTypes.func,
  maxVisible: PropTypes.number,
  emptyMessage: PropTypes.string
};

export default ActivityLog;
