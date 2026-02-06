/**
 * DashboardHeader - Unified Header Component for ALL Dashboards
 * 
 * Provides a consistent header design across all dashboard types:
 * - Admin Dashboard
 * - Workstation Dashboards (WS-1 to WS-9)
 * - Control Dashboards (Production/Assembly Control)
 * - Planning Dashboards
 * 
 * Features:
 * - Left: Icon + Title + Subtitle
 * - Right: Status indicator (optional) + Refresh button
 * - Glassmorphism styling with gradient accent
 * - Theme support via CSS custom properties
 */

import { useState, useCallback } from 'react';
import PropTypes from 'prop-types';

function DashboardHeader({
  // Content
  icon,
  title,
  subtitle,
  
  // Refresh
  onRefresh,
  refreshLabel = 'Refresh',
  refreshingLabel = 'Updating...',
  
  // Status (optional)
  showStatus = false,
  statusLabel = 'Online',
  statusVariant = 'online', // 'online', 'offline', 'warning'
  
  // Theme/Styling
  themeClass = '',
  className = '',
}) {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = useCallback(async () => {
    if (!onRefresh || refreshing) return;
    
    setRefreshing(true);
    try {
      await onRefresh();
    } finally {
      setRefreshing(false);
    }
  }, [onRefresh, refreshing]);

  return (
    <header className={`dashboard-header ${themeClass} ${className}`.trim()}>
      <div className="dashboard-header__left">
        {icon && <div className="dashboard-header__icon">{icon}</div>}
        <div className="dashboard-header__info">
          <h1 className="dashboard-header__title">{title}</h1>
          {subtitle && <span className="dashboard-header__sub">{subtitle}</span>}
        </div>
      </div>
      
      <div className="dashboard-header__right">
        {showStatus && (
          <div className={`dashboard-header__status dashboard-header__status--${statusVariant}`}>
            <span className="dashboard-header__status-dot" />
            {statusLabel}
          </div>
        )}
        
        {onRefresh && (
          <button 
            className={`dashboard-header__refresh ${refreshing ? 'dashboard-header__refresh--spin' : ''}`}
            onClick={handleRefresh}
            disabled={refreshing}
          >
            <span className="dashboard-header__refresh-icon">⟳</span>
            {refreshing ? refreshingLabel : refreshLabel}
          </button>
        )}
      </div>
    </header>
  );
}

DashboardHeader.propTypes = {
  // Content
  icon: PropTypes.node,
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  
  // Refresh
  onRefresh: PropTypes.func,
  refreshLabel: PropTypes.string,
  refreshingLabel: PropTypes.string,
  
  // Status
  showStatus: PropTypes.bool,
  statusLabel: PropTypes.string,
  statusVariant: PropTypes.oneOf(['online', 'offline', 'warning']),
  
  // Theme/Styling
  themeClass: PropTypes.string,
  className: PropTypes.string,
};

export default DashboardHeader;
