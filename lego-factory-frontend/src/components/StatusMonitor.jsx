import React from 'react';
import '../styles/Chart.css';

/**
 * StatusMonitor - Reusable component for monitoring entity status
 * @param {Array} items - Array of {id, name, status, details}
 * @param {string} title - Monitor title
 * @param {Function} onItemClick - Optional click handler
 * @param {Object} statusConfig - Status configuration {statusKey: {label, color, icon}}
 * @param {boolean} compact - Use compact horizontal layout with custom icons
 * @param {boolean} gridLayout - Use grid layout (3 columns)
 */
function StatusMonitor({ items, title, onItemClick, statusConfig, compact = false, gridLayout = false }) {
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

  // Custom workstation icons based on name/type - matching HomePage feature cards
  const getWorkstationIcon = (name) => {
    const nameLower = (name || '').toLowerCase();
    // Exact matches for specific station types
    if (nameLower.includes('plant') && nameLower.includes('warehouse')) return '🏭';
    if (nameLower.includes('modules') && nameLower.includes('supermarket')) return '🏢';
    if (nameLower.includes('production') && nameLower.includes('planning')) return '📋';
    if (nameLower.includes('production') && nameLower.includes('control')) return '🏭';
    if (nameLower.includes('assembly') && nameLower.includes('control')) return '⚙️';
    if (nameLower.includes('parts') && nameLower.includes('supply')) return '📦';
    
    // Generic matches for manufacturing and assembly stations
    if (nameLower.includes('manufacturing') || nameLower.includes('mfg')) return '🔧';
    if (nameLower.includes('assembly') || nameLower.includes('assy')) return '🔩';
    
    // Fallback matches
    if (nameLower.includes('warehouse') || nameLower.includes('wh') || nameLower.includes('storage')) return '📦';
    if (nameLower.includes('production') || nameLower.includes('prod')) return '⚙️';
    if (nameLower.includes('quality') || nameLower.includes('inspection')) return '🔍';
    if (nameLower.includes('packaging')) return '📦';
    if (nameLower.includes('shipping')) return '🚚';
    if (nameLower.includes('supply') || nameLower.includes('materials')) return '📋';
    
    return '⚙️'; // default changed from lightning to gear
  };

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
      <div className={`status-monitor ${compact ? 'status-monitor-compact' : ''} ${gridLayout ? 'status-monitor-grid' : ''}`}>
        {items.map((item, index) => {
          const statusInfo = config[item.status] || { label: item.status, color: '#6b7280', icon: '?' };
          const workstationIcon = compact ? getWorkstationIcon(item.name) : null;
          
          return (
            <div 
              key={item.id || index} 
              className={`status-item ${compact ? 'status-item-compact' : ''} ${onItemClick ? 'status-item-clickable' : ''}`}
              onClick={() => onItemClick && onItemClick(item)}
            >
              {compact ? (
                <>
                  <span className="workstation-icon">{workstationIcon}</span>
                  <div className="status-name">{item.name}</div>
                  <div className="status-badge" style={{ 
                    backgroundColor: statusInfo.color,
                    color: 'white'
                  }}>
                    {statusInfo.label}
                  </div>
                </>
              ) : (
                <>
                  <div className="status-indicator" style={{ backgroundColor: statusInfo.color }}>
                    {statusInfo.icon}
                  </div>
                  <div className="status-content">
                    <div className="status-name">{item.name}</div>
                    {item.details && <div className="status-details">{item.details}</div>}
                  </div>
                  <div className="status-badge" style={{ 
                    backgroundColor: statusInfo.color,
                    color: 'white'
                  }}>
                    {statusInfo.label}
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default StatusMonitor;
