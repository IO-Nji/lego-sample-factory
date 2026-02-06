/**
 * HeroStatsSection - Dynamic Hero Statistics Row
 * 
 * Renders a row of hero-style statistics cards using the new StatCard components.
 * Configurable layout and automatic card type selection based on data.
 * 
 * @example
 * <HeroStatsSection 
 *   stats={[
 *     { id: 'total', label: 'Total Orders', value: 125, trend: { value: 12, direction: 'up' } },
 *     { id: 'pending', label: 'Pending', value: 23, type: 'threshold', thresholds: { warning: 20, critical: 30 } },
 *     { id: 'completed', label: 'Completed', value: 102, type: 'progress', max: 125 },
 *   ]}
 *   layout="3-col"
 * />
 */

import React from 'react';
import PropTypes from 'prop-types';
import {
  StatCardHero,
  StatCardTrend,
  StatCardProgress,
  StatCardThreshold,
  StatCardCompact,
} from '../../statistics';
import styles from './HeroStatsSection.module.css';

// Layout configurations
const LAYOUTS = {
  '2-col': { columns: 2, cardSize: 'large' },
  '3-col': { columns: 3, cardSize: 'medium' },
  '4-col': { columns: 4, cardSize: 'medium' },
  '5-col': { columns: 5, cardSize: 'small' },
  'auto': { columns: 'auto', cardSize: 'medium' },
};

const HeroStatsSection = ({
  stats = [],
  layout = '4-col',
  title,
  compact = false,
}) => {
  if (!stats || stats.length === 0) {
    return null;
  }

  const layoutConfig = LAYOUTS[layout] || LAYOUTS['4-col'];
  
  const containerStyle = layoutConfig.columns === 'auto' 
    ? {}
    : { '--hero-columns': layoutConfig.columns };

  return (
    <div 
      className={`${styles.heroStatsSection} ${compact ? styles.compact : ''}`}
      style={containerStyle}
    >
      {title && <h3 className={styles.sectionTitle}>{title}</h3>}
      
      <div className={styles.statsGrid}>
        {stats.map((stat, index) => (
          <HeroStatCard 
            key={stat.id || index} 
            stat={stat} 
            size={layoutConfig.cardSize}
          />
        ))}
      </div>
    </div>
  );
};

/**
 * Individual Hero Stat Card - Auto-selects appropriate card type
 */
const HeroStatCard = ({ stat, size }) => {
  const { 
    id,
    label, 
    value, 
    type = 'hero', 
    icon,
    color,
    trend,
    max,
    thresholds,
    sparklineData,
    subtitle,
    unit,
  } = stat;

  // Common props
  const commonProps = {
    label,
    value,
    icon,
    color,
    subtitle,
    unit,
  };

  // Select card type based on data
  switch (type) {
    case 'trend':
      return (
        <StatCardTrend
          {...commonProps}
          trend={trend?.value}
          direction={trend?.direction}
          sparklineData={sparklineData}
        />
      );
    
    case 'progress':
      return (
        <StatCardProgress
          {...commonProps}
          current={value}
          max={max || 100}
          showPercentage
        />
      );
    
    case 'threshold':
      return (
        <StatCardThreshold
          {...commonProps}
          thresholds={thresholds}
        />
      );
    
    case 'compact':
      return (
        <StatCardCompact
          {...commonProps}
          size={size}
        />
      );
    
    case 'hero':
    default:
      return (
        <StatCardHero
          {...commonProps}
          trend={trend}
          highlight={stat.highlight}
        />
      );
  }
};

HeroStatsSection.propTypes = {
  stats: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.string,
    label: PropTypes.string.isRequired,
    value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
    type: PropTypes.oneOf(['hero', 'trend', 'progress', 'threshold', 'compact']),
    icon: PropTypes.string,
    color: PropTypes.string,
    trend: PropTypes.shape({
      value: PropTypes.number,
      direction: PropTypes.oneOf(['up', 'down', 'flat']),
    }),
    max: PropTypes.number,
    thresholds: PropTypes.shape({
      warning: PropTypes.number,
      critical: PropTypes.number,
    }),
    sparklineData: PropTypes.arrayOf(PropTypes.number),
    subtitle: PropTypes.string,
    unit: PropTypes.string,
    highlight: PropTypes.bool,
  })),
  layout: PropTypes.oneOf(['2-col', '3-col', '4-col', '5-col', 'auto']),
  title: PropTypes.string,
  compact: PropTypes.bool,
};

HeroStatCard.propTypes = {
  stat: PropTypes.object.isRequired,
  size: PropTypes.string,
};

export default HeroStatsSection;
