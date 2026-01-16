import React, { useEffect, useRef } from 'react';
import styles from './Notification.module.css';

/**
 * Notification Component - Console-like notification display
 * 
 * Features:
 * - Displays up to 5 visible notifications
 * - New notifications appear at top
 * - Scrollable for older notifications
 * - Auto-scrolls to top on new notification
 * - Supports notification types: success, info, warning, error
 * 
 * @param {Array} notifications - Array of notification objects
 *   [{id, message, type, timestamp, station}]
 * @param {string} title - Optional title (default: "Notifications")
 * @param {number} maxVisible - Max visible lines without scrolling (default: 5)
 */
const Notification = ({ 
  notifications = [], 
  title = "Notifications",
  maxVisible = 5,
  onClear = null
}) => {
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
  
  const formatLoginTimestamp = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const time = date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false 
    });
    const dateStr = date.toLocaleDateString('en-US', {
      day: '2-digit',
      month: '2-digit'
    });
    return `${time} ${dateStr}`;
  };
  
  const isLoginNotification = (notification) => {
    return notification.message === 'User logged in' || 
           notification.message === 'Logged in';
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

  return (
    <div className={styles.notificationContainer}>
      <div className={styles.header}>
        <h3 className={styles.title}>{title}</h3>
        {onClear && notifications.length > 0 && (
          <button 
            className={styles.clearButton}
            onClick={onClear}
            title="Clear all notifications"
          >
            Clear
          </button>
        )}
      </div>
      
      <div 
        ref={containerRef}
        className={styles.notificationList}
        style={{ maxHeight: `calc(${maxVisible} * 2.5rem)` }}
      >
        {notifications.length === 0 ? (
          <div className={styles.emptyState}>
            <span className={styles.emptyIcon}>📭</span>
            <p className={styles.emptyText}>No notifications</p>
          </div>
        ) : (
          notifications.map((notification) => {
            const isLogin = isLoginNotification(notification);
            return (
              <div 
                key={notification.id}
                className={`${styles.notificationItem} ${styles[notification.type || 'info']}`}
              >
                {isLogin ? (
                  <>
                    <span className={styles.message}>
                      Logged in {formatLoginTimestamp(notification.timestamp)}
                    </span>
                    {notification.station && (
                      <span className={styles.station}>
                        [{notification.station}]
                      </span>
                    )}
                  </>
                ) : (
                  <>
                    <span className={styles.icon}>
                      {getNotificationIcon(notification.type)}
                    </span>
                    <span className={styles.timestamp}>
                      {formatTimestamp(notification.timestamp)}
                    </span>
                    <span className={styles.message}>
                      {notification.message}
                    </span>
                    {notification.station && (
                      <span className={styles.station}>
                        [{notification.station}]
                      </span>
                    )}
                  </>
                )}
              </div>
            );
          })
        )}
      </div>
      
      {notifications.length > maxVisible && (
        <div className={styles.scrollHint}>
          ↓ Scroll for more ({notifications.length - maxVisible} older)
        </div>
      )}
    </div>
  );
};

export default Notification;
