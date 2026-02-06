/**
 * Activity Log Configuration
 * 
 * Centralized configuration for activity log messaging, colors,
 * and formatting utilities. Provides consistent log structure 
 * across all workstation and admin dashboards.
 * 
 * MESSAGE FORMAT STANDARD:
 * ────────────────────────────────────────────────────────────────
 * [ICON] [ACTION] [TARGET]                              [HH:MM:SS]
 * ────────────────────────────────────────────────────────────────
 * 
 * Examples:
 * ✓  ORD-1234 confirmed                                   14:32:15
 * ▶  Injection Molding started                            14:30:00
 * 👤 warehouse_operator logged in                         14:28:45
 * ⚠  Low stock: Gear Module (WS-8)                        14:25:00
 */

// ============================================================================
// ACTIVITY TYPES & CONFIGURATION
// ============================================================================

/**
 * Activity type configuration with icons, colors, and action verbs
 */
export const ACTIVITY_TYPES = {
  // Authentication
  LOGIN: { icon: '👤', color: '#6366f1', action: 'logged in', category: 'auth' },
  LOGOUT: { icon: '👋', color: '#94a3b8', action: 'logged out', category: 'auth' },
  
  // Order lifecycle
  ORDER_CREATED: { icon: '📋', color: '#3b82f6', action: 'created', category: 'order' },
  ORDER_CONFIRMED: { icon: '✓', color: '#10b981', action: 'confirmed', category: 'order' },
  ORDER_STARTED: { icon: '▶', color: '#06b6d4', action: 'started', category: 'order' },
  ORDER_COMPLETED: { icon: '✅', color: '#059669', action: 'completed', category: 'order' },
  ORDER_SUBMITTED: { icon: '📤', color: '#8b5cf6', action: 'submitted', category: 'order' },
  ORDER_FULFILLED: { icon: '📦', color: '#10b981', action: 'fulfilled', category: 'order' },
  ORDER_HALTED: { icon: '⏸', color: '#f59e0b', action: 'halted', category: 'order' },
  ORDER_RESUMED: { icon: '▶', color: '#06b6d4', action: 'resumed', category: 'order' },
  ORDER_CANCELLED: { icon: '✕', color: '#ef4444', action: 'cancelled', category: 'order' },
  ORDER_REJECTED: { icon: '🚫', color: '#dc2626', action: 'rejected', category: 'order' },
  
  // Production
  PRODUCTION_SCHEDULED: { icon: '📅', color: '#8b5cf6', action: 'scheduled', category: 'production' },
  PRODUCTION_DISPATCHED: { icon: '🚀', color: '#f59e0b', action: 'dispatched', category: 'production' },
  TASK_STARTED: { icon: '⚙️', color: '#06b6d4', action: 'started', category: 'production' },
  TASK_COMPLETED: { icon: '🏁', color: '#10b981', action: 'completed', category: 'production' },
  
  // Inventory
  STOCK_CREDITED: { icon: '📈', color: '#10b981', action: 'credited', category: 'inventory' },
  STOCK_DEBITED: { icon: '📉', color: '#f59e0b', action: 'debited', category: 'inventory' },
  STOCK_LOW: { icon: '⚠️', color: '#f59e0b', action: 'low stock', category: 'inventory' },
  STOCK_CRITICAL: { icon: '🔴', color: '#ef4444', action: 'critical stock', category: 'inventory' },
  STOCK_ADJUSTED: { icon: '📊', color: '#6366f1', action: 'adjusted', category: 'inventory' },
  
  // System
  SYSTEM_INFO: { icon: 'ℹ', color: '#64748b', action: '', category: 'system' },
  SYSTEM_ERROR: { icon: '⚠', color: '#ef4444', action: 'error', category: 'system' },
  DATA_REFRESHED: { icon: '🔄', color: '#94a3b8', action: 'refreshed', category: 'system' },
};

// ============================================================================
// WORKSTATION CONFIGURATION
// ============================================================================

/**
 * Workstation identifiers with display names and colors
 * Keyed by both string (WS-1) and numeric ID (1) for flexibility
 */
