/**
 * DashboardPanelRow - 4-Column Panel Grid Layout
 * 
 * Provides consistent 4-column layout for the second row of all dashboards.
 * Responsive: 4 columns → 2 columns → 1 column based on screen size.
 * 
 * Each panel receives the same dimensions ensuring visual consistency.
 */

import PropTypes from 'prop-types';
import '../../styles/DashboardPanels.css';

function DashboardPanelRow({ children, className = '' }) {
  return (
    <div className={`dashboard-panel-row ${className}`.trim()}>
      {children}
    </div>
  );
}

DashboardPanelRow.propTypes = {
  children: PropTypes.node.isRequired,
  className: PropTypes.string
};

export default DashboardPanelRow;
