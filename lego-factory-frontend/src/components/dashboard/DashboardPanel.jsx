/**
 * DashboardPanel - Reusable Card Component for Dashboard Grids
 * 
 * A glassmorphism-styled panel that can display:
 * - Header with icon, title, and badge
 * - Stats row (4-column mini stats)
 * - Sub-stats footer
 * - Custom children content
 * - Action buttons
 * 
 * Used in the 4-column panel row across all dashboards.
 */

import PropTypes from 'prop-types';
import '../../styles/DashboardPanels.css';

function DashboardPanel({
  // Header
  icon,
  title,
  badge,
  badgeVariant = 'default', // 'default', 'active', 'warning', 'danger', 'success'
  
  // Stats (optional 4-column mini stats)
  stats = [],
  
  // Sub-stats footer (optional)
  subStats = [],
  
  // Alerts (optional)
  alerts = [],
  
  // Actions (optional button grid)
  actions = [],
  onAction,
  
  // Custom content
  children,
  
  // Styling
  variant = 'default', // 'default', 'actions', 'compact'
  className = '',
  themeClass = ''
}) {
  return (
    <div className={`dashboard-panel glass ${variant !== 'default' ? `dashboard-panel--${variant}` : ''} ${themeClass} ${className}`.trim()}>
      {/* Panel Header */}
      <div className="dashboard-panel__head">
        {icon && <span className="dashboard-panel__icon">{icon}</span>}
        <h2 className="dashboard-panel__title">{title}</h2>
        {badge && (
          <span className={`dashboard-panel__badge dashboard-panel__badge--${badgeVariant}`}>
            {badge}
          </span>
        )}
      </div>
      
      {/* Stats Row */}
      {stats.length > 0 && (
        <div className="dashboard-panel__stats">
          {stats.map((stat, index) => (
            <div 
              key={stat.id || index} 
              className={`dashboard-panel__stat ${stat.variant ? `dashboard-panel__stat--${stat.variant}` : ''}`}
            >
              <span className="dashboard-panel__stat-val">{stat.value}</span>
              <span className="dashboard-panel__stat-lbl">{stat.label}</span>
            </div>
          ))}
        </div>
      )}
      
      {/* Custom Children */}
      {children}
      
      {/* Sub Stats Footer */}
      {subStats.length > 0 && (
        <div className="dashboard-panel__substats">
          {subStats.map((sub, index) => (
            <span key={index}>
              {sub.icon} {sub.label}: {sub.value}
            </span>
          ))}
        </div>
      )}
      
      {/* Alerts */}
      {alerts.length > 0 && (
        <div className="dashboard-panel__alerts">
          {alerts.map((alert, index) => (
            <div 
              key={index} 
              className={`dashboard-panel__alert dashboard-panel__alert--${alert.type || 'info'}`}
            >
              {alert.type === 'critical' ? '🔴' : alert.type === 'warning' ? '🟠' : '🔵'} {alert.message}
            </div>
          ))}
        </div>
      )}
      
      {/* Action Buttons */}
      {actions.length > 0 && (
        <div className="dashboard-panel__actions">
          {actions.map((action, index) => (
            <button
              key={index}
              className={`dashboard-panel__action ${action.primary ? 'dashboard-panel__action--primary' : ''}`}
              onClick={() => onAction && onAction(action.type)}
              disabled={action.disabled}
            >
              {action.icon} {action.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

DashboardPanel.propTypes = {
  icon: PropTypes.node,
  title: PropTypes.string.isRequired,
  badge: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  badgeVariant: PropTypes.oneOf(['default', 'active', 'warning', 'danger', 'success']),
  stats: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    label: PropTypes.string.isRequired,
    variant: PropTypes.string
  })),
  subStats: PropTypes.arrayOf(PropTypes.shape({
    icon: PropTypes.string,
    label: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  })),
  alerts: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.oneOf(['info', 'warning', 'critical']),
    message: PropTypes.string.isRequired
  })),
  actions: PropTypes.arrayOf(PropTypes.shape({
    type: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    icon: PropTypes.string,
    primary: PropTypes.bool,
    disabled: PropTypes.bool
  })),
  onAction: PropTypes.func,
  children: PropTypes.node,
  variant: PropTypes.oneOf(['default', 'actions', 'compact']),
  className: PropTypes.string,
  themeClass: PropTypes.string
};

export default DashboardPanel;