export const WORKSTATIONS = {
  // String keys
  'WS-1': { code: 'INJ-MOLD', name: 'Injection Molding', color: '#dc2626', shortName: 'Inj' },
  'WS-2': { code: 'PRE-PROD', name: 'Parts Pre-Production', color: '#ea580c', shortName: 'Pre' },
  'WS-3': { code: 'PART-FIN', name: 'Part Finishing', color: '#ca8a04', shortName: 'Fin' },
  'WS-4': { code: 'GEAR-ASM', name: 'Gear Assembly', color: '#0891b2', shortName: 'Gear' },
  'WS-5': { code: 'MOTOR-ASM', name: 'Motor Assembly', color: '#4f46e5', shortName: 'Motor' },
  'WS-6': { code: 'FINAL-ASM', name: 'Final Assembly', color: '#9333ea', shortName: 'Final' },
  'WS-7': { code: 'PLANT-WH', name: 'Plant Warehouse', color: '#2563eb', shortName: 'Plant' },
  'WS-8': { code: 'MODS-SM', name: 'Modules Supermarket', color: '#7c3aed', shortName: 'Mods' },
  'WS-9': { code: 'PARTS-SUP', name: 'Parts Supply', color: '#059669', shortName: 'Parts' },
  // Numeric ID keys (same data)
  1: { code: 'INJ-MOLD', name: 'Injection Molding', color: '#dc2626', shortName: 'Inj' },
  2: { code: 'PRE-PROD', name: 'Parts Pre-Production', color: '#ea580c', shortName: 'Pre' },
  3: { code: 'PART-FIN', name: 'Part Finishing', color: '#ca8a04', shortName: 'Fin' },
  4: { code: 'GEAR-ASM', name: 'Gear Assembly', color: '#0891b2', shortName: 'Gear' },
  5: { code: 'MOTOR-ASM', name: 'Motor Assembly', color: '#4f46e5', shortName: 'Motor' },
  6: { code: 'FINAL-ASM', name: 'Final Assembly', color: '#9333ea', shortName: 'Final' },
  7: { code: 'PLANT-WH', name: 'Plant Warehouse', color: '#2563eb', shortName: 'Plant' },
  8: { code: 'MODS-SM', name: 'Modules Supermarket', color: '#7c3aed', shortName: 'Mods' },
  9: { code: 'PARTS-SUP', name: 'Parts Supply', color: '#059669', shortName: 'Parts' },
};

/**
 * Station code to workstation mapping (for backwards compatibility)
 */
export const STATION_CODES = {
  'INJ-MOLD': 'WS-1',
  'PRE-PROD': 'WS-2',
  'PART-FIN': 'WS-3',
  'GEAR-ASM': 'WS-4',
  'MOTOR-ASM': 'WS-5',
  'FINAL-ASM': 'WS-6',
  'FINAL-ASSY': 'WS-6',
  'PLANT-WH': 'WS-7',
  'MODS-SM': 'WS-8',
  'MODS-SP': 'WS-8',
  'MOD-SUPER': 'WS-8',
  'PARTS-SUP': 'WS-9',
  'PARTS-WH': 'WS-9',
  'PROD-PL': 'PROD-PLANNING',
  'PROD-CTRL': 'PROD-CONTROL',
  'ASM-CTRL': 'ASSEMBLY-CONTROL',
  'ADMIN': 'ADMIN',
  'SYSTEM': 'SYSTEM',
};

/**
 * Role-based workstation colors
 */
export const ROLE_COLORS = {
  'ADMIN': '#374151',
  'PRODUCTION_PLANNING': '#06b6d4',
  'PRODUCTION_CONTROL': '#8b5cf6',
  'ASSEMBLY_CONTROL': '#a855f7',
  'PLANT_WAREHOUSE': '#2563eb',
  'MODULES_SUPERMARKET': '#7c3aed',
  'PARTS_SUPPLY': '#059669',
  'MANUFACTURING': '#dc2626',
  'VIEWER': '#6b7280',
  'SYSTEM': '#94a3b8',
};

// Legacy exports for backwards compatibility
export const workstationColors = Object.fromEntries(
  Object.entries(WORKSTATIONS).map(([key, val]) => [val.code, val.color])
);
workstationColors['PROD-PLANNING'] = ROLE_COLORS['PRODUCTION_PLANNING'];
workstationColors['PROD-CONTROL'] = ROLE_COLORS['PRODUCTION_CONTROL'];
workstationColors['ASSEMBLY-CTRL'] = ROLE_COLORS['ASSEMBLY_CONTROL'];
workstationColors['ADMIN'] = ROLE_COLORS['ADMIN'];
workstationColors['SYSTEM'] = ROLE_COLORS['SYSTEM'];

