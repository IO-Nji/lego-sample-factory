/**
 * StatisticsGrid - Responsive Grid Layout for Statistics Cards
 * 
 * A wrapper component that provides responsive grid layouts for
 * statistics cards. Supports various column configurations.
 * 
 * @example
 * <StatisticsGrid columns={4} gap="1rem">
 *   <StatCard value={10} label="Total" />
 *   <StatCard value={5} label="Active" />
 *   <StatCard value={3} label="Done" />
 *   <StatCard value={2} label="Pending" />
 * </StatisticsGrid>
 */

import React from 'react';
import styles from './statistics.module.css';

const StatisticsGrid = ({
  children,
  columns = 'auto', // 2, 3, 4, 6, 'auto'
  gap = '1rem',
  className = '',
  style = {},
}) => {
  const getGridClass = () => {
    switch (columns) {
      case 2: return styles.statsGrid2;
      case 3: return styles.statsGrid3;
      case 4: return styles.statsGrid4;
      case 6: return styles.statsGrid6;
      case 'auto':
      default: return styles.statsGridAuto;
    }
  };

  return (
    <div 
      className={`${styles.statsGrid} ${getGridClass()} ${className}`}
      style={{ gap, ...style }}
    >
      {children}
    </div>
  );
};

export default StatisticsGrid;
