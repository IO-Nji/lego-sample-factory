/**
 * Dashboard Configuration System
 * 
 * Centralized configuration for all dashboard types in the LIFE MES system.
 * Defines layouts, sections, and behavior for each dashboard category.
 * 
 * @see DASHBOARD_ARCHITECTURE_REDESIGN.md for design documentation
 */

// Dashboard type constants
export const DASHBOARD_TYPES = {
  ADMIN: 'ADMIN',
  WORKSTATION: 'WORKSTATION',
  WAREHOUSE: 'WAREHOUSE',
  CONTROL: 'CONTROL',
  PLANNING: 'PLANNING',
};

// Layout type constants
export const LAYOUT_TYPES = {
  ADMIN: 'admin',           // Multi-row with charts, settings
  WORKSTATION: 'workstation', // Activity + Orders + Optional inventory
  WAREHOUSE: 'warehouse',     // Full inventory management
  CONTROL: 'control',         // Multi-station monitoring
  PLANNING: 'planning',       // Gantt-centric layout
};

/**
 * Section visibility configuration
 * Defines which sections are available for each dashboard type
 */
export const SECTION_DEFAULTS = {
  ADMIN: {
    heroStats: true,
    activityLog: true,
    charts: true,
    timeline: true,
    workstationMonitor: true,
    recentOrders: true,
    settings: true,
    inventory: false,
    createForm: false,
    orders: false,
  },
  WORKSTATION: {
    heroStats: true,
    activityLog: true,
    charts: false,
    timeline: false,
    workstationMonitor: false,
    recentOrders: false,
    settings: false,
    inventory: false,
    createForm: false,
    orders: true,
  },
  WAREHOUSE: {
    heroStats: true,
    activityLog: true,
    charts: false,
    timeline: false,
    workstationMonitor: false,
    recentOrders: false,
    settings: false,
    inventory: true,
    createForm: true,
    orders: true,
  },
  CONTROL: {
    heroStats: true,
    activityLog: true,
    charts: true,
    timeline: true,
    workstationMonitor: true,
    recentOrders: false,
    settings: false,
    inventory: false,
    createForm: false,
    orders: true,
  },
  PLANNING: {
    heroStats: true,
    activityLog: true,
    charts: false,
    timeline: true,
    workstationMonitor: true,
    recentOrders: false,
    settings: false,
    inventory: false,
    createForm: false,
    orders: true,
  },
};

/**
 * Hero stats configuration by dashboard type
 * Defines which statistics cards appear in the hero row
 */
export const HERO_STATS_CONFIG = {
  ADMIN: {
    layout: '4-col',
    cards: [
      { id: 'total', type: 'hero', label: 'Total Orders', icon: '📦', variant: 'primary' },
      { id: 'pending', type: 'trend', label: 'Processing', icon: '⚙️', variant: 'info' },
      { id: 'completed', type: 'trend', label: 'Completed', icon: '✅', variant: 'success' },
      { id: 'lowStock', type: 'threshold', label: 'Low Stock', icon: '⚠️', variant: 'danger' },
    ],
  },
  WORKSTATION: {
    layout: '4-col',
    cards: [
      { id: 'total', type: 'basic', label: 'Total', icon: '📦', variant: 'primary' },
      { id: 'pending', type: 'basic', label: 'Pending', icon: '📝', variant: 'warning' },
      { id: 'inProgress', type: 'basic', label: 'In Progress', icon: '⚙️', variant: 'info' },
      { id: 'completed', type: 'basic', label: 'Completed', icon: '✅', variant: 'success' },
    ],
  },
  WAREHOUSE: {
    layout: '4-col',
    cards: [
      { id: 'total', type: 'hero', label: 'Total Orders', icon: '📦', variant: 'primary' },
      { id: 'pending', type: 'basic', label: 'Pending', icon: '📝', variant: 'warning' },
      { id: 'confirmed', type: 'basic', label: 'Confirmed', icon: '✓', variant: 'info' },
      { id: 'processing', type: 'progress', label: 'Processing', icon: '⚙️', variant: 'success' },
    ],
  },
  CONTROL: {
    layout: '4-col',
    cards: [
      { id: 'total', type: 'hero', label: 'Control Orders', icon: '📋', variant: 'primary' },
      { id: 'scheduled', type: 'trend', label: 'Scheduled', icon: '📅', variant: 'info' },
      { id: 'inProgress', type: 'trend', label: 'In Progress', icon: '⚙️', variant: 'warning' },
      { id: 'completed', type: 'trend', label: 'Completed', icon: '✅', variant: 'success' },
    ],
  },
  PLANNING: {
    layout: '4-col',
    cards: [
      { id: 'total', type: 'hero', label: 'Production Orders', icon: '🏭', variant: 'primary' },
      { id: 'scheduled', type: 'basic', label: 'Scheduled', icon: '📅', variant: 'info' },
      { id: 'activeStations', type: 'basic', label: 'Active Stations', icon: '🔧', variant: 'warning' },
      { id: 'efficiency', type: 'progress', label: 'Efficiency', icon: '📊', variant: 'success' },
    ],
  },
};

