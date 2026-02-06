/**
 * Workstation Panel Configurations
 * 
 * Defines the 4 panels that appear in the second row of each workstation dashboard.
 * Each workstation has different operational needs, so panels are customized.
 * 
 * Panel Types:
 * - orders: Order status summary
 * - queue: Work queue/upcoming tasks
 * - materials: Material/parts status
 * - machine: Machine/equipment status
 * - inventory: Inventory levels
 * - quality: Quality metrics
 * - production: Production progress
 * - actions: Quick action buttons
 * - alerts: Warnings and notifications
 * - throughput: Processing rates
 */

// Panel configuration for each workstation
export const WORKSTATION_PANELS = {
  // WS-1: Injection Molding - First manufacturing step
  1: {
    name: 'Injection Molding',
    panels: [
      {
        type: 'orders',
        icon: '📋',
        title: 'Molding Orders',
        statsKeys: ['pending', 'progress', 'completed', 'rate']
      },
      {
        type: 'machine',
        icon: '⚙️',
        title: 'Machine Status',
        statsKeys: ['temperature', 'pressure', 'cycleTime', 'efficiency']
      },
      {
        type: 'materials',
        icon: '🧱',
        title: 'Raw Materials',
        statsKeys: ['plastic', 'colorant', 'available', 'needed']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'START_BATCH', label: 'Start Batch', icon: '▶️', primary: true },
          { type: 'PAUSE', label: 'Pause', icon: '⏸️' },
          { type: 'QUALITY_CHECK', label: 'Quality Check', icon: '✓' },
          { type: 'MAINTENANCE', label: 'Maintenance', icon: '🔧' }
        ]
      }
    ]
  },

  // WS-2: Parts Pre-Production - Processing molded parts
  2: {
    name: 'Parts Pre-Production',
    panels: [
      {
        type: 'queue',
        icon: '📥',
        title: 'Work Queue',
        statsKeys: ['incoming', 'processing', 'outgoing', 'today']
      },
      {
        type: 'production',
        icon: '🔄',
        title: 'Processing',
        statsKeys: ['batches', 'parts', 'yield', 'rejects']
      },
      {
        type: 'materials',
        icon: '📦',
        title: 'Parts Inventory',
        statsKeys: ['fromWS1', 'inProcess', 'ready', 'buffer']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'NEXT_BATCH', label: 'Next Batch', icon: '▶️', primary: true },
          { type: 'QUALITY', label: 'QC Check', icon: '✓' },
          { type: 'TRANSFER', label: 'Transfer Out', icon: '📤' },
          { type: 'REJECT', label: 'Log Reject', icon: '⚠️' }
        ]
      }
    ]
  },

  // WS-3: Part Finishing - Final manufacturing step
  3: {
    name: 'Part Finishing',
    panels: [
      {
        type: 'orders',
        icon: '📋',
        title: 'Finishing Orders',
        statsKeys: ['pending', 'progress', 'completed', 'rate']
      },
      {
        type: 'quality',
        icon: '✓',
        title: 'Quality Control',
        statsKeys: ['passed', 'failed', 'rework', 'yield']
      },
      {
        type: 'throughput',
        icon: '📊',
        title: 'Throughput',
        statsKeys: ['hourly', 'daily', 'target', 'variance']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'COMPLETE_BATCH', label: 'Complete', icon: '✅', primary: true },
          { type: 'QUALITY_HOLD', label: 'QC Hold', icon: '⏸️' },
          { type: 'SHIP_WS9', label: 'Ship to WS-9', icon: '📤' },
          { type: 'REPORT', label: 'Report Issue', icon: '⚠️' }
        ]
      }
    ]
  },

  // WS-4: Gear Assembly - Builds gear modules
  4: {
    name: 'Gear Assembly',
    panels: [
      {
        type: 'orders',
        icon: '⚙️',
        title: 'Assembly Orders',
        statsKeys: ['pending', 'progress', 'completed', 'rate']
      },
      {
        type: 'materials',
        icon: '🔩',
        title: 'Parts Available',
        statsKeys: ['gears', 'shafts', 'bearings', 'kits']
      },
      {
        type: 'production',
        icon: '🏭',
        title: 'Build Progress',
        statsKeys: ['modules', 'tested', 'passed', 'shipped']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'START_MODULE', label: 'Start Module', icon: '▶️', primary: true },
          { type: 'TEST', label: 'Run Test', icon: '🔬' },
          { type: 'SHIP_WS8', label: 'Ship to WS-8', icon: '📤' },
          { type: 'REQUEST', label: 'Request Parts', icon: '📥' }
        ]
      }
    ]
  },

  // WS-5: Motor Assembly - Builds motor modules
  5: {
    name: 'Motor Assembly',
    panels: [
      {
        type: 'orders',
        icon: '🔌',
        title: 'Motor Orders',
        statsKeys: ['pending', 'progress', 'completed', 'rate']
      },
      {
        type: 'materials',
        icon: '🔩',
        title: 'Components',
        statsKeys: ['motors', 'wiring', 'housing', 'kits']
      },
      {
        type: 'quality',
        icon: '⚡',
        title: 'Test Results',
        statsKeys: ['tested', 'passed', 'failed', 'yield']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'START_MOTOR', label: 'Start Build', icon: '▶️', primary: true },
          { type: 'POWER_TEST', label: 'Power Test', icon: '⚡' },
          { type: 'SHIP_WS8', label: 'Ship to WS-8', icon: '📤' },
          { type: 'RETEST', label: 'Retest', icon: '🔄' }
        ]
      }
    ]
  },

  // WS-6: Final Assembly - Assembles complete products
  6: {
    name: 'Final Assembly',
    panels: [
      {
        type: 'orders',
        icon: '🎯',
        title: 'Final Assembly',
        statsKeys: ['pending', 'progress', 'completed', 'rate']
      },
      {
        type: 'materials',
        icon: '📦',
        title: 'Module Kits',
        statsKeys: ['gearMod', 'motorMod', 'housing', 'ready']
      },
      {
        type: 'production',
        icon: '🏭',
        title: 'Products',
        statsKeys: ['assembled', 'tested', 'packed', 'shipped']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'START_PRODUCT', label: 'Start Product', icon: '▶️', primary: true },
          { type: 'FINAL_TEST', label: 'Final Test', icon: '✓' },
          { type: 'PACK', label: 'Pack', icon: '📦' },
          { type: 'SHIP_WS7', label: 'Ship to WS-7', icon: '📤' }
        ]
      }
    ]
  },

  // WS-7: Plant Warehouse - Customer fulfillment
  7: {
    name: 'Plant Warehouse',
    panels: [
      {
        type: 'orders',
        icon: '📋',
        title: 'Customer Orders',
        statsKeys: ['pending', 'confirmed', 'processing', 'completed']
      },
      {
        type: 'inventory',
        icon: '📦',
        title: 'Product Stock',
        statsKeys: ['inStock', 'reserved', 'lowStock', 'outOfStock']
      },
      {
        type: 'fulfillment',
        icon: '🚚',
        title: 'Fulfillment',
        statsKeys: ['ready', 'picking', 'packed', 'shipped']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'NEW_ORDER', label: 'New Order', icon: '➕', primary: true },
          { type: 'FULFILL', label: 'Fulfill', icon: '📦' },
          { type: 'REQUEST_PROD', label: 'Request Prod', icon: '🏭' },
          { type: 'INVENTORY', label: 'Stock Count', icon: '📊' }
        ]
      }
    ]
  },

  // WS-8: Modules Supermarket - Internal warehouse for modules
  8: {
    name: 'Modules Supermarket',
    panels: [
      {
        type: 'orders',
        icon: '📋',
        title: 'Warehouse Orders',
        statsKeys: ['pending', 'confirmed', 'awaiting', 'fulfilled']
      },
      {
        type: 'inventory',
        icon: '🧩',
        title: 'Module Stock',
        statsKeys: ['gearMod', 'motorMod', 'lowStock', 'total']
      },
      {
        type: 'production',
        icon: '🏭',
        title: 'Production Status',
        statsKeys: ['ordered', 'inProgress', 'ready', 'overdue']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'FULFILL_ORDER', label: 'Fulfill', icon: '✅', primary: true },
          { type: 'ORDER_PROD', label: 'Order Prod', icon: '🏭' },
          { type: 'RECEIVE', label: 'Receive', icon: '📥' },
          { type: 'TRANSFER', label: 'Transfer', icon: '📤' }
        ]
      }
    ]
  },

  // WS-9: Parts Supply Warehouse - Raw materials/parts
  9: {
    name: 'Parts Supply',
    panels: [
      {
        type: 'orders',
        icon: '📋',
        title: 'Supply Orders',
        statsKeys: ['pending', 'picking', 'fulfilled', 'total']
      },
      {
        type: 'inventory',
        icon: '🧱',
        title: 'Parts Stock',
        statsKeys: ['available', 'reserved', 'lowStock', 'critical']
      },
      {
        type: 'demand',
        icon: '📊',
        title: 'Demand',
        statsKeys: ['ws1', 'ws2', 'ws3', 'assembly']
      },
      {
        type: 'actions',
        icon: '⚡',
        title: 'Quick Actions',
        actions: [
          { type: 'FULFILL_SUPPLY', label: 'Fulfill', icon: '✅', primary: true },
          { type: 'PICK', label: 'Pick Order', icon: '🛒' },
          { type: 'RECEIVE', label: 'Receive', icon: '📥' },
          { type: 'REORDER', label: 'Reorder', icon: '🔄' }
        ]
      }
    ]
  }
};

// Helper to get panels for a workstation
export function getWorkstationPanels(workstationId) {
  return WORKSTATION_PANELS[workstationId] || null;
}

// Default stats structure (to be filled with real data)
export const DEFAULT_STATS = {
  pending: { value: 0, label: 'Pending', variant: 'pending' },
  progress: { value: 0, label: 'In Progress', variant: 'progress' },
  completed: { value: 0, label: 'Completed', variant: 'completed' },
  rate: { value: '0%', label: 'Rate', variant: 'rate' },
  confirmed: { value: 0, label: 'Confirmed', variant: 'info' },
  processing: { value: 0, label: 'Processing', variant: 'progress' },
  fulfilled: { value: 0, label: 'Fulfilled', variant: 'success' },
  awaiting: { value: 0, label: 'Awaiting', variant: 'warning' },
  total: { value: 0, label: 'Total', variant: 'info' },
  inStock: { value: 0, label: 'In Stock', variant: 'success' },
  reserved: { value: 0, label: 'Reserved', variant: 'info' },
  lowStock: { value: 0, label: 'Low Stock', variant: 'warning' },
  critical: { value: 0, label: 'Critical', variant: 'danger' },
  outOfStock: { value: 0, label: 'Out', variant: 'danger' }
};
