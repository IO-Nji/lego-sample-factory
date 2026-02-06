/**
 * Design System Components - Central Export
 * 
 * Import all standardized components from this file for consistency.
 * All components follow the design system defined in docs/DesignSystem.md
 * 
 * @example
 * import { Button, Card, StatCard, Input, Alert } from '../components';
 */

// Core UI Components
export { default as Button } from './Button';
export { default as Card } from './Card';
export { default as ControlPage } from './ControlPage';
export { default as Footer } from './Footer';
export { default as StatCard } from './StatCard';
export { default as StatisticsGrid } from './StatisticsGrid';
export { default as Badge } from './Badge';

// Statistics Cards (New Glassmorphism Design System)
export {
  StatCardTrend,
  StatCardProgress,
  StatCardSparkline,
  StatCardThreshold,
  StatCardCompact,
  StatCardMulti,
  StatCardTimeline,
  StatCardDonut,
  StatCardHero,
  StatCardWorkstation,
  StatCardScenario,
  StatisticsGrid as StatsGrid,  // Alias for convenience
  statisticsStyles,
} from './statistics';

// Data Display
export { default as Table } from './Table';
export { default as PieChart } from './PieChart';
export { default as BarChart } from './BarChart';
export { default as GanttChart } from './GanttChart';
export { default as CompactScheduleTimeline } from './CompactScheduleTimeline';

// Form Components
export { default as Input } from './Input';
export { default as Select } from './Select';

// Navigation
export { default as Tabs } from './Tabs';

// Feedback
export { default as LoadingSpinner } from './LoadingSpinner';
export { default as Alert } from './Alert';
export { default as Notification } from './Notification';

// Layout Components
// Note: DashboardLayout was removed in Feb 2026 cleanup - use StandardDashboardLayout
export { default as StandardDashboardLayout } from './StandardDashboardLayout';
export { default as PageHeader } from './PageHeader';

// Dashboard System Components
export {
  BaseDashboard,
  SectionCard,
  EmptyState,
  HeroStatsSection,
  ActivityLogSection,
  WorkstationMonitorSection,
  DASHBOARD_TYPES,
  SECTION_DEFAULTS,
  getDashboardConfig,
  resolveDashboardType,
  // New unified workstation dashboard components
  WorkstationDashboard,
  WarehouseDashboard,
  ControlDashboard,
  ActivityPanel,
  InventoryPanel,
  FormPanel,
  QuickOrderPanel,
  // Panel row components (Feb 2026)
  DashboardHeader,
  DashboardPanel,
  DashboardPanelRow,
} from './dashboard';

// Reusable Dashboard Components
export { default as InventoryTable } from './InventoryTable';
export { default as OrdersSection } from './OrdersSection';
export { default as FormCard } from './FormCard';
export { default as ActivityLog } from './ActivityLog';

// Unified Order Card (use this for all order card implementations)
export { default as UnifiedOrderCard, ORDER_TYPES } from './orders/UnifiedOrderCard';
export * from './orders/orderCardConfig';

// New Order Grid System (use for migrated dashboards)
export { 
  default as OrdersGrid, 
  VIEW_MODES, 
  DEFAULT_FILTER_OPTIONS, 
  DEFAULT_SORT_OPTIONS 
} from './orders/OrdersGrid';
export { GridCard, CompactCard, ListRow } from './orders/OrderCardVariants';

// Feedback & Interaction
export { default as Tooltip } from './Tooltip';

// Workstation Components
export { default as WorkstationCard } from './WorkstationCard';

// Admin Components
export { default as AdminSettingsPanel } from './AdminSettingsPanel';
