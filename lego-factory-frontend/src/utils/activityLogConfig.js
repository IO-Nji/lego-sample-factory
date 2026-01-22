/**
 * Activity Log Configuration
 * 
 * Centralized configuration for workstation colors, order type colors,
 * and message formatting utilities for the Activity Log component.
 */

// Workstation color mapping
export const workstationColors = {
  'PLANT-WH': '#2563eb',           // Blue - Plant Warehouse
  'MODULES-SM': '#7c3aed',         // Purple - Modules Supermarket
  'PARTS-WH': '#059669',           // Green - Parts Supply Warehouse
  'INJECTION-MOLD': '#dc2626',     // Red - Injection Molding
  'PRE-PROD': '#ea580c',           // Orange - Parts Pre-Production
  'PART-FINISH': '#ca8a04',        // Yellow - Part Finishing
  'GEAR-ASSEMBLY': '#0891b2',      // Cyan - Gear Assembly
  'MOTOR-ASSEMBLY': '#4f46e5',     // Indigo - Motor Assembly
  'FINAL-ASSEMBLY': '#9333ea',     // Purple - Final Assembly
  'PROD-PLANNING': '#06b6d4',      // Teal - Production Planning
  'PROD-CONTROL': '#8b5cf6',       // Violet - Production Control
  'ASSEMBLY-CTRL': '#a855f7',      // Purple - Assembly Control
  'ADMIN': '#374151',              // Gray - Admin
  'SYSTEM': '#6b7280'              // Gray - System messages
};

// Order type color mapping
export const orderTypeColors = {
  'customer': '#10b981',           // Green - Customer orders
  'warehouse': '#3b82f6',          // Blue - Warehouse orders
  'production': '#f59e0b',         // Amber - Production orders
  'supply': '#8b5cf6',             // Purple - Supply orders
  'assembly': '#ec4899',           // Pink - Assembly orders
  'control': '#6366f1',            // Indigo - Control orders
  'default': '#6b7280'             // Gray - Default
};

// Role to workstation color mapping
export const roleToWorkstationColor = {
  'ADMIN': workstationColors['ADMIN'],
  'PLANT_WAREHOUSE': workstationColors['PLANT-WH'],
  'MODULES_SUPERMARKET': workstationColors['MODULES-SM'],
  'PARTS_SUPPLY': workstationColors['PARTS-WH'],
  'PRODUCTION_PLANNING': workstationColors['PROD-PLANNING'],
  'PRODUCTION_CONTROL': workstationColors['PROD-CONTROL'],
  'ASSEMBLY_CONTROL': workstationColors['ASSEMBLY-CTRL'],
  'MANUFACTURING': workstationColors['INJECTION-MOLD'],
  'VIEWER': workstationColors['SYSTEM']
};

/**
 * Format message by removing redundant words
 * @param {string} message - Original message
 * @returns {string} Formatted message
 */
export function formatActivityMessage(message) {
  if (!message) return '';
  
  // Remove "Order" before order numbers
  let formatted = message.replace(/Order\s+(#?\w+-\d+)/gi, '$1');
  
  // Remove redundant "order" after order numbers (e.g., "CUST-123 order created")
  formatted = formatted.replace(/(#?\w+-\d+)\s+order\s+/gi, '$1 ');
  
  // Standardize capitalization
  formatted = formatted.charAt(0).toUpperCase() + formatted.slice(1);
  
  return formatted;
}

/**
 * Extract order type from message or order number
 * @param {string} message - Notification message
 * @param {string} orderNumber - Order number (optional)
 * @returns {string} Order type key for color mapping
 */
export function extractOrderType(message, orderNumber = '') {
  const lowerMsg = (message + ' ' + orderNumber).toLowerCase();
  
  if (lowerMsg.includes('customer') || lowerMsg.includes('cust-')) {
    return 'customer';
  }
  if (lowerMsg.includes('warehouse') || lowerMsg.includes('wh-')) {
    return 'warehouse';
  }
  if (lowerMsg.includes('production') || lowerMsg.includes('prod-')) {
    return 'production';
  }
  if (lowerMsg.includes('supply') || lowerMsg.includes('sup-')) {
    return 'supply';
  }
  if (lowerMsg.includes('assembly') || lowerMsg.includes('asm-')) {
    return 'assembly';
  }
  if (lowerMsg.includes('control') || lowerMsg.includes('ctrl-')) {
    return 'control';
  }
  
  return 'default';
}

/**
 * Get workstation color from station identifier or role
 * @param {string} station - Workstation identifier or role
 * @returns {string} Hex color code
 */
export function getWorkstationColor(station) {
  if (!station) return workstationColors['SYSTEM'];
  
  // Try direct workstation match
  if (workstationColors[station]) {
    return workstationColors[station];
  }
  
  // Try role-based match
  if (roleToWorkstationColor[station]) {
    return roleToWorkstationColor[station];
  }
  
  // Try partial match
  const stationUpper = station.toUpperCase();
  for (const [key, color] of Object.entries(workstationColors)) {
    if (stationUpper.includes(key.replace('-', ' ')) || key.includes(stationUpper)) {
      return color;
    }
  }
  
  return workstationColors['SYSTEM'];
}

/**
 * Get order type color
 * @param {string} orderType - Order type identifier
 * @returns {string} Hex color code
 */
export function getOrderTypeColor(orderType) {
  return orderTypeColors[orderType] || orderTypeColors['default'];
}

/**
 * Create a formatted login notification message
 * @param {object} user - User object from session
 * @returns {string} Formatted login message
 */
export function createLoginMessage(user) {
  if (!user) return 'User logged in';
  
  const name = user.firstName && user.lastName 
    ? `${user.firstName} ${user.lastName}`
    : user.username;
  
  const workstation = user.workstation?.name || user.workstationName || '';
  
  if (workstation) {
    return `${name} logged in to ${workstation}`;
  }
  
  return `${name} logged in`;
}

/**
 * Enhanced notification structure
 * @typedef {Object} ActivityNotification
 * @property {string|number} id - Unique identifier
 * @property {string} message - Notification message
 * @property {string} type - Notification type (success, error, warning, info)
 * @property {string} timestamp - ISO timestamp
 * @property {string} station - Workstation identifier
 * @property {string} [orderType] - Order type for color coding
 * @property {string} [orderNumber] - Order number if applicable
 * @property {string} [userName] - User name for personalization
 */
