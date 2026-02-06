/**
 * BaseDashboard - Core Dashboard Component
 * 
 * Provides the foundational layout and structure for all dashboard types.
 * Uses glassmorphism design from the homepage aesthetic.
 * 
 * @example
 * <BaseDashboard
 *   title="Admin Dashboard"
 *   subtitle="Real-time monitoring and control"
 *   icon="🏭"
 *   type={DASHBOARD_TYPES.ADMIN}
 *   heroStats={<HeroStatsSection stats={stats} layout="4-col" />}
 *   activityLog={<ActivityLog notifications={notifications} />}
 *   primaryContent={<OrdersSection orders={orders} />}
 *   onRefresh={handleRefresh}
 * />
 */

import React from 'react';
import PropTypes from 'prop-types';
import Footer from '../Footer';
import styles from './BaseDashboard.module.css';

const BaseDashboard = ({
  // Identity
  title,
  subtitle,
  icon,
  
  // Content Sections
  heroStats,
  activityLog,
  primaryContent,
  analyticsContent,
  workstationMonitor,
  detailsPrimary,
  detailsSecondary,
  ordersContent,
  inventoryContent,
  
  // Actions
  onRefresh,
  refreshing = false,
  customActions,
  
  // State
  loading = false,
  error,
  onDismissError,
  
  // Layout options
  activityPosition = 'left', // 'left' | 'right' | 'none'
  showFooter = true,
  
  // Children for custom sections
  children,
}) => {
  // Loading state
  if (loading) {
    return (
      <div className={styles.dashboardContainer}>
        <DashboardHeader 
          icon={icon} 
          title={title} 
          subtitle="Loading..." 
        />
        <div className={styles.loadingState}>
          <div className={styles.loadingSpinner} />
          <p>Initializing dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.dashboardContainer}>
      {/* Header */}
      <DashboardHeader
        icon={icon}
        title={title}
        subtitle={subtitle}
        onRefresh={onRefresh}
        refreshing={refreshing}
        customActions={customActions}
      />

      {/* Error Message */}
      {error && (
        <div className={styles.errorMessage}>
          <p><strong>Error:</strong> {error}</p>
          {onDismissError && (
            <button className={styles.errorDismiss} onClick={onDismissError}>
              Dismiss
            </button>
          )}
        </div>
      )}

      {/* Hero Stats Row */}
      {heroStats && (
        <div className={styles.heroStatsRow}>
          {heroStats}
        </div>
      )}

      {/* Main Content Row (Activity + Primary) */}
      {(activityLog || primaryContent) && (
        <div className={styles.mainContentRow}>
          {activityPosition === 'left' && activityLog && (
            <div className={styles.activityColumn}>
              <div className={styles.sectionCard}>
                {activityLog}
              </div>
            </div>
          )}
          
          {primaryContent && (
            <div className={styles.primaryColumn}>
              <div className={styles.sectionCard}>
                {primaryContent}
              </div>
            </div>
          )}
          
          {activityPosition === 'right' && activityLog && (
            <div className={styles.activityColumn}>
              <div className={styles.sectionCard}>
                {activityLog}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Analytics Row */}
      {analyticsContent && (
        <div className={styles.analyticsRow}>
          {analyticsContent}
        </div>
      )}

      {/* Workstation Monitor Row */}
      {workstationMonitor && (
        <div className={styles.workstationRow}>
          <div className={styles.sectionCard}>
            {workstationMonitor}
          </div>
        </div>
      )}

      {/* Details Row */}
      {(detailsPrimary || detailsSecondary) && (
        <div className={styles.detailsRow}>
          {detailsPrimary && (
            <div className={styles.sectionCard}>
              {detailsPrimary}
            </div>
          )}
          {detailsSecondary && (
            <div className={styles.sectionCard}>
              {detailsSecondary}
            </div>
          )}
        </div>
      )}

      {/* Orders Row */}
      {(ordersContent || inventoryContent) && (
        <div className={styles.ordersRow}>
          {ordersContent && (
            <div className={styles.ordersColumn}>
              <div className={styles.sectionCard}>
                {ordersContent}
              </div>
            </div>
          )}
          {inventoryContent && (
            <div className={styles.inventoryColumn}>
              <div className={styles.sectionCard}>
                {inventoryContent}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Custom Children */}
      {children}

      {/* Footer */}
      {showFooter && (
        <div className={styles.dashboardFooter}>
          <Footer />
        </div>
      )}
    </div>
  );
};

/**
 * Dashboard Header Component
 */
const DashboardHeader = ({ 
  icon, 
  title, 
  subtitle, 
  onRefresh, 
  refreshing,
  customActions 
}) => (
  <header className={styles.dashboardHeader}>
    {icon && <div className={styles.dashboardIcon}>{icon}</div>}
    <div className={styles.dashboardHeaderContent}>
      <h1 className={styles.dashboardTitle}>{title}</h1>
      {subtitle && <p className={styles.dashboardSubtitle}>{subtitle}</p>}
    </div>
    <div className={styles.dashboardActions}>
      {customActions}
      {onRefresh && (
        <button 
          className={`${styles.refreshButton} ${refreshing ? styles.refreshing : ''}`}
          onClick={onRefresh}
          disabled={refreshing}
        >
          ⟳ {refreshing ? 'Refreshing...' : 'Refresh'}
        </button>
      )}
    </div>
  </header>
);

/**
 * Section Card Wrapper - For consistent section styling
 */
export const SectionCard = ({ 
  title, 
  actions, 
  children, 
  className = '',
  strong = false,
}) => (
  <div className={`${styles.sectionCard} ${strong ? styles.sectionCardStrong : ''} ${className}`}>
    {(title || actions) && (
      <div className={styles.sectionHeader}>
        {title && <h3 className={styles.sectionTitle}>{title}</h3>}
        {actions && <div className={styles.sectionActions}>{actions}</div>}
      </div>
    )}
    <div className={styles.sectionContent}>
      {children}
    </div>
  </div>
);

/**
 * Empty State Component
 */
export const EmptyState = ({ icon = '📭', title, message }) => (
  <div className={styles.emptyState}>
    <div className={styles.emptyStateIcon}>{icon}</div>
    {title && <h4 className={styles.emptyStateTitle}>{title}</h4>}
    <p className={styles.emptyStateText}>{message}</p>
  </div>
);

// PropTypes
BaseDashboard.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  heroStats: PropTypes.node,
  activityLog: PropTypes.node,
  primaryContent: PropTypes.node,
  analyticsContent: PropTypes.node,
  workstationMonitor: PropTypes.node,
  detailsPrimary: PropTypes.node,
  detailsSecondary: PropTypes.node,
  ordersContent: PropTypes.node,
  inventoryContent: PropTypes.node,
  onRefresh: PropTypes.func,
  refreshing: PropTypes.bool,
  customActions: PropTypes.node,
  loading: PropTypes.bool,
  error: PropTypes.string,
  onDismissError: PropTypes.func,
  activityPosition: PropTypes.oneOf(['left', 'right', 'none']),
  showFooter: PropTypes.bool,
  children: PropTypes.node,
};

SectionCard.propTypes = {
  title: PropTypes.string,
  actions: PropTypes.node,
  children: PropTypes.node,
  className: PropTypes.string,
  strong: PropTypes.bool,
};

EmptyState.propTypes = {
  icon: PropTypes.string,
  title: PropTypes.string,
  message: PropTypes.string.isRequired,
};

export default BaseDashboard;
