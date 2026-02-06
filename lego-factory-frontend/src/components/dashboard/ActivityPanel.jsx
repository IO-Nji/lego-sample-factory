/**
 * ActivityPanel - Unified Activity Log Component
 * 
 * Consistent activity feed for all dashboards (workstations, control, admin).
 * 
 * MESSAGE FORMAT:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ [ICON] [MESSAGE]                                  [HH:MM:SS]│
 * └─────────────────────────────────────────────────────────────┘
 * 
 * Examples:
 *   ✓  ORD-1234 confirmed                               14:32:15
 *   ▶  Injection task started                           14:30:00
 *   👤 warehouse_operator logged in                     14:28:45
 */

import PropTypes from 'prop-types';
import { ACTIVITY_TYPES } from '../../utils/activityLogConfig';
import '../../styles/ActivityPanel.css';

function ActivityPanel({ 
  title = 'Activity',
  notifications = [], 
  onClear,
  maxItems = 8,
  compact = false,
  showStation = false
}) {
  
  /**
   * Get icon for notification type/activity
   */
  const getIcon = (notification) => {
    // Use activity type icon if available
    if (notification.activityType && ACTIVITY_TYPES[notification.activityType]) {
      return ACTIVITY_TYPES[notification.activityType].icon;
    }
    // Fallback to type-based icons
    switch (notification.type) {
      case 'success': return '✓';
      case 'warning': return '⚠';
      case 'error': return '✕';
      case 'info': 
      default: return 'ℹ';
    }
  };

  /**
   * Get accent color for notification
   */
  const getAccentColor = (notification) => {
    // Use activity type color if available
    if (notification.activityType && ACTIVITY_TYPES[notification.activityType]) {
      return ACTIVITY_TYPES[notification.activityType].color;
    }
    // Use station color if provided
    if (notification.stationColor) {
      return notification.stationColor;
    }
    // Fallback to type-based colors
    const typeColors = {
      success: '#10b981',
      warning: '#f59e0b',
      error: '#ef4444',
      info: '#3b82f6'
    };
    return typeColors[notification.type] || '#64748b';
  };

  /**
   * Format timestamp as HH:MM:SS
   */
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit', 
      second: '2-digit',
      hour12: false 
    });
  };

  /**
   * Get relative time for older entries
   */
  const getRelativeTime = (timestamp) => {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000);
    
    if (diff < 60) return 'now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h`;
    return formatTime(timestamp);
  };

  // Display items - limit in compact mode
  const visibleItems = notifications.slice(0, maxItems);
  const hiddenCount = notifications.length - visibleItems.length;

  return (
    <div className={`activity-panel ${compact ? 'activity-panel--compact' : ''}`}>
      {/* Header */}
      <div className="activity-panel__header">
        <h4 className="activity-panel__title">
          <span className="activity-panel__icon">📋</span>
          {title}
          {notifications.length > 0 && (
            <span className="activity-panel__count">{notifications.length}</span>
          )}
        </h4>
        {onClear && notifications.length > 0 && (
          <button 
            className="activity-panel__clear" 
            onClick={onClear}
            title="Clear activity log"
          >
            Clear
          </button>
        )}
      </div>
      
      {/* Activity List */}
      <div className="activity-panel__list">
        {visibleItems.length > 0 ? (
          <>
            {visibleItems.map((notification) => (
              <div 
                key={notification.id}
                className={`activity-item activity-item--${notification.type || 'info'}`}
                style={{ '--accent-color': getAccentColor(notification) }}
              >
                <span className="activity-item__icon">
                  {getIcon(notification)}
                </span>
                <div className="activity-item__content">
                  <span className="activity-item__message">
                    {notification.message}
                  </span>
                  {showStation && notification.station && (
                    <span className="activity-item__station">
                      {notification.stationName || notification.station}
                    </span>
                  )}
                </div>
                <span className="activity-item__time">
                  {compact ? getRelativeTime(notification.timestamp) : formatTime(notification.timestamp)}
                </span>
              </div>
            ))}
            {hiddenCount > 0 && (
              <div className="activity-panel__more">
                +{hiddenCount} more entries
              </div>
            )}
          </>
        ) : (
          <div className="activity-panel__empty">
            <span className="activity-panel__empty-icon">📭</span>
            <span>No recent activity</span>
          </div>
        )}
      </div>
    </div>
  );
}

ActivityPanel.propTypes = {
  title: PropTypes.string,
  notifications: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    message: PropTypes.string.isRequired,
    type: PropTypes.oneOf(['info', 'success', 'warning', 'error']),
    activityType: PropTypes.string,
    timestamp: PropTypes.string,
    station: PropTypes.string,
    stationName: PropTypes.string,
    stationColor: PropTypes.string,
  })),
  onClear: PropTypes.func,
  maxItems: PropTypes.number,
  compact: PropTypes.bool,
  showStation: PropTypes.bool,
};

export default ActivityPanel;
