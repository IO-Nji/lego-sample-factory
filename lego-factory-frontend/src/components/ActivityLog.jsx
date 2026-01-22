import PropTypes from 'prop-types';
import { useEffect, useRef } from 'react';
import styles from './ActivityLog.module.css';
import { getWorkstationColor, getOrderTypeColor } from '../utils/activityLogConfig';

/**
 * ActivityLog - Enhanced component for displaying activity notifications/logs
 * 
 * Features:
 * - Standardized activity log display with color coding
 * - Configurable title and icon
 * - Auto-scrolling notification list
 * - Clear all functionality
 * - Compact layout for dashboard sidebar
 * - Color-coded workstation names
 * - Color-coded order types
 * - Automatic login event tracking via useActivityLog hook
 * 
 * Usage with useActivityLog hook:
 * const { notifications, addNotification, clearNotifications } = useActivityLog(session, 'PLANT-WH');
 * 
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
  emptyMessage = 'No activity yet',
  enableColorCoding = true
}) {
  const containerRef = useRef(null);
  const prevCountRef = useRef(notifications.length);

  // Auto-scroll to top when new notification arrives
  useEffect(() => {
    if (notifications.length > prevCountRef.current && containerRef.current) {
      containerRef.current.scrollTop = 0;
    }
    prevCountRef.current = notifications.length;
  }, [notifications.length]);

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit',
      hour12: false 
    });
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'success': return '✓';
      case 'error': return '✗';
      case 'warning': return '⚠';
      case 'info':
      default: return 'ℹ';
    }
  };

  const renderNotificationMessage = (notification) => {
    if (!enableColorCoding) {
      return <span>{notification.message}</span>;
    }

    const { message, station, orderType } = notification;
    
    // Extract order numbers from message (e.g., CUST-123, WH-456, PROD-789)
    const orderNumberPattern = /([A-Z]+-\d+)/g;
    const parts = message.split(orderNumberPattern);
    
    return (
      <span>
        {parts.map((part, index) => {
          // Check if this part is an order number
          if (part.match(/^[A-Z]+-\d+$/)) {
            const color = orderType ? getOrderTypeColor(orderType) : getOrderTypeColor('default');
            return (
              <span key={index} style={{ color, fontWeight: '600' }}>
                {part}
              </span>
            );
          }
          return <span key={index}>{part}</span>;
        })}
      </span>
    );
  };

  return (
    <div className={styles.activityLogContainer}>
      <div className={styles.header}>
        <h2 className={styles.title}>{icon} {title}</h2>
        {onClear && notifications.length > 0 && (
          <button 
            onClick={onClear} 
            className={styles.clearButton}
            aria-label="Clear notifications"
          >
            Clear All
          </button>
        )}
      </div>
      
      <div 
        ref={containerRef}
        className={styles.notificationList}
        style={{ 
          maxHeight: `calc(${maxVisible} * 2.5rem)`,
          minHeight: '10rem'
        }}
      >
        {notifications.length > 0 ? (
          notifications.map((notification) => {
            const stationColor = enableColorCoding && notification.station 
              ? getWorkstationColor(notification.station) 
              : 'var(--color-text-secondary)';
            
            return (
              <div 
                key={notification.id} 
                className={`${styles.notificationItem} ${styles[notification.type]}`}
              >
                <div className={styles.notificationHeader}>
                  <span className={styles.icon}>
                    {getNotificationIcon(notification.type)}
                  </span>
                  <span 
                    className={styles.station}
                    style={enableColorCoding ? { color: stationColor, fontWeight: '600' } : {}}
                  >
                    {notification.station}
                  </span>
                  <span className={styles.timestamp}>
                    {formatTimestamp(notification.timestamp)}
                  </span>
                </div>
                <div className={styles.message}>
                  {renderNotificationMessage(notification)}
                </div>
              </div>
            );
          })
        ) : (
          <div className={styles.emptyState}>
            {emptyMessage}
          </div>
        )}
      </div>
    </div>
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
    station: PropTypes.string,
    orderType: PropTypes.string,
    orderNumber: PropTypes.string,
    userName: PropTypes.string
  })).isRequired,
  onClear: PropTypes.func,
  maxVisible: PropTypes.number,
  emptyMessage: PropTypes.string,
  enableColorCoding: PropTypes.bool
};

export default ActivityLog;
