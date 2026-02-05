/**
 * Unified Order Card Configuration
 * Defines order type properties, status mappings, and action configurations
 */

// ═══════════════════════════════════════════════════════════════
// ORDER TYPE CONFIGURATION
// ═══════════════════════════════════════════════════════════════

export const ORDER_TYPES = {
  CUSTOMER_ORDER: 'customer',
  WAREHOUSE_ORDER: 'warehouse',
  PRODUCTION_ORDER: 'production',
  PRODUCTION_CONTROL_ORDER: 'controlProd',
  ASSEMBLY_CONTROL_ORDER: 'controlAsm',
  WORKSTATION_ORDER: 'workstation',
  SUPPLY_ORDER: 'supply',
  FINAL_ASSEMBLY_ORDER: 'finalAssembly',
};

export const ORDER_TYPE_CONFIG = {
  [ORDER_TYPES.CUSTOMER_ORDER]: {
    icon: '📦',
    label: 'Customer Order',
    prefix: 'CUST',
    cssClass: 'customer',
    showScenario: true,
    showPriority: false,
    showItems: true,
    showTimestamps: ['OD'],
    referenceField: null,
  },
  
  [ORDER_TYPES.WAREHOUSE_ORDER]: {
    icon: '🏢',
    label: 'Warehouse Order',
    prefix: 'WO',
    cssClass: 'warehouse',
    showScenario: true,
    showPriority: false,
    showItems: true,
    showTimestamps: ['OD'],
    referenceField: 'customerOrderId',
    referencePrefix: 'CUST',
  },
  
  [ORDER_TYPES.PRODUCTION_ORDER]: {
    icon: '⚙️',
    label: 'Production Order',
    prefix: 'PO',
    cssClass: 'production',
    showScenario: false,
    showPriority: true,
    showItems: true,
    showTimestamps: ['OD', 'DD'],
    referenceField: 'warehouseOrderId',
    referencePrefix: 'WO',
  },
  
  [ORDER_TYPES.PRODUCTION_CONTROL_ORDER]: {
    icon: '🔧',
    label: 'Production Control',
    prefix: 'PCO',
    cssClass: 'controlProd',
    showScenario: false,
    showPriority: true,
    showItems: true,
    showTimestamps: ['TS', 'TC', 'AS', 'AF'],
    referenceField: 'productionOrderId',
    referencePrefix: 'PO',
  },
  
  [ORDER_TYPES.ASSEMBLY_CONTROL_ORDER]: {
    icon: '🔩',
    label: 'Assembly Control',
    prefix: 'ACO',
    cssClass: 'controlAsm',
    showScenario: false,
    showPriority: true,
    showItems: true,
    showTimestamps: ['TS', 'TC', 'AS', 'AF'],
    referenceField: 'productionOrderId',
    referencePrefix: 'PO',
  },
  
  [ORDER_TYPES.WORKSTATION_ORDER]: {
    icon: '🏭',
    label: 'Workstation Order',
    prefix: 'WSO',
    cssClass: 'workstation',
    showScenario: false,
    showPriority: true,
    showItems: true,
    showTimestamps: ['TS', 'TC', 'AS', 'AF'],
    referenceField: 'controlOrderId',
    referencePrefix: 'CO',
  },
  
  [ORDER_TYPES.SUPPLY_ORDER]: {
    icon: '📦',
    label: 'Supply Order',
    prefix: 'SO',
    cssClass: 'supply',
    showScenario: false,
    showPriority: true,
    showItems: true,
    showTimestamps: ['OD', 'ND', 'FF'],
    showWorkstationFlow: true,
    referenceField: 'sourceOrderId',
  },
  
  [ORDER_TYPES.FINAL_ASSEMBLY_ORDER]: {
    icon: '🔨',
    label: 'Final Assembly',
    prefix: 'FA',
    cssClass: 'finalAssembly',
    showScenario: false,
    showPriority: false,
    showItems: true,
    showTimestamps: ['OD', 'ST', 'CT', 'SUB'],
    referenceField: 'warehouseOrderId',
    referencePrefix: 'WO',
  },
};

// ═══════════════════════════════════════════════════════════════
// STATUS CONFIGURATION
// ═══════════════════════════════════════════════════════════════