export const orderTypeColors = {
  'customer': '#10b981',
  'warehouse': '#3b82f6',
  'production': '#f59e0b',
  'supply': '#8b5cf6',
  'assembly': '#ec4899',
  'control': '#6366f1',
  'default': '#6b7280'
};

export const roleToWorkstationColor = ROLE_COLORS;

// ============================================================================
// MESSAGE CREATION UTILITIES
// ============================================================================

/**
 * Create a standardized activity log entry
 * 
 * @param {string} type - Activity type from ACTIVITY_TYPES
 * @param {Object} options - Message options
 * @param {string} options.target - Primary target (order number, task name, etc.)
 * @param {string} options.station - Station code or workstation ID
 * @param {string} options.user - Username (for auth events)
 * @param {string} options.details - Additional details
 * @param {string} options.customMessage - Override the auto-generated message
 * @returns {Object} Formatted activity log entry
 */
export function createActivityEntry(type, options = {}) {
  const config = ACTIVITY_TYPES[type] || ACTIVITY_TYPES.SYSTEM_INFO;
  const { target, station, user, details, customMessage } = options;
  
  // Build the message
  let message = customMessage;
  if (!message) {
    const parts = [];
    if (target) parts.push(target);
    if (config.action) parts.push(config.action);
    if (details) parts.push(`(${details})`);
    message = parts.join(' ') || config.action || 'Activity';
  }
  
  // Resolve station info
  const stationInfo = resolveStation(station);
  
  return {
    id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: getNotificationType(type),
    activityType: type,
    message,
    icon: config.icon,
    color: config.color,
    category: config.category,
    station: stationInfo.code,
    stationName: stationInfo.name,
    stationColor: stationInfo.color,
    user: user || null,
    target: target || null,
    timestamp: new Date().toISOString(),
  };
}

/**
 * Convert activity type to notification type (success/error/warning/info)
 */
function getNotificationType(activityType) {
  if (activityType.includes('COMPLETED') || activityType.includes('FULFILLED') || 
      activityType.includes('SUBMITTED') || activityType === 'LOGIN') {
    return 'success';
  }
  if (activityType.includes('ERROR') || activityType.includes('CANCELLED') || 
      activityType.includes('CRITICAL') || activityType.includes('REJECTED')) {
    return 'error';
  }
  if (activityType.includes('HALTED') || activityType.includes('LOW') || 
      activityType.includes('WARNING')) {
    return 'warning';
  }
  return 'info';
}

/**
 * Resolve station code to full station info
 */
function resolveStation(station) {
  if (!station) return { code: 'SYSTEM', name: 'System', color: '#94a3b8' };
  
  // Direct workstation ID match (WS-1, WS-2, etc.)
  if (WORKSTATIONS[station]) {
    const ws = WORKSTATIONS[station];
    return { code: ws.code, name: ws.name, color: ws.color };
  }
  
  // Station code match
  if (STATION_CODES[station]) {
    const wsKey = STATION_CODES[station];
    if (WORKSTATIONS[wsKey]) {
      const ws = WORKSTATIONS[wsKey];
      return { code: ws.code, name: ws.name, color: ws.color };
    }
    // Non-workstation roles
    return { code: station, name: station.replace(/-/g, ' '), color: ROLE_COLORS[wsKey] || '#94a3b8' };
  }
  
  // Role match
  if (ROLE_COLORS[station]) {
    return { code: station, name: station.replace(/_/g, ' '), color: ROLE_COLORS[station] };
  }
  
  return { code: station, name: station, color: '#94a3b8' };
}

// ============================================================================
// PREDEFINED MESSAGE BUILDERS
// ============================================================================

/**
 * Create login activity entry
 * @param {string} username - User's username
 * @param {Object|null} station - Station info from WORKSTATIONS (with code, name, color)
 */