/**
 * Activity log configuration by dashboard type
 */
export const ACTIVITY_LOG_CONFIG = {
  ADMIN: {
    title: 'System Activity',
    maxItems: 15,
    showClear: true,
    position: 'left',
    width: '320px',
  },
  WORKSTATION: {
    title: 'Station Activity',
    maxItems: 10,
    showClear: true,
    position: 'left',
    width: '280px',
  },
  WAREHOUSE: {
    title: 'Warehouse Activity',
    maxItems: 12,
    showClear: true,
    position: 'left',
    width: '280px',
  },
  CONTROL: {
    title: 'Control Activity',
    maxItems: 12,
    showClear: true,
    position: 'left',
    width: '300px',
  },
  PLANNING: {
    title: 'Planning Activity',
    maxItems: 10,
    showClear: true,
    position: 'left',
    width: '280px',
  },
};

/**
 * Get dashboard configuration by type
 * @param {string} type - Dashboard type from DASHBOARD_TYPES
 * @returns {Object} Complete configuration for the dashboard
 */
export const getDashboardConfig = (type) => {
  return {
    type,
    sections: SECTION_DEFAULTS[type] || SECTION_DEFAULTS.WORKSTATION,
    heroStats: HERO_STATS_CONFIG[type] || HERO_STATS_CONFIG.WORKSTATION,
    activityLog: ACTIVITY_LOG_CONFIG[type] || ACTIVITY_LOG_CONFIG.WORKSTATION,
  };
};

/**
 * Resolve dashboard type from role
 * @param {string} role - User role
 * @returns {string} Dashboard type
 */
export const resolveDashboardType = (role) => {
  const roleMap = {
    ADMIN: DASHBOARD_TYPES.ADMIN,
    PRODUCTION_PLANNING: DASHBOARD_TYPES.PLANNING,
    PRODUCTION_CONTROL: DASHBOARD_TYPES.CONTROL,
    ASSEMBLY_CONTROL: DASHBOARD_TYPES.CONTROL,
    PLANT_WAREHOUSE: DASHBOARD_TYPES.WAREHOUSE,
    MODULES_SUPERMARKET: DASHBOARD_TYPES.WAREHOUSE,
    PARTS_SUPPLY: DASHBOARD_TYPES.WAREHOUSE,
    MANUFACTURING: DASHBOARD_TYPES.WORKSTATION,
    VIEWER: DASHBOARD_TYPES.WORKSTATION,
  };
  return roleMap[role] || DASHBOARD_TYPES.WORKSTATION;
};

/**
 * Resolve dashboard type from workstation ID
 * @param {number} workstationId - Workstation ID (1-9)
 * @returns {string} Dashboard type
 */
export const resolveDashboardTypeFromWorkstation = (workstationId) => {
  // Warehouse workstations (7, 8, 9)
  if ([7, 8, 9].includes(workstationId)) {
    return DASHBOARD_TYPES.WAREHOUSE;
  }
  // Manufacturing and Assembly workstations (1-6)
  return DASHBOARD_TYPES.WORKSTATION;
};

export default {
  DASHBOARD_TYPES,
  LAYOUT_TYPES,
  SECTION_DEFAULTS,
  HERO_STATS_CONFIG,
  ACTIVITY_LOG_CONFIG,
  getDashboardConfig,
  resolveDashboardType,
  resolveDashboardTypeFromWorkstation,
};