export const STATUS_CONFIG = {
  // Common statuses
  PENDING: { label: 'Pending', cssClass: 'pending' },
  CONFIRMED: { label: 'Confirmed', cssClass: 'confirmed' },
  IN_PROGRESS: { label: 'In Progress', cssClass: 'inProgress' },
  COMPLETED: { label: 'Completed', cssClass: 'completed' },
  CANCELLED: { label: 'Cancelled', cssClass: 'error' },
  
  // Customer Order specific
  PROCESSING: { label: 'Processing', cssClass: 'inProgress' },
  DELIVERED: { label: 'Delivered', cssClass: 'completed' },
  
  // Warehouse Order specific
  AWAITING_PRODUCTION: { label: 'Awaiting', cssClass: 'waiting' },
  MODULES_READY: { label: 'Ready', cssClass: 'confirmed' },
  FULFILLED: { label: 'Fulfilled', cssClass: 'completed' },
  
  // Production Order specific
  CREATED: { label: 'Created', cssClass: 'pending' },
  SUBMITTED: { label: 'Submitted', cssClass: 'confirmed' },
  SCHEDULED: { label: 'Scheduled', cssClass: 'confirmed' },
  DISPATCHED: { label: 'Dispatched', cssClass: 'inProgress' },
  IN_PRODUCTION: { label: 'In Production', cssClass: 'inProgress' },
  
  // Control Order specific
  ASSIGNED: { label: 'Assigned', cssClass: 'confirmed' },
  HALTED: { label: 'Halted', cssClass: 'error' },
  ABANDONED: { label: 'Abandoned', cssClass: 'error' },
  
  // Workstation Order specific
  WAITING_FOR_PARTS: { label: 'Waiting', cssClass: 'waiting' },
  
  // Supply Order specific
  REJECTED: { label: 'Rejected', cssClass: 'error' },
};

// ═══════════════════════════════════════════════════════════════
// PRIORITY CONFIGURATION
// ═══════════════════════════════════════════════════════════════

export const PRIORITY_CONFIG = {
  URGENT: { label: 'Urgent', cssClass: 'urgent' },
  HIGH: { label: 'High', cssClass: 'high' },
  NORMAL: { label: 'Normal', cssClass: 'normal' },
  LOW: { label: 'Low', cssClass: 'low' },
};

// ═══════════════════════════════════════════════════════════════
// SCENARIO CONFIGURATION
// ═══════════════════════════════════════════════════════════════

export const SCENARIO_CONFIG = {
  DIRECT_FULFILLMENT: { label: 'S1', cssClass: 's1' },
  WAREHOUSE_ORDER_NEEDED: { label: 'S2', cssClass: 's2' },
  PRODUCTION_REQUIRED: { label: 'S3', cssClass: 's3' },
  DIRECT_PRODUCTION: { label: 'S4', cssClass: 's4' },
};

// ═══════════════════════════════════════════════════════════════
// TIMESTAMP LABELS
// ═══════════════════════════════════════════════════════════════

export const TIMESTAMP_LABELS = {
  OD: 'OD',  // Order Date
  DD: 'DD',  // Due Date
  ND: 'ND',  // Needed By
  TS: 'TS',  // Target Start
  TC: 'TC',  // Target Completion
  AS: 'AS',  // Actual Start
  AF: 'AF',  // Actual Finish
  ST: 'ST',  // Start Time
  CT: 'CT',  // Completion Time
  SUB: 'SUB', // Submitted At
  FF: 'FF',  // Fulfilled At
};

// ═══════════════════════════════════════════════════════════════
// ACTION CONFIGURATION
// ═══════════════════════════════════════════════════════════════

export const ACTION_TYPES = {
  VIEW: 'view',
  CONFIRM: 'confirm',
  START: 'start',
  COMPLETE: 'complete',
  SUBMIT: 'submit',
  FULFILL: 'fulfill',
  REQUEST_WO: 'requestWO',
  REQUEST_PRODUCTION: 'requestProduction',
  ORDER_PRODUCTION: 'orderProduction',
  SCHEDULE: 'schedule',
  DISPATCH: 'dispatch',
  HALT: 'halt',
  RESUME: 'resume',
  CANCEL: 'cancel',
  REJECT: 'reject',
  DETAILS: 'details',
  REQUEST_PARTS: 'requestParts',
  VIEW_SUPPLY: 'viewSupply',
  VIEW_WORKSTATION: 'viewWorkstation',
};

