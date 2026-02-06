/**
 * Statistics Components - Central Export
 * 
 * All statistics card types for the LIFE MES system.
 * Import from this file for consistent access to all card components.
 * 
 * @example
 * import { 
 *   StatCardTrend, 
 *   StatCardProgress, 
 *   StatisticsGrid 
 * } from '../components/statistics';
 */

// Grid Layout
export { default as StatisticsGrid } from './StatisticsGrid';

// Type 2: Trend Card - Metric with trend indicator
export { default as StatCardTrend } from './StatCardTrend';

// Type 3: Progress Card - Progress/gauge visualization
export { default as StatCardProgress } from './StatCardProgress';

// Type 4: Sparkline Card - Mini chart visualization
export { default as StatCardSparkline } from './StatCardSparkline';

// Type 5: Threshold Card - Stock/threshold alert
export { default as StatCardThreshold } from './StatCardThreshold';

// Type 6: Compact Card - Inline compact stats
export { default as StatCardCompact } from './StatCardCompact';

// Type 7: Multi-Metric Card - Grouped metrics
export { default as StatCardMulti } from './StatCardMulti';

// Type 8: Timeline Card - Order status timeline
export { default as StatCardTimeline } from './StatCardTimeline';

// Type 9: Donut Card - Donut chart visualization
export { default as StatCardDonut } from './StatCardDonut';

// Type 10: Hero Card - Featured/hero stat
export { default as StatCardHero } from './StatCardHero';

// Type 11: Workstation Card - Workstation status
export { default as StatCardWorkstation } from './StatCardWorkstation';

// Type 12: Scenario Card - Scenario distribution
export { default as StatCardScenario } from './StatCardScenario';

// Re-export shared styles for custom implementations
export { default as statisticsStyles } from './statistics.module.css';

/**
 * Card Type Reference:
 * 
 * | Type | Component        | Use Case                           |
 * |------|------------------|-----------------------------------|
 * | 1    | StatCard         | Basic metric (use existing)       |
 * | 2    | StatCardTrend    | Metric with trend indicator       |
 * | 3    | StatCardProgress | Progress bars, capacity gauges    |
 * | 4    | StatCardSparkline| Time series, mini charts          |
 * | 5    | StatCardThreshold| Stock alerts, threshold warnings  |
 * | 6    | StatCardCompact  | Dense layouts, sidebars           |
 * | 7    | StatCardMulti    | Grouped related metrics           |
 * | 8    | StatCardTimeline | Order pipeline visualization      |
 * | 9    | StatCardDonut    | Completion rates, distributions   |
 * | 10   | StatCardHero     | Featured KPI, prominent display   |
 * | 11   | StatCardWorkstation| Workstation status monitoring   |
 * | 12   | StatCardScenario | Scenario distribution             |
 */
