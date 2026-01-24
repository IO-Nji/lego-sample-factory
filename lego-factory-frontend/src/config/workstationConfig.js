/**
 * Workstation Configuration
 * Centralized configuration for all workstation dashboards
 * 
 * This file defines metadata, API endpoints, and display properties
 * for each workstation in the LIFE system.
 */

export const WORKSTATION_CONFIG = {
  // Manufacturing Workstations (WS-1, WS-2, WS-3)
  1: {
    id: 1,
    code: 'WS-1',
    stationCode: 'INJ-MOLD',
    name: 'Injection Molding',
    type: 'MANUFACTURING',
    apiEndpoint: '/injection-molding-orders',
    title: 'Injection Molding Dashboard',
    subtitle: 'WS-1 | Plastic parts injection molding station',
    icon: '🏭',
    orderCardType: 'ProductionControlOrderCard',
    ordersTitle: 'Injection Molding Orders',
    emptyMessage: 'No injection molding orders found. Orders will appear here when dispatched from production control.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - injection molding in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - parts ready for pre-production`,
    completeConfirm: 'Complete this injection molding order?',
  },
  2: {
    id: 2,
    code: 'WS-2',
    stationCode: 'PART-PREP',
    name: 'Parts Pre-Production',
    type: 'MANUFACTURING',
    apiEndpoint: '/part-preproduction-orders',
    title: 'Parts Pre-Production Dashboard',
    subtitle: 'WS-2 | Parts preparation and processing station',
    icon: '🔩',
    orderCardType: 'ProductionControlOrderCard',
    ordersTitle: 'Parts Pre-Production Orders',
    emptyMessage: 'No parts pre-production orders found. Orders will appear here when dispatched from production control.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - parts preparation in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - parts ready for finishing`,
    completeConfirm: 'Complete this parts pre-production order?',
  },
  3: {
    id: 3,
    code: 'WS-3',
    stationCode: 'PART-FIN',
    name: 'Part Finishing',
    type: 'MANUFACTURING',
    apiEndpoint: '/part-finishing-orders',
    title: 'Part Finishing Dashboard',
    subtitle: 'WS-3 | Parts finishing and quality control station',
    icon: '✨',
    orderCardType: 'ProductionControlOrderCard',
    ordersTitle: 'Part Finishing Orders',
    emptyMessage: 'No part finishing orders found. Orders will appear here when dispatched from production control.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - finishing in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - Parts Supply Warehouse credited`,
    completeConfirm: 'Complete this part finishing order? Parts will be credited to Parts Supply Warehouse.',
  },
  
  // Assembly Workstations (WS-4, WS-5, WS-6)
  4: {
    id: 4,
    code: 'WS-4',
    stationCode: 'GEAR-ASSY',
    name: 'Gear Assembly',
    type: 'ASSEMBLY',
    apiEndpoint: '/gear-assembly-orders',
    title: 'Gear Assembly Dashboard',
    subtitle: 'WS-4 | Gear module assembly station',
    icon: '⚙️',
    orderCardType: 'AssemblyControlOrderCard',
    ordersTitle: 'Gear Assembly Orders',
    emptyMessage: 'No gear assembly orders found. Orders will appear here when dispatched from assembly control.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - gear assembly in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - Modules Supermarket credited`,
    completeConfirm: 'Complete this gear assembly order? Modules will be credited to Modules Supermarket.',
  },
  5: {
    id: 5,
    code: 'WS-5',
    stationCode: 'MOTOR-ASSY',
    name: 'Motor Assembly',
    type: 'ASSEMBLY',
    apiEndpoint: '/motor-assembly-orders',
    title: 'Motor Assembly Dashboard',
    subtitle: 'WS-5 | Motor module assembly station',
    icon: '🔌',
    orderCardType: 'AssemblyControlOrderCard',
    ordersTitle: 'Motor Assembly Orders',
    emptyMessage: 'No motor assembly orders found. Orders will appear here when dispatched from assembly control.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - motor assembly in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - Modules Supermarket credited`,
    completeConfirm: 'Complete this motor assembly order? Modules will be credited to Modules Supermarket.',
  },
  6: {
    id: 6,
    code: 'WS-6',
    stationCode: 'FINAL-ASSY',
    name: 'Final Assembly',
    type: 'ASSEMBLY',
    apiEndpoint: '/final-assembly-orders',
    title: 'Final Assembly Dashboard',
    subtitle: 'WS-6 | Final product assembly station',
    icon: '🔧',
    orderCardType: 'FinalAssemblyOrderCard',
    ordersTitle: 'Final Assembly Orders',
    emptyMessage: 'No final assembly orders found. Orders will appear here when created from warehouse orders.',
    startMessage: (orderNumber) => `Order ${orderNumber} started - assembly in progress`,
    completeMessage: (orderNumber) => `Order ${orderNumber} completed - Plant Warehouse credited`,
    completeConfirm: 'Complete this assembly? This will credit the Plant Warehouse with finished products.',
  },
  
  // Warehouse Workstations (WS-7, WS-8, WS-9)
  7: {
    id: 7,
    code: 'WS-7',
    stationCode: 'PLANT-WH',
    name: 'Plant Warehouse',
    type: 'WAREHOUSE',
    apiEndpoint: '/customer-orders',
    title: 'Plant Warehouse Dashboard',
    subtitle: 'WS-7 | Customer order fulfillment',
    icon: '📦',
    inventoryType: 'PRODUCT',
    ordersTitle: 'Customer Orders',
    emptyMessage: 'No customer orders found.',
  },
  8: {
    id: 8,
    code: 'WS-8',
    stationCode: 'MOD-SUPER',
    name: 'Modules Supermarket',
    type: 'WAREHOUSE',
    apiEndpoint: '/warehouse-orders',
    title: 'Modules Supermarket Dashboard',
    subtitle: 'WS-8 | Internal warehouse operations',
    icon: '🏪',
    inventoryType: 'MODULE',
    ordersTitle: 'Warehouse Orders',
    emptyMessage: 'No warehouse orders found.',
  },
  9: {
    id: 9,
    code: 'WS-9',
    stationCode: 'PARTS-SUP',
    name: 'Parts Supply',
    type: 'WAREHOUSE',
    apiEndpoint: '/supply-orders',
    title: 'Parts Supply Dashboard',
    subtitle: 'WS-9 | Raw materials distribution',
    icon: '🔧',
    inventoryType: 'PART',
    ordersTitle: 'Supply Orders',
    emptyMessage: 'No supply orders found.',
  },
};

