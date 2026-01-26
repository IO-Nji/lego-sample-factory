import PropTypes from 'prop-types';
import Footer from './Footer';
import styles from './StandardDashboardLayout.module.css';

/**
 * StandardDashboardLayout - Two-row dashboard layout component
 * 
 * Top Row (3 columns):
 * - Left: Activity/Logs (flexible width, takes remaining space)
 * - Middle: Statistics Grid (fixed minimal width)
 * - Right: Primary Form (fixed minimal width)
 * 
 * Bottom Row (2 columns):
 * - Left: Content Grid (flexible width)
 * - Right: Filter/Search Form (fixed minimal width)
 */
function StandardDashboardLayout({
  title,
  subtitle,
  icon,
  activityContent,
  statsContent,
  formContent,
  contentGrid,
  filterContent,
  inventoryContent,
}) {
  return (
    <div className={styles.dashboardContainer}>
      {/* Header */}
      <div className={styles.dashboardHeader}>
        {icon && <div className={styles.dashboardIcon}>{icon}</div>}
        <div className={styles.dashboardHeaderContent}>
          <h1 className={styles.dashboardTitle}>{title}</h1>
          {subtitle && <p className={styles.dashboardSubtitle}>{subtitle}</p>}
        </div>
      </div>

      {/* Top Row: Activity, Stats, Form */}
      <div className={styles.topRow}>
        {/* Activity/Logs Column */}
        {activityContent && (
          <div className={styles.activityColumn}>
            <div className={styles.card}>
              {activityContent}
            </div>
          </div>
        )}

        {/* Statistics Column */}
        {statsContent && (
          <div className={styles.statsColumn}>
            {statsContent}
          </div>
        )}

        {/* Form Column */}
        {formContent && (
          <div className={styles.formColumn}>
            <div className={styles.card}>
              {formContent}
            </div>
          </div>
        )}
      </div>

      {/* Bottom Row: Content Grid, Inventory */}
      <div className={styles.bottomRow}>
        {/* Content Grid Column */}
        {contentGrid && (
          <div className={styles.ordersColumn}>
            <div className={styles.card}>
              {contentGrid}
            </div>
          </div>
        )}

        {/* Inventory Column */}
        {inventoryContent && (
          <div className={styles.inventoryColumn}>
            <div className={styles.card}>
              {inventoryContent}
            </div>
          </div>
        )}

        {/* Filter Column (legacy support) */}
        {filterContent && !inventoryContent && (
          <div className={styles.filterColumn}>
            <div className={styles.card}>
              {filterContent}
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <Footer />
    </div>
  );
}

StandardDashboardLayout.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  activityContent: PropTypes.node,
  statsContent: PropTypes.node,
  formContent: PropTypes.node,
  contentGrid: PropTypes.node,
  filterContent: PropTypes.node,
  inventoryContent: PropTypes.node,
};

export default StandardDashboardLayout;
