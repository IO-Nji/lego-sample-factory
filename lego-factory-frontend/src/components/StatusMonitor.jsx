import React from 'react';
import '../styles/Chart.css';

/**
 * StatusMonitor - Reusable component for monitoring entity status
 * @param {Array} items - Array of {id, name, status, details}
 * @param {string} title - Monitor title
 * @param {Function} onItemClick - Optional click handler
 * @param {Object} statusConfig - Status configuration {statusKey: {label, color, icon}}
 */
function StatusMonitor({ items, title, onItemClick, statusConfig }) {
  const defaultStatusConfig = {
    ACTIVE: { label: 'Active', color: '#10b981', icon: '✓' },
    IDLE: { label: 'Idle', color: '#f59e0b', icon: '○' },
    OFFLINE: { label: 'Offline', color: '#ef4444', icon: '✕' },
    MAINTENANCE: { label: 'Maintenance', color: '#6366f1', icon: '⚙' },
    PENDING: { label: 'Pending', color: '#f59e0b', icon: '○' },
    PROCESSING: { label: 'Processing', color: '#3b82f6', icon: '↻' },
    COMPLETED: { label: 'Completed', color: '#10b981', icon: '✓' },
    CANCELLED: { label: 'Cancelled', color: '#6b7280', icon: '✕' },
  };

  const config = statusConfig || defaultStatusConfig;

  if (items.length === 0) {
    return (
      <div className="chart-container">
        {title && <h3 className="chart-title">{title}</h3>}
        <div className="chart-empty">No items to monitor</div>
      </div>
    );
  }

  return (
    <div className="chart-container">
      {title && <h3 className="chart-title">{title}</h3>}
      <div className="status-monitor">
        {items.map((item, index) => {
          const statusInfo = config[item.status] || { label: item.status, color: '#6b7280', icon: '?' };
          return (
            <div 
              key={item.id || index} 
              className={`status-item ${onItemClick ? 'status-item-clickable' : ''}`}
              onClick={() => onItemClick && onItemClick(item)}
            >
              <div className="status-indicator" style={{ backgroundColor: statusInfo.color }}>
                {statusInfo.icon}
              </div>
              <div className="status-content">
                <div className="status-name">{item.name}</div>
                {item.details && <div className="status-details">{item.details}</div>}
              </div>
              <div className="status-badge" style={{ color: statusInfo.color }}>
                {statusInfo.label}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default StatusMonitor;
