/**
 * ActivityLogSection - Real-time Activity Feed
 * 
 * Displays recent system activity with filtering and auto-refresh.
 * Supports different configurations for Admin vs Workstation views.
 * 
 * @example
 * <ActivityLogSection
 *   notifications={notifications}
 *   maxItems={10}
 *   showTimestamps
 *   showFilters
 *   onLoadMore={handleLoadMore}
 * />
 */

import React, { useState, useMemo } from 'react';
import PropTypes from 'prop-types';
import styles from './ActivityLogSection.module.css';

// Activity type configurations
const ACTIVITY_TYPES = {
  order_created: { icon: '📦', label: 'Order Created', color: '#4ecdc4' },
  order_confirmed: { icon: '✓', label: 'Confirmed', color: '#27ae60' },
  order_started: { icon: '▶', label: 'Started', color: '#3498db' },
  order_completed: { icon: '✅', label: 'Completed', color: '#2ecc71' },
  order_halted: { icon: '⏸', label: 'Halted', color: '#e67e22' },
  order_failed: { icon: '❌', label: 'Failed', color: '#e74c3c' },
  stock_updated: { icon: '📊', label: 'Stock Updated', color: '#9b59b6' },
  supply_fulfilled: { icon: '🚚', label: 'Supply Fulfilled', color: '#1abc9c' },
  production_scheduled: { icon: '📅', label: 'Scheduled', color: '#f39c12' },
  user_login: { icon: '👤', label: 'User Login', color: '#34495e' },
  system: { icon: '⚙️', label: 'System', color: '#7f8c8d' },
  warning: { icon: '⚠️', label: 'Warning', color: '#f1c40f' },
  error: { icon: '🔴', label: 'Error', color: '#e74c3c' },
};

const ActivityLogSection = ({
  notifications = [],
  maxItems = 10,
  showTimestamps = true,
  showFilters = false,
  showWorkstation = true,
  compact = false,
  title = 'Activity Log',
  emptyMessage = 'No recent activity',
  onLoadMore,
  hasMore = false,
  loading = false,
}) => {
  const [filter, setFilter] = useState('all');
  
  // Filter notifications
  const filteredNotifications = useMemo(() => {
    if (filter === 'all') return notifications;
    return notifications.filter(n => n.type === filter || n.category === filter);
  }, [notifications, filter]);

  // Limit displayed items
  const displayedNotifications = filteredNotifications.slice(0, maxItems);

  // Get unique types for filter dropdown
  const availableTypes = useMemo(() => {
    const types = new Set(notifications.map(n => n.type || n.category || 'system'));
    return Array.from(types);
  }, [notifications]);

  return (
    <div className={`${styles.activityLogSection} ${compact ? styles.compact : ''}`}>
      {/* Header */}
      <div className={styles.header}>
        <h3 className={styles.title}>{title}</h3>
        {showFilters && availableTypes.length > 1 && (
          <select 
            className={styles.filterSelect}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="all">All Activity</option>
            {availableTypes.map(type => (
              <option key={type} value={type}>
                {ACTIVITY_TYPES[type]?.label || type}
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Activity List */}
      <div className={styles.activityList}>
        {displayedNotifications.length === 0 ? (
          <div className={styles.emptyState}>
            <span className={styles.emptyIcon}>📭</span>
            <p>{emptyMessage}</p>
          </div>
        ) : (
          displayedNotifications.map((notification, index) => (
            <ActivityItem
              key={notification.id || index}
              notification={notification}
              showTimestamp={showTimestamps}
              showWorkstation={showWorkstation}
              compact={compact}
            />
          ))
        )}
        
        {loading && (
          <div className={styles.loadingMore}>
            <div className={styles.spinner} />
            Loading...
          </div>
        )}
      </div>

      {/* Load More */}
      {hasMore && onLoadMore && !loading && (
        <button className={styles.loadMoreButton} onClick={onLoadMore}>
          Load More Activity
        </button>
      )}
    </div>
  );
};

/**
 * Individual Activity Item
 */
const ActivityItem = ({ notification, showTimestamp, showWorkstation, compact }) => {
  const { 
    type = 'system', 
    message, 
    timestamp, 
    workstation,
    orderNumber,
    details,
  } = notification;

  const typeConfig = ACTIVITY_TYPES[type] || ACTIVITY_TYPES.system;
  
  // Format timestamp
  const formattedTime = timestamp ? formatRelativeTime(new Date(timestamp)) : null;

  return (
    <div className={`${styles.activityItem} ${compact ? styles.compactItem : ''}`}>
      <div 
        className={styles.activityIcon}
        style={{ backgroundColor: `${typeConfig.color}20`, color: typeConfig.color }}
      >
        {typeConfig.icon}
      </div>
      
      <div className={styles.activityContent}>
        <div className={styles.activityMain}>
          <span className={styles.activityMessage}>
            {message}
            {orderNumber && (
              <span className={styles.orderNumber}> #{orderNumber}</span>
            )}
          </span>
        </div>
        
        <div className={styles.activityMeta}>
          {showWorkstation && workstation && (
            <span className={styles.workstationBadge}>
              {workstation}
            </span>
          )}
          {showTimestamp && formattedTime && (
            <span className={styles.timestamp}>{formattedTime}</span>
          )}
        </div>
        
        {details && !compact && (
          <p className={styles.activityDetails}>{details}</p>
        )}
      </div>
    </div>
  );
};

/**
 * Format timestamp to relative time
 */
const formatRelativeTime = (date) => {
  const now = new Date();
  const diff = now - date;
  
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (seconds < 60) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  
  return date.toLocaleDateString();
};

// PropTypes
ActivityLogSection.propTypes = {
  notifications: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    type: PropTypes.string,
    message: PropTypes.string.isRequired,
    timestamp: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Date)]),
    workstation: PropTypes.string,
    orderNumber: PropTypes.string,
    details: PropTypes.string,
  })),
  maxItems: PropTypes.number,
  showTimestamps: PropTypes.bool,
  showFilters: PropTypes.bool,
  showWorkstation: PropTypes.bool,
  compact: PropTypes.bool,
  title: PropTypes.string,
  emptyMessage: PropTypes.string,
  onLoadMore: PropTypes.func,
  hasMore: PropTypes.bool,
  loading: PropTypes.bool,
};

ActivityItem.propTypes = {
  notification: PropTypes.object.isRequired,
  showTimestamp: PropTypes.bool,
  showWorkstation: PropTypes.bool,
  compact: PropTypes.bool,
};

export default ActivityLogSection;
