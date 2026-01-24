import React from 'react';
import StatCard from './StatCard';
import styles from './StatisticsGrid.module.css';

const StatisticsGrid = ({ stats }) => {
  return (
    <div className={styles.statisticsGrid}>
      {stats.map((stat, index) => (
        <StatCard
          key={index}
          value={stat.value}
          label={stat.label}
          variant={stat.variant}
          icon={stat.icon}
          className={styles.statCard}
        />
      ))}
    </div>
  );
};

export default StatisticsGrid;
