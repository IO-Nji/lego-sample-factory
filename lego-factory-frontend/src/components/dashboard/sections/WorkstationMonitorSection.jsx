/**
 * WorkstationMonitorSection - System-wide Workstation Status
 * 
 * Displays all workstations in a grid with real-time status indicators.
 * Used primarily in Admin and Control dashboards.
 * 
 * @example
 * <WorkstationMonitorSection
 *   workstations={workstationStatuses}
 *   onWorkstationClick={handleWorkstationClick}
 *   highlightWorkstation={7}
 * />
 */

import React from 'react';
import PropTypes from 'prop-types';
import styles from './WorkstationMonitorSection.module.css';

// Workstation type configurations
const WORKSTATION_TYPES = {
  MANUFACTURING: { color: '#3498db', label: 'Manufacturing' },
  ASSEMBLY: { color: '#9b59b6', label: 'Assembly' },
  WAREHOUSE: { color: '#27ae60', label: 'Warehouse' },
};

// Status configurations
const STATUS_CONFIG = {
  IDLE: { color: '#7f8c8d', label: 'Idle', icon: '⏸' },
  ACTIVE: { color: '#27ae60', label: 'Active', icon: '▶' },
  BUSY: { color: '#f39c12', label: 'Busy', icon: '🔄' },
  WARNING: { color: '#e67e22', label: 'Warning', icon: '⚠️' },
  ERROR: { color: '#e74c3c', label: 'Error', icon: '❌' },
  OFFLINE: { color: '#34495e', label: 'Offline', icon: '○' },
};

const WorkstationMonitorSection = ({
  workstations = [],
  title = 'Workstation Status',
  onWorkstationClick,
  highlightWorkstation,
  compact = false,
  showLegend = true,
}) => {
  // Group workstations by type
  const groupedWorkstations = React.useMemo(() => {
    return workstations.reduce((acc, ws) => {
      const type = ws.type || 'MANUFACTURING';
      if (!acc[type]) acc[type] = [];
      acc[type].push(ws);
      return acc;
    }, {});
  }, [workstations]);

  return (
    <div className={`${styles.monitorSection} ${compact ? styles.compact : ''}`}>
      <div className={styles.header}>
        <h3 className={styles.title}>{title}</h3>
        {showLegend && <StatusLegend />}
      </div>

      <div className={styles.workstationGrid}>
        {Object.entries(groupedWorkstations).map(([type, stations]) => (
          <div key={type} className={styles.typeGroup}>
            <div 
              className={styles.typeLabel}
              style={{ '--type-color': WORKSTATION_TYPES[type]?.color || '#666' }}
            >
              {WORKSTATION_TYPES[type]?.label || type}
            </div>
            <div className={styles.stationsRow}>
              {stations.map((ws) => (
                <WorkstationCard
                  key={ws.id}
                  workstation={ws}
                  onClick={onWorkstationClick}
                  highlighted={highlightWorkstation === ws.id}
                  compact={compact}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

/**
 * Individual Workstation Card
 */
const WorkstationCard = ({ workstation, onClick, highlighted, compact }) => {
  const { 
    id, 
    name, 
    shortName,
    status = 'IDLE',
    activeOrders = 0,
    pendingOrders = 0,
    operator,
  } = workstation;

  const statusConfig = STATUS_CONFIG[status] || STATUS_CONFIG.IDLE;

  return (
    <div
      className={`
        ${styles.workstationCard} 
        ${highlighted ? styles.highlighted : ''} 
        ${compact ? styles.compactCard : ''}
      `}
      onClick={() => onClick?.(workstation)}
      style={{ '--status-color': statusConfig.color }}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.statusIndicator}>
        <span className={styles.statusDot} />
      </div>
      
      <div className={styles.cardContent}>
        <div className={styles.wsLabel}>{shortName || `WS-${id}`}</div>
        {!compact && (
          <>
            <div className={styles.wsName}>{name}</div>
            <div className={styles.wsStats}>
              <span className={styles.activeCount} title="Active">
                {activeOrders} active
              </span>
              <span className={styles.pendingCount} title="Pending">
                {pendingOrders} pending
              </span>
            </div>
            {operator && (
              <div className={styles.operator}>
                <span className={styles.operatorIcon}>👤</span>
                {operator}
              </div>
            )}
          </>
        )}
      </div>

      <div className={styles.statusBadge}>
        {statusConfig.icon}
      </div>
    </div>
  );
};

/**
 * Status Legend Component
 */
const StatusLegend = () => (
  <div className={styles.legend}>
    {Object.entries(STATUS_CONFIG).slice(0, 4).map(([key, config]) => (
      <div key={key} className={styles.legendItem}>
        <span 
          className={styles.legendDot}
          style={{ backgroundColor: config.color }}
        />
        <span className={styles.legendLabel}>{config.label}</span>
      </div>
    ))}
  </div>
);

// PropTypes
WorkstationMonitorSection.propTypes = {
  workstations: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    name: PropTypes.string.isRequired,
    shortName: PropTypes.string,
    type: PropTypes.oneOf(['MANUFACTURING', 'ASSEMBLY', 'WAREHOUSE']),
    status: PropTypes.oneOf(['IDLE', 'ACTIVE', 'BUSY', 'WARNING', 'ERROR', 'OFFLINE']),
    activeOrders: PropTypes.number,
    pendingOrders: PropTypes.number,
    operator: PropTypes.string,
  })),
  title: PropTypes.string,
  onWorkstationClick: PropTypes.func,
  highlightWorkstation: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  compact: PropTypes.bool,
  showLegend: PropTypes.bool,
};

WorkstationCard.propTypes = {
  workstation: PropTypes.object.isRequired,
  onClick: PropTypes.func,
  highlighted: PropTypes.bool,
  compact: PropTypes.bool,
};

export default WorkstationMonitorSection;
