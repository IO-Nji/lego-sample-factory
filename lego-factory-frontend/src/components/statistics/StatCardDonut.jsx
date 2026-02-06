/**
 * StatCardDonut - Type 9: Donut Chart Card
 * 
 * A statistics card with a donut/ring chart visualization.
 * Great for showing completion rates, distribution, or composition.
 * 
 * @example
 * <StatCardDonut
 *   title="Fulfillment Rate"
 *   value={78}
 *   max={100}
 *   centerLabel="Complete"
 *   segments={[
 *     { label: 'Fulfilled', value: 78, color: '#10b981' },
 *     { label: 'Pending', value: 22, color: '#e2e8f0' },
 *   ]}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardDonut = ({
  title,
  value,
  max = 100,
  centerLabel = '',
  segments = [], // Array of { label, value, color }
  onClick,
  loading = false,
  className = '',
  formatValue = (v) => v,
  size = 80, // SVG size in pixels
  strokeWidth = 8,
}) => {
  // Calculate SVG properties
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const center = size / 2;
  
  // Calculate percentage for primary value
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);
  
  // If no segments provided, create default based on value/max
  const effectiveSegments = segments.length > 0 
    ? segments 
    : [
        { label: 'Complete', value: value, color: '#10b981' },
        { label: 'Remaining', value: max - value, color: 'rgba(0,0,0,0.06)' },
      ];

  // Calculate segment offsets
  let offset = 0;
  const segmentData = effectiveSegments.map((segment) => {
    const segmentPercentage = (segment.value / max) * 100;
    const dashArray = (segmentPercentage / 100) * circumference;
    const dashOffset = circumference - (offset / 100) * circumference;
    offset += segmentPercentage;
    return {
      ...segment,
      dashArray,
      dashOffset: circumference - dashOffset,
    };
  });

  return (
    <div 
      className={`
        ${styles.donutCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.donutChart} style={{ width: size, height: size }}>
        <svg viewBox={`0 0 ${size} ${size}`}>
          {/* Background circle */}
          <circle
            className={styles.donutBg}
            cx={center}
            cy={center}
            r={radius}
            strokeWidth={strokeWidth}
          />
          
          {/* Segment circles */}
          {segmentData.map((segment, index) => (
            <circle
              key={index}
              className={styles.donutFill}
              cx={center}
              cy={center}
              r={radius}
              stroke={segment.color}
              strokeWidth={strokeWidth}
              strokeDasharray={`${segment.dashArray} ${circumference - segment.dashArray}`}
              strokeDashoffset={segment.dashOffset}
            />
          ))}
        </svg>
        
        <div className={styles.donutCenter}>
          <div className={styles.donutCenterValue}>{formatValue(value)}</div>
          {centerLabel && <div className={styles.donutCenterLabel}>{centerLabel}</div>}
        </div>
      </div>
      
      <div className={styles.donutCardContent}>
        <div className={styles.donutCardTitle}>{title}</div>
        
        <div className={styles.donutLegend}>
          {effectiveSegments.map((segment, index) => (
            <div key={index} className={styles.donutLegendItem}>
              <span 
                className={styles.donutLegendColor} 
                style={{ background: segment.color }}
              />
              <span>{segment.label}: {segment.value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default StatCardDonut;
