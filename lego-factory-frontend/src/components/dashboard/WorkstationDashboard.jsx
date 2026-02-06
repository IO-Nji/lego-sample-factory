/**
 * WorkstationDashboard - Unified Layout Component
 * 
 * Provides consistent layout for all workstation dashboards:
 * - Header with workstation info and status (using DashboardHeader)
 * - Panel row with 4 operational panels (using DashboardPanelRow)
 * - Two-column main layout:
 *   - Left: Orders grid (65%)
 *   - Right: Sidebar with activity (35%)
 * - Footer
 * 
 * Theme colors applied via CSS classes (ws-theme-1 through ws-theme-9)
 */

import PropTypes from 'prop-types';
import { Footer } from '../index';
import DashboardHeader from './DashboardHeader';
import DashboardPanelRow from './DashboardPanelRow';
import '../../styles/WorkstationDashboard.css';
import '../../styles/DashboardHeader.css';
import '../../styles/DashboardPanels.css';

function WorkstationDashboard({
  // Header props
  workstationId,
  title,
  subtitle,
  icon,
  
  // Panel row content (4 panels for the second row)
  panelRowContent,
  
  // Content props
  ordersContent,
  activityContent,
  
  // State props
  loading = false,
  error = null,
  success = null,
  
  // Actions
  onRefresh,
  onDismissError,
  onDismissSuccess,
  
  // Theme (auto-detected from workstationId if not provided)
  theme
}) {
  // Determine theme class
  const getThemeClass = () => {
    if (theme) return theme;
    if (workstationId >= 1 && workstationId <= 9) return `ws-theme-${workstationId}`;
    return '';
  };

  if (loading) {
    return (
      <div className={`ws-page ${getThemeClass()}`}>
        <div className="ws-bg" />
        <div className="ws-loading">
          <div className="ws-spinner" />
          <span>Loading dashboard...</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`ws-page ${getThemeClass()}`}>
      <div className="ws-bg" />
      
      {/* Header - Using Unified DashboardHeader Component */}
      <DashboardHeader
        icon={icon}
        title={title}
        subtitle={subtitle}
        onRefresh={onRefresh}
        showStatus={true}
        statusLabel="Online"
        themeClass={getThemeClass()}
      />
      
      {/* Error Message */}
      {error && (
        <div className="ws-message ws-message--error">
          <span>⚠️ {error}</span>
          {onDismissError && (
            <button className="ws-message__close" onClick={onDismissError}>✕</button>
          )}
        </div>
      )}
      
      {/* Success Message */}
      {success && (
        <div className="ws-message ws-message--success">
          <span>✓ {success}</span>
          {onDismissSuccess && (
            <button className="ws-message__close" onClick={onDismissSuccess}>✕</button>
          )}
        </div>
      )}
      
      {/* Panel Row - 4 operational panels */}
      {panelRowContent && (
        <DashboardPanelRow>
          {panelRowContent}
        </DashboardPanelRow>
      )}
      
      {/* Main Content - 70/30 Split */}
      <main className="ws-main-split">
        {/* Orders Column (70%) */}
        <div className="ws-main-left">
          {ordersContent}
        </div>
        
        {/* Sidebar Column (30%) - Activity */}
        <div className="ws-main-right">
          {/* Activity Panel - Compact */}
          {activityContent && (
            <div className="ws-sidebar-panel ws-activity-compact ws-glass">
              {activityContent}
            </div>
          )}
        </div>
      </main>
      
      {/* Footer */}
      <Footer />
    </div>
  );
}

WorkstationDashboard.propTypes = {
  workstationId: PropTypes.number,
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  panelRowContent: PropTypes.node,
  ordersContent: PropTypes.node,
  activityContent: PropTypes.node,
  loading: PropTypes.bool,
  error: PropTypes.string,
  success: PropTypes.string,
  onRefresh: PropTypes.func,
  onDismissError: PropTypes.func,
  onDismissSuccess: PropTypes.func,
  theme: PropTypes.string
};

export default WorkstationDashboard;
