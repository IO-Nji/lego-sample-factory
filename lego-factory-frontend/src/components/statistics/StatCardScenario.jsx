/**
 * StatCardScenario - Type 12: Scenario Distribution Card
 * 
 * Displays order distribution across the 4 business scenarios.
 * Color-coded badges with counts for each scenario type.
 * 
 * @example
 * <StatCardScenario
 *   title="Scenario Distribution"
 *   scenarios={[
 *     { id: 1, name: 'Direct', count: 34 },
 *     { id: 2, name: 'Warehouse', count: 28 },
 *     { id: 3, name: 'Production', count: 15 },
 *     { id: 4, name: 'High Volume', count: 8 },
 *   ]}
 * />
 */

import React from 'react';
import styles from './statistics.module.css';

const StatCardScenario = ({
  title = 'Scenario Distribution',
  scenarios = [],
  onClick,
  loading = false,
  className = '',
}) => {
  // Default scenarios if not provided
  const defaultScenarios = [
    { id: 1, name: 'Direct Fulfillment', count: 0 },
    { id: 2, name: 'Warehouse + Assembly', count: 0 },
    { id: 3, name: 'Full Production', count: 0 },
    { id: 4, name: 'High Volume', count: 0 },
  ];

  const effectiveScenarios = scenarios.length > 0 ? scenarios : defaultScenarios;

  // Calculate total for percentage display
  const total = effectiveScenarios.reduce((sum, s) => sum + (s.count || 0), 0);

  const getScenarioBadgeClass = (id) => {
    switch (id) {
      case 1: return styles.s1;
      case 2: return styles.s2;
      case 3: return styles.s3;
      case 4: return styles.s4;
      default: return styles.s1;
    }
  };

  return (
    <div 
      className={`
        ${styles.scenarioCard} 
        ${loading ? styles.loading : ''} 
        ${onClick ? styles.clickable : ''} 
        ${className}
      `}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className={styles.scenarioCardHeader}>
        <span className={styles.scenarioCardTitle}>{title}</span>
        {total > 0 && (
          <span className={styles.timelineCardTotal}>Total: {total}</span>
        )}
      </div>
      
      <div className={styles.scenarioGrid}>
        {effectiveScenarios.map((scenario) => (
          <div key={scenario.id} className={styles.scenarioItem}>
            <div className={`${styles.scenarioBadge} ${getScenarioBadgeClass(scenario.id)}`}>
              S{scenario.id}
            </div>
            <div className={styles.scenarioInfo}>
              <div className={styles.scenarioName}>{scenario.name}</div>
              <div className={styles.scenarioCount}>{scenario.count}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default StatCardScenario;
