/**
 * WarehouseDashboard - Layout Component for Warehouse Stations
 * 
 * Two-row layout:
 * - Row 1: Panel cards (stats, metrics, quick order)
 * - Row 2: 70/30 split - Orders (left) | Activity + Inventory (right)
 * 
 * Based on WorkstationDashboard but with inventory and order forms.
 */

import PropTypes from 'prop-types';
import DashboardHeader from './DashboardHeader';
import DashboardPanelRow from './DashboardPanelRow';
import { Footer } from '../index';
import '../../styles/WorkstationDashboard.css';
import '../../styles/DashboardHeader.css';
import '../../styles/DashboardPanels.css';

function WarehouseDashboard({
  workstationId,
  title,
  subtitle,
  icon = '🏢',
  stats = [],
  panelRowContent,
  ordersContent,
  activityContent,
  inventoryContent,
  loading = false,
  error,
  onRefresh,
  onDismissError
}) {
  const themeClass = `ws-theme-${workstationId}`;

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

      {/* Row 1: Panel Cards */}
      {panelRowContent && (
        <DashboardPanelRow>
          {panelRowContent}
        </DashboardPanelRow>
      )}

      {/* Legacy Stats Row - For backwards compatibility */}
      {!panelRowContent && stats.length > 0 && (
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

      {/* Row 2: Main Content - 70/30 Split */}
      <div className="ws-main-split">
        {/* Left Column - Orders (70%) */}
        <div className="ws-main-left">
          <div className="ws-glass-card">
            {ordersContent}
          </div>
        </div>

        {/* Right Column - Activity + Inventory (30%) */}
        <div className="ws-main-right">
          {/* Activity Log - Compact (5 items visible) */}
          {activityContent && (
            <div className="ws-sidebar-panel ws-activity-compact">
              {activityContent}
            </div>
          )}
          
          {/* Inventory Panel - Below Activity */}
          {inventoryContent && (
            <div className="ws-sidebar-panel ws-inventory-panel">
              {inventoryContent}
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <Footer />
    </div>
  );
}

WarehouseDashboard.propTypes = {
  workstationId: PropTypes.number.isRequired,
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
  panelRowContent: PropTypes.node,
  ordersContent: PropTypes.node,
  activityContent: PropTypes.node,
  inventoryContent: PropTypes.node,
  loading: PropTypes.bool,
  error: PropTypes.string,
  onRefresh: PropTypes.func,
  onDismissError: PropTypes.func
};

export default WarehouseDashboard;
