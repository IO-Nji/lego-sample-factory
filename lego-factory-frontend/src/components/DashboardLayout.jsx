import PropTypes from 'prop-types';
import PageHeader from './PageHeader';
import '../styles/DashboardLayout.css';

/**
 * StandardizedDashboardLayout - Reusable layout component for all station dashboards
 * 
 * Layout Structure Options:
 * 1. Default Layout (layout="default"):
 *    - Row 1: Stats cards (4 columns)
 *    - Row 2: Main content area (2 columns: primary + secondary)
 *    - Row 3: Orders section
 * 
 * 2. Compact Layout (layout="compact"):
 *    - Row 1: Stats cards (4 columns) + Primary Content (side-by-side)
 *    - Row 2: Orders section (full width)
 *    - No secondary content displayed
 * 
 * @param {Object} props
 * @param {string} props.title - Page title
 * @param {string} props.subtitle - Page subtitle
 * @param {string} props.icon - Page icon emoji
 * @param {string} props.layout - Layout type: 'default' | 'compact'
 * @param {React.ReactNode} props.statsCards - Array of StatsCard components
 * @param {React.ReactNode} props.primaryContent - Main content (form or table)
 * @param {React.ReactNode} props.secondaryContent - Secondary content (inventory/stock display)
 * @param {React.ReactNode} props.ordersSection - Orders display section
 * @param {React.ReactNode} props.infoBox - Optional information box
 * @param {Object} props.messages - Error and success messages {error, success}
 * @param {Function} props.onDismissError - Callback to dismiss error message
 * @param {Function} props.onDismissSuccess - Callback to dismiss success message
 */
function DashboardLayout({
  title,
  subtitle,
  icon,
  layout = 'default',
  statsCards,
  primaryContent,
  secondaryContent,
  ordersSection,
  infoBox,
  messages = {},
  onDismissError,
  onDismissSuccess
}) {
  return (
    <div className="dashboard-layout">
      {/* Page Header */}
      {title && (
        <PageHeader
          title={title}
          subtitle={subtitle}
          icon={icon}
        />
      )}

      {/* Messages Section */}
      {messages.error && (
        <div className="dashboard-message dashboard-message-error">
          <p className="dashboard-message-title">Error</p>
          <p className="dashboard-message-text">{messages.error}</p>
          {onDismissError && (
            <button
              onClick={onDismissError}
              className="dashboard-message-dismiss"
            >
              Dismiss
            </button>
          )}
        </div>
      )}

      {messages.success && (
        <div className="dashboard-message dashboard-message-success">
          <p className="dashboard-message-title">Success</p>
          <p className="dashboard-message-text">{messages.success}</p>
          {onDismissSuccess && (
            <button
              onClick={onDismissSuccess}
              className="dashboard-message-dismiss"
            >
              Dismiss
            </button>
          )}
        </div>
      )}

      {/* Layout: Compact - Stats + Primary Content in one row */}
      {layout === 'compact' && (
        <div className="dashboard-compact-row">
          {statsCards && (
            <div className="dashboard-stats-compact">
              {statsCards}
            </div>
          )}
          {primaryContent && (
            <div className="dashboard-primary-content">
              {primaryContent}
            </div>
          )}
        </div>
      )}

      {/* Layout: Default - Stats in full row, then Primary + Secondary */}
      {layout === 'default' && (
        <>
          {/* Stats Cards Row */}
          {statsCards && (
            <div className="dashboard-stats-row">
              {statsCards}
            </div>
          )}

          {/* Main Content Row: Primary (Form/Table) + Secondary (Inventory) */}
          <div className="dashboard-content-row">
            {primaryContent && (
              <div className="dashboard-primary-content">
                {primaryContent}
              </div>
            )}
            {secondaryContent && (
              <div className="dashboard-secondary-content">
                {secondaryContent}
              </div>
            )}
          </div>
        </>
      )}

  layout: PropTypes.oneOf(['default', 'compact']),
      {/* Orders Section */}
      {ordersSection && (
        <div className="dashboard-orders-section">
          {ordersSection}
        </div>
      )}

      {/* Optional Info Box */}
      {infoBox && (
        <div className="dashboard-info-box">
          {infoBox}
        </div>
      )}
    </div>
  );
}

DashboardLayout.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  statsCards: PropTypes.node,
  primaryContent: PropTypes.node,
  secondaryContent: PropTypes.node,
  ordersSection: PropTypes.node,
  infoBox: PropTypes.node,
  messages: PropTypes.shape({
    error: PropTypes.string,
    success: PropTypes.string
  }),
  onDismissError: PropTypes.func,
  onDismissSuccess: PropTypes.func
};

export default DashboardLayout;