/**
 * Standard filter options for order lists
 */
export const STANDARD_FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

/**
 * Standard sort options for order lists
 */
export const STANDARD_SORT_OPTIONS = [
  { value: 'orderNumber', label: 'Order Number' },
  { value: 'status', label: 'Status' },
];

/**
 * Calculate standard order statistics from an orders array
 * @param {Array} orders - Array of order objects
 * @returns {Array} Stats data for StatisticsGrid component
 */
export const calculateOrderStats = (orders) => [
  { value: orders.length, label: 'Total Orders', variant: 'default', icon: '📦' },
  { value: orders.filter(o => o.status === 'PENDING').length, label: 'Pending', variant: 'warning', icon: '📝' },
  { value: orders.filter(o => o.status === 'IN_PROGRESS').length, label: 'In Progress', variant: 'info', icon: '⚙️' },
  { value: orders.filter(o => o.status === 'COMPLETED').length, label: 'Completed', variant: 'success', icon: '✅' },
];

/**
 * Get workstation config by ID
 * @param {number} workstationId 
 * @returns {Object|null} Workstation configuration or null if not found
 */
export const getWorkstationConfig = (workstationId) => {
  return WORKSTATION_CONFIG[workstationId] || null;
};

/**
 * Get all workstations of a specific type
 * @param {string} type - 'MANUFACTURING', 'ASSEMBLY', or 'WAREHOUSE'
 * @returns {Array} Array of workstation configs
 */
export const getWorkstationsByType = (type) => {
  return Object.values(WORKSTATION_CONFIG).filter(ws => ws.type === type);
};

export default WORKSTATION_CONFIG;
