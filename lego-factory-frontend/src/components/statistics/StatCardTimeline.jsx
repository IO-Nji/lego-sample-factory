/**
 * StatCardTimeline - Type 8: Order Status Timeline
 * 
 * A horizontal flow visualization showing order progression through
 * different statuses. Ideal for showing pipeline/workflow status.
 * 
 * @example
 * <StatCardTimeline
 *   title="Order Pipeline"
 *   total={67}
 *   steps={[
 *     { label: 'Pending', value: 12, status: 'pending' },
 *     { label: 'Confirmed', value: 18, status: 'confirmed' },
 *     { label: 'In Progress', value: 25, status: 'inProgress' },
 *     { label: 'Completed', value: 12, status: 'completed' },
 *   ]}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardTimeline = ({
  title,
  total,
  steps = [], // Array of { label, value, status }
  onClick,
  loading = false,
  className = '',
  showArrows = true,
}) => {
  return (
    <div 
      className={`
        ${styles.timelineCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.timelineCardHeader}>
        <span className={styles.timelineCardTitle}>{title}</span>
        {total !== undefined && (
          <span className={styles.timelineCardTotal}>Total: {total}</span>
        )}
      </div>
      
      <div className={styles.timelineFlow}>
        {steps.map((step, index) => (
          <React.Fragment key={index}>
            <div className={styles.timelineStep}>
              <div className={`${styles.timelineStepBar} ${styles[step.status] || ''}`}>
                {step.value}
              </div>
              <div className={styles.timelineStepLabel}>{step.label}</div>
            </div>
            
            {showArrows && index < steps.length - 1 && (
              <svg className={styles.timelineArrow} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14m-7-7l7 7-7 7" />
              </svg>
            )}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
};

export default StatCardTimeline;