export const ACTION_CONFIG = {
  [ACTION_TYPES.VIEW]: { label: 'View', variant: 'ghost' },
  [ACTION_TYPES.CONFIRM]: { label: 'Confirm', variant: 'primary' },
  [ACTION_TYPES.START]: { label: 'Start', variant: 'primary' },
  [ACTION_TYPES.COMPLETE]: { label: 'Complete', variant: 'success' },
  [ACTION_TYPES.SUBMIT]: { label: 'Submit', variant: 'primary' },
  [ACTION_TYPES.FULFILL]: { label: 'Fulfill', variant: 'success' },
  [ACTION_TYPES.REQUEST_WO]: { label: 'Request WO', variant: 'primary' },
  [ACTION_TYPES.REQUEST_PRODUCTION]: { label: 'Order Production', variant: 'primary' },
  [ACTION_TYPES.ORDER_PRODUCTION]: { label: 'Order Production', variant: 'primary' },
  [ACTION_TYPES.SCHEDULE]: { label: 'Schedule', variant: 'primary' },
  [ACTION_TYPES.DISPATCH]: { label: 'Dispatch', variant: 'primary' },
  [ACTION_TYPES.HALT]: { label: 'Halt', variant: 'warning' },
  [ACTION_TYPES.RESUME]: { label: 'Resume', variant: 'primary' },
  [ACTION_TYPES.CANCEL]: { label: 'Cancel', variant: 'secondary' },
  [ACTION_TYPES.REJECT]: { label: 'Reject', variant: 'danger' },
  [ACTION_TYPES.DETAILS]: { label: 'Details', variant: 'secondary' },
  [ACTION_TYPES.REQUEST_PARTS]: { label: 'Request Parts', variant: 'secondary' },
  [ACTION_TYPES.VIEW_SUPPLY]: { label: 'View Supply', variant: 'secondary' },
  [ACTION_TYPES.VIEW_WORKSTATION]: { label: 'View Workstation', variant: 'secondary' },
};

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Get status configuration for a given status string
 */
export const getStatusConfig = (status) => {
  if (!status) return STATUS_CONFIG.PENDING;
  const normalizedStatus = status.toUpperCase().replace(/-/g, '_').replace(/ /g, '_');
  return STATUS_CONFIG[normalizedStatus] || { label: status, cssClass: 'pending' };
};

/**
 * Get priority configuration for a given priority string
 */
export const getPriorityConfig = (priority) => {
  if (!priority) return null;
  const normalizedPriority = priority.toUpperCase();
  return PRIORITY_CONFIG[normalizedPriority] || null;
};

/**
 * Get scenario configuration for a given triggerScenario string
 */
export const getScenarioConfig = (scenario) => {
  if (!scenario) return null;
  const normalizedScenario = scenario.toUpperCase().replace(/-/g, '_');
  return SCENARIO_CONFIG[normalizedScenario] || null;
};

/**
 * Get order type configuration
 */
export const getOrderTypeConfig = (orderType) => {
  return ORDER_TYPE_CONFIG[orderType] || ORDER_TYPE_CONFIG[ORDER_TYPES.CUSTOMER_ORDER];
};

/**
 * Format order number with prefix
 */
export const formatOrderNumber = (order, orderType) => {
  const config = getOrderTypeConfig(orderType);
  if (order.orderNumber) return `#${order.orderNumber}`;
  if (order.id) return `#${config.prefix}-${String(order.id).padStart(4, '0')}`;
  return '#---';
};

/**
 * Get reference subtitle text
 */
