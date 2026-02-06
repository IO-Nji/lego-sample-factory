/**
 * ControlDashboard - Layout Component for Control Stations
 * 
 * Extended layout for control dashboards that manage:
 * - Production/Assembly control orders
 * - Workstation orders monitoring
 * - Supply order coordination
 * - Gantt chart scheduling (for Production Planning)
 */

import PropTypes from 'prop-types';
import DashboardHeader from './DashboardHeader';
import { Footer } from '../index';
import '../../styles/WorkstationDashboard.css';
import '../../styles/DashboardHeader.css';

function ControlDashboard({
  themeClass = 'ws-theme-planning',
  title,
  subtitle,
  icon = '🎛️',
  stats = [],
  mainContent,
  secondaryContent,
  sidebarContent,
  loading = false,
  error,
  onRefresh,
  onDismissError
}) {
  return (
    <div className={`ws-page ${themeClass}`}>
      <div className="ws-bg" />
      
      {/* Header - Using Unified DashboardHeader Component */}
      <DashboardHeader
        icon={icon}
        title={title}
        subtitle={subtitle}
        onRefresh={onRefresh}
        showStatus={true}
        statusLabel="Online"
        themeClass={themeClass}
      />

      {/* Error Banner */}
      {error && (
        <div className="ws-error-banner">
          <span>⚠️ {error}</span>
          {onDismissError && (
            <button onClick={onDismissError} className="ws-error-dismiss">×</button>
          )}
        </div>
      )}

      {/* Stats Row */}
      {stats.length > 0 && (
        <div className="ws-stats-row">
          {stats.map((stat, idx) => (
            <div key={stat.id || idx} className={`ws-stat-card ws-stat-card--${stat.variant || 'default'}`}>
              <div className="ws-stat-icon">{stat.icon}</div>
              <div className="ws-stat-content">
                <div className="ws-stat-value">{stat.value}</div>
                <div className="ws-stat-label">{stat.label}</div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Main Content Area - Control Layout */}
      <div className="ws-main-control">
        {/* Primary Content (70%) - Orders or Main View */}
        <div className="ws-control-primary">
          <div className="ws-glass-card">
            {mainContent}
          </div>
        </div>

        {/* Secondary/Sidebar Content (30%) */}
        <div className="ws-control-secondary">
          {secondaryContent && (
            <div className="ws-sidebar-panel ws-control-secondary-panel">
              {secondaryContent}
            </div>
          )}
          {sidebarContent && (
            <div className="ws-sidebar-panel ws-control-sidebar-panel">
              {sidebarContent}
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <Footer />
    </div>
  );
}

ControlDashboard.propTypes = {
  themeClass: PropTypes.string,
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  stats: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    label: PropTypes.string,
    icon: PropTypes.string,
    variant: PropTypes.string
  })),
  mainContent: PropTypes.node,
  secondaryContent: PropTypes.node,
  sidebarContent: PropTypes.node,
  loading: PropTypes.bool,
  error: PropTypes.string,
  onRefresh: PropTypes.func,
  onDismissError: PropTypes.func
};

export default ControlDashboard;
