/**
 * Dashboard Components - Index
 * 
 * Central export for all dashboard-related components.
 * Use these components to build consistent dashboards across the application.
 * 
 * @example
 * import { 
 *   DashboardHeader,
 *   DashboardPanel,
 *   DashboardPanelRow,
 *   WorkstationDashboard,
 *   WarehouseDashboard,
 *   ActivityPanel,
 *   InventoryPanel,
 *   FormPanel,
 *   getWorkstationPanels,
 *   getDashboardConfig 
 * } from '../components/dashboard';
 */

// Unified Header Component (used by all dashboards)
export { default as DashboardHeader } from './DashboardHeader';

// Panel Components (for the 4-column grid row)
export { default as DashboardPanel } from './DashboardPanel';
export { default as DashboardPanelRow } from './DashboardPanelRow';
export { WORKSTATION_PANELS, getWorkstationPanels, DEFAULT_STATS } from './workstationPanelConfig';

// Base Components
export { default as BaseDashboard, SectionCard, EmptyState } from './BaseDashboard';

// Section Components (for Admin and Control dashboards)
export { 
  HeroStatsSection, 
  ActivityLogSection, 
  WorkstationMonitorSection 
} from './sections';

// Workstation Dashboard Components (New unified design)
export { default as WorkstationDashboard } from './WorkstationDashboard';
export { default as WarehouseDashboard } from './WarehouseDashboard';
export { default as ControlDashboard } from './ControlDashboard';
export { default as ActivityPanel } from './ActivityPanel';
export { default as InventoryPanel } from './InventoryPanel';
export { default as FormPanel } from './FormPanel';
export { default as QuickOrderPanel } from './QuickOrderPanel';
export { 
  DASHBOARD_TYPES,
  SECTION_DEFAULTS,
  HERO_STATS_CONFIG,
  ACTIVITY_LOG_CONFIG,
  getDashboardConfig,
  resolveDashboardType,
} from './dashboardConfig';