export const getReferenceSubtitle = (order, orderType) => {
  const config = getOrderTypeConfig(orderType);
  
  // Custom subtitle logic for specific order types
  if (orderType === ORDER_TYPES.WORKSTATION_ORDER) {
    const wsName = order.workstationName || `WS-${order.workstationId}`;
    return wsName;
  }
  
  if (orderType === ORDER_TYPES.SUPPLY_ORDER) {
    // Supply orders show source order reference
    if (order.sourceOrderType && order.sourceOrderId) {
      return `${order.sourceOrderType}: #${order.sourceOrderId}`;
    }
  }
  
  // Default: show parent reference
  if (config.referenceField && order[config.referenceField]) {
    return `Source: ${config.referencePrefix || ''}-${order[config.referenceField]}`;
  }
  
  return config.label;
};

/**
 * Get actions for an order based on status and scenario
 */
export const getOrderActions = (order, orderType) => {
  const status = order.status?.toUpperCase().replace(/-/g, '_').replace(/ /g, '_');
  const scenario = order.triggerScenario?.toUpperCase().replace(/-/g, '_');
  const actions = [];
  
  switch (orderType) {
    case ORDER_TYPES.CUSTOMER_ORDER:
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.CONFIRM, ...ACTION_CONFIG[ACTION_TYPES.CONFIRM] });
      } else if (status === 'CONFIRMED') {
        if (scenario === 'DIRECT_FULFILLMENT') {
          actions.push({ type: ACTION_TYPES.FULFILL, label: 'Fulfill', variant: 'success' });
        } else if (scenario === 'WAREHOUSE_ORDER_NEEDED') {
          actions.push({ type: ACTION_TYPES.REQUEST_WO, ...ACTION_CONFIG[ACTION_TYPES.REQUEST_WO] });
        } else if (scenario === 'DIRECT_PRODUCTION') {
          actions.push({ type: ACTION_TYPES.ORDER_PRODUCTION, ...ACTION_CONFIG[ACTION_TYPES.ORDER_PRODUCTION] });
        }
        actions.push({ type: ACTION_TYPES.CANCEL, ...ACTION_CONFIG[ACTION_TYPES.CANCEL] });
      } else if (status === 'PROCESSING') {
        actions.push({ type: ACTION_TYPES.COMPLETE, ...ACTION_CONFIG[ACTION_TYPES.COMPLETE], disabled: !order.canComplete });
      }
      break;
      
    case ORDER_TYPES.WAREHOUSE_ORDER:
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.CONFIRM, ...ACTION_CONFIG[ACTION_TYPES.CONFIRM] });
      } else if (status === 'CONFIRMED') {
        if (scenario === 'DIRECT_FULFILLMENT') {
          actions.push({ type: ACTION_TYPES.FULFILL, ...ACTION_CONFIG[ACTION_TYPES.FULFILL] });
        } else if (scenario === 'PRODUCTION_REQUIRED') {
          actions.push({ type: ACTION_TYPES.REQUEST_PRODUCTION, ...ACTION_CONFIG[ACTION_TYPES.REQUEST_PRODUCTION] });
        }
      } else if (status === 'MODULES_READY') {
        actions.push({ type: ACTION_TYPES.FULFILL, ...ACTION_CONFIG[ACTION_TYPES.FULFILL] });
      } else if (status === 'AWAITING_PRODUCTION') {
        actions.push({ type: ACTION_TYPES.FULFILL, ...ACTION_CONFIG[ACTION_TYPES.FULFILL], disabled: true });
      }
      break;
      
    case ORDER_TYPES.PRODUCTION_ORDER:
      // ProductionOrder workflow:
      // CREATED/PENDING → Confirm
      // CONFIRMED → Schedule with SimAL
      // SCHEDULED → Dispatch to create control orders
      // DISPATCHED/IN_PRODUCTION → Monitoring (auto-completes when control orders done)
      // COMPLETED → No actions (terminal state)
      if (status === 'CREATED' || status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.CONFIRM, ...ACTION_CONFIG[ACTION_TYPES.CONFIRM] });
      } else if (status === 'CONFIRMED') {
        actions.push({ type: ACTION_TYPES.SCHEDULE, ...ACTION_CONFIG[ACTION_TYPES.SCHEDULE] });
      } else if (status === 'SCHEDULED') {
        actions.push({ type: ACTION_TYPES.DISPATCH, ...ACTION_CONFIG[ACTION_TYPES.DISPATCH] });
      } else if (status === 'DISPATCHED' || status === 'IN_PRODUCTION') {
        // Show status indicator - control orders are running
        actions.push({ type: ACTION_TYPES.DISPATCH, label: 'In Production', variant: 'ghost', disabled: true });
      }
      // COMPLETED status has no actions
      break;
      
    case ORDER_TYPES.PRODUCTION_CONTROL_ORDER:
    case ORDER_TYPES.ASSEMBLY_CONTROL_ORDER:
      // Control order workflow:
      // PENDING → Confirm
      // CONFIRMED (no supply) → Request Parts
      // CONFIRMED (supply pending) → Waiting for Parts (disabled Dispatch)
      // CONFIRMED (supply fulfilled) → Dispatch to Workstation
      // ASSIGNED → Waiting for workstation completion (no actions)
      // IN_PROGRESS → Halt
      // HALTED → Resume
      // COMPLETED → No actions (terminal state)
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.CONFIRM, ...ACTION_CONFIG[ACTION_TYPES.CONFIRM] });
      } else if (status === 'CONFIRMED') {
        // Check if supply order exists and its status
        const hasSupplyOrder = order.supplyOrderId || order.supplyOrderStatus;
        const supplyFulfilled = order.supplyOrderStatus === 'FULFILLED';
        
        if (!hasSupplyOrder) {
          // No supply order yet - show Request Parts
          actions.push({ type: ACTION_TYPES.REQUEST_PARTS, ...ACTION_CONFIG[ACTION_TYPES.REQUEST_PARTS] });
        } else if (supplyFulfilled) {
          // Supply fulfilled - can dispatch
          actions.push({ type: ACTION_TYPES.DISPATCH, ...ACTION_CONFIG[ACTION_TYPES.DISPATCH] });
        } else {
          // Supply order exists but not fulfilled - show disabled waiting button
          actions.push({ type: ACTION_TYPES.DISPATCH, label: 'Waiting for Parts', variant: 'secondary', disabled: true });
        }
      } else if (status === 'ASSIGNED') {
        // Workstation order created and running - show status indicator only
        actions.push({ type: ACTION_TYPES.DISPATCH, label: 'Workstation Active', variant: 'ghost', disabled: true });
      } else if (status === 'IN_PROGRESS') {
        actions.push({ type: ACTION_TYPES.HALT, ...ACTION_CONFIG[ACTION_TYPES.HALT] });
      } else if (status === 'HALTED') {
        actions.push({ type: ACTION_TYPES.RESUME, ...ACTION_CONFIG[ACTION_TYPES.RESUME] });
      }
      // COMPLETED status has no actions (terminal state)
      break;
      
    case ORDER_TYPES.WORKSTATION_ORDER:
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.START, ...ACTION_CONFIG[ACTION_TYPES.START] });
      } else if (status === 'WAITING_FOR_PARTS') {
        actions.push({ type: ACTION_TYPES.START, ...ACTION_CONFIG[ACTION_TYPES.START], disabled: true });
      } else if (status === 'IN_PROGRESS') {
        actions.push({ type: ACTION_TYPES.COMPLETE, ...ACTION_CONFIG[ACTION_TYPES.COMPLETE] });
        actions.push({ type: ACTION_TYPES.HALT, ...ACTION_CONFIG[ACTION_TYPES.HALT] });
      } else if (status === 'HALTED') {
        actions.push({ type: ACTION_TYPES.RESUME, ...ACTION_CONFIG[ACTION_TYPES.RESUME] });
      }
      break;
      
    case ORDER_TYPES.SUPPLY_ORDER:
      // Supply orders have no confirm step - View action always available
      // PENDING → View + Fulfill / Reject
      // IN_PROGRESS → View + Fulfill
      // FULFILLED/REJECTED/CANCELLED → View only
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.VIEW, ...ACTION_CONFIG[ACTION_TYPES.VIEW] });
        actions.push({ type: ACTION_TYPES.FULFILL, ...ACTION_CONFIG[ACTION_TYPES.FULFILL] });
        actions.push({ type: ACTION_TYPES.REJECT, ...ACTION_CONFIG[ACTION_TYPES.REJECT] });
      } else if (status === 'IN_PROGRESS') {
        actions.push({ type: ACTION_TYPES.VIEW, ...ACTION_CONFIG[ACTION_TYPES.VIEW] });
        actions.push({ type: ACTION_TYPES.FULFILL, ...ACTION_CONFIG[ACTION_TYPES.FULFILL] });
      } else {
        // FULFILLED, REJECTED, CANCELLED - view only
        actions.push({ type: ACTION_TYPES.VIEW, ...ACTION_CONFIG[ACTION_TYPES.VIEW] });
      }
      break;
      
    case ORDER_TYPES.FINAL_ASSEMBLY_ORDER:
      // Final Assembly workflow:
      // PENDING → Confirm Order
      // CONFIRMED → Start Assembly
      // IN_PROGRESS → Complete Assembly + Halt
      // COMPLETED (or COMPLETED_ASSEMBLY) → Submit Completion
      // SUBMITTED → View Details only
      if (status === 'PENDING') {
        actions.push({ type: ACTION_TYPES.CONFIRM, ...ACTION_CONFIG[ACTION_TYPES.CONFIRM] });
      } else if (status === 'CONFIRMED') {
        actions.push({ type: ACTION_TYPES.START, ...ACTION_CONFIG[ACTION_TYPES.START] });
      } else if (status === 'IN_PROGRESS') {
        actions.push({ type: ACTION_TYPES.COMPLETE, ...ACTION_CONFIG[ACTION_TYPES.COMPLETE] });
        actions.push({ type: ACTION_TYPES.HALT, ...ACTION_CONFIG[ACTION_TYPES.HALT] });
      } else if (status === 'HALTED') {
        actions.push({ type: ACTION_TYPES.RESUME, ...ACTION_CONFIG[ACTION_TYPES.RESUME] });
      } else if (status === 'COMPLETED' || status === 'COMPLETED_ASSEMBLY') {
        // Assembly work done, awaiting submission to Plant Warehouse
        actions.push({ type: ACTION_TYPES.SUBMIT, ...ACTION_CONFIG[ACTION_TYPES.SUBMIT] });
      }
      // SUBMITTED status = final, no buttons needed
      break;
  }
  
  return actions;
};