export function createLoginEntry(username, station = null) {
  const config = ACTIVITY_TYPES.LOGIN;
  
  // Resolve station info
  let stationInfo = { code: 'SYSTEM', name: 'System', color: '#94a3b8' };
  if (station) {
    stationInfo = { code: station.code, name: station.name, color: station.color };
  }
  
  return {
    id: `login-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'success',
    activityType: 'LOGIN',
    message: `${username} logged in`,
    icon: config.icon,
    color: config.color,
    category: config.category,
    station: stationInfo.code,
    stationName: stationInfo.name,
    stationColor: stationInfo.color,
    user: username,
    timestamp: new Date().toISOString(),
  };
}

/**
 * Create order action entry
 * @param {string} action - Action verb: 'created', 'confirmed', 'started', 'completed', etc.
 * @param {string} orderNumber - Order number (e.g., 'ORD-1234', 'CUST-001')
 * @param {string} type - Notification type: 'success', 'error', 'warning', 'info'
 * @param {string|null} customMessage - Optional custom message override
 * @param {Object|null} station - Station info from WORKSTATIONS (with code, name, color)
 */
export function createOrderEntry(action, orderNumber, type = 'success', customMessage = null, station = null) {
  const typeMap = {
    'created': 'ORDER_CREATED',
    'confirmed': 'ORDER_CONFIRMED',
    'started': 'ORDER_STARTED',
    'completed': 'ORDER_COMPLETED',
    'submitted': 'ORDER_SUBMITTED',
    'fulfilled': 'ORDER_FULFILLED',
    'processing': 'ORDER_STARTED',
    'halted': 'ORDER_HALTED',
    'resumed': 'ORDER_RESUMED',
    'cancelled': 'ORDER_CANCELLED',
    'rejected': 'ORDER_REJECTED',
    'sent to production': 'PRODUCTION_DISPATCHED',
    'error': 'SYSTEM_ERROR',
  };
  
  const activityType = typeMap[action.toLowerCase()] || 'ORDER_CREATED';
  const config = ACTIVITY_TYPES[activityType] || ACTIVITY_TYPES.ORDER_CREATED;
  
  // Build message: "[orderNumber] [action]"
  const message = customMessage || `${orderNumber} ${action}`;
  
  // Resolve station info
  let stationInfo = { code: 'SYSTEM', name: 'System', color: '#94a3b8' };
  if (station) {
    stationInfo = { code: station.code, name: station.name, color: station.color };
  }
  
  return {
    id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: type,
    activityType: activityType,
    message,
    icon: config.icon,
    color: config.color,
    category: config.category,
    station: stationInfo.code,
    stationName: stationInfo.name,
    stationColor: stationInfo.color,
    orderNumber,
    timestamp: new Date().toISOString(),
  };
}

/**
 * Create task/production entry
 */
export function createTaskEntry(action, taskName, station, details) {
  const typeMap = {
    'scheduled': 'PRODUCTION_SCHEDULED',
    'dispatched': 'PRODUCTION_DISPATCHED',
    'started': 'TASK_STARTED',
    'completed': 'TASK_COMPLETED',
  };
  
  const type = typeMap[action.toLowerCase()] || 'TASK_STARTED';
  return createActivityEntry(type, {
    target: taskName,
    station,
    details,
  });
}

/**
 * Create inventory entry
 */
export function createInventoryEntry(action, itemName, station, quantity) {
  const typeMap = {
    'credited': 'STOCK_CREDITED',
    'debited': 'STOCK_DEBITED',
    'low': 'STOCK_LOW',
    'critical': 'STOCK_CRITICAL',
    'adjusted': 'STOCK_ADJUSTED',
  };
  
  const type = typeMap[action.toLowerCase()] || 'STOCK_ADJUSTED';
  const details = quantity ? `qty: ${quantity}` : null;
  
  return createActivityEntry(type, {
    target: itemName,
    station,
    details,
  });
}

/**
 * Create system entry
 * @param {string} message - System message
 * @param {string} type - Notification type: 'success', 'error', 'warning', 'info'
 * @param {Object|null} station - Station info from WORKSTATIONS
 */
export function createSystemEntry(message, type = 'info', station = null) {
  const activityType = type === 'error' ? 'SYSTEM_ERROR' : 'SYSTEM_INFO';
  const config = ACTIVITY_TYPES[activityType];
  
  // Resolve station info
  let stationInfo = { code: 'SYSTEM', name: 'System', color: '#94a3b8' };
  if (station) {
    stationInfo = { code: station.code, name: station.name, color: station.color };
  }
  
  return {
    id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: type,
    activityType: activityType,
    message,
    icon: config.icon,
    color: type === 'error' ? '#ef4444' : type === 'warning' ? '#f59e0b' : type === 'success' ? '#10b981' : config.color,
    category: config.category,
    station: stationInfo.code,
    stationName: stationInfo.name,
    stationColor: stationInfo.color,
    timestamp: new Date().toISOString(),
  };
}

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
