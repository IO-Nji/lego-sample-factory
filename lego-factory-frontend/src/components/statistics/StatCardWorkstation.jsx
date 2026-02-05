/**
 * StatCardWorkstation - Type 11: Workstation Status Card
 * 
 * A compact status indicator for workstation monitoring.
 * Shows workstation name, status (active/idle/alert), and metrics.
 * 
 * @example
 * <StatCardWorkstation
 *   name="Injection Molding"
 *   code="WS-1"
 *   status="active"
 *   icon="🏭"
 *   orders={3}
 *   estimatedTime="45 min"
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardWorkstation = ({
  name,
  code,
  status = 'idle', // 'active' | 'idle' | 'alert'
  icon,
  orders,
  estimatedTime,
  onClick,
  loading = false,
  className = '',
}) => {
  const getStatusClass = () => {
    switch (status) {
      case 'active': return styles.active;
      case 'alert': return styles.alertStatus;
      default: return styles.idle;
    }
  };

  const getStatusLabel = () => {
    switch (status) {
      case 'active': return 'Active';
      case 'alert': return 'Needs Attention';
      default: return 'Idle';
    }
  };

  return (
    <div 
      className={`
        ${styles.workstationCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={`${styles.workstationStatusIndicator} ${getStatusClass()}`}>
        {icon || '🏭'}
      </div>
      
      <div className={styles.workstationInfo}>
        <div className={styles.workstationName}>{name}</div>
        <div className={styles.workstationDetail}>
          {code && <span>{code}</span>}
          {code && ' • '}
          <span>{getStatusLabel()}</span>
        </div>
      </div>
      
      <div className={styles.workstationMetrics}>
        {orders !== undefined && (
          <div className={`${styles.workstationMetric} ${styles.orders}`}>
            {orders} {orders === 1 ? 'order' : 'orders'}
          </div>
        )}
        {estimatedTime && (
          <div className={`${styles.workstationMetric} ${styles.time}`}>
            {estimatedTime}
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCardWorkstation;