/**
 * Format timestamp value for display
 */
export const formatTimestamp = (value, format = 'datetime') => {
  if (!value) return '—';
  
  try {
    const date = new Date(value);
    if (isNaN(date.getTime())) return '—';
    
    if (format === 'time') {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
    }
    if (format === 'date') {
      return date.toLocaleDateString('en-US', { year: 'numeric', month: '2-digit', day: '2-digit' });
    }
    // datetime
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  } catch {
    return '—';
  }
};

/**
 * Get timestamps to display for an order
 */
export const getOrderTimestamps = (order, orderType) => {
  const config = getOrderTypeConfig(orderType);
  const timestamps = [];
  
  const fieldMap = {
    OD: ['orderDate', 'createdAt', 'created'],
    DD: ['dueDate', 'deliveryDate'],
    ND: ['neededBy', 'requiredBy'],
    TS: ['targetStart', 'scheduledStart', 'plannedStart'],
    TC: ['targetCompletion', 'scheduledEnd', 'plannedEnd'],
    AS: ['actualStart', 'startedAt', 'startTime'],
    AF: ['actualFinish', 'completedAt', 'finishTime'],
    ST: ['startTime', 'actualStart'],
    CT: ['completionTime', 'completedAt'],
    SUB: ['submittedAt', 'submitTime'],
    FF: ['fulfilledAt', 'fulfillmentTime'],
  };
  
  for (const label of config.showTimestamps || []) {
    const fields = fieldMap[label] || [];
    let value = null;
    
    for (const field of fields) {
      if (order[field]) {
        value = order[field];
        break;
      }
    }
    
    timestamps.push({
      label: TIMESTAMP_LABELS[label],
      value: formatTimestamp(value, label === 'TS' || label === 'TC' || label === 'AS' || label === 'AF' || label === 'ST' || label === 'CT' ? 'time' : 'datetime'),
      isOverdue: label === 'DD' && value && new Date(value) < new Date(),
    });
  }
  
  return timestamps;
};
