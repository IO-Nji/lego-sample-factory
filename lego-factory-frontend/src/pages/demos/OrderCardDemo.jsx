/**
 * UnifiedOrderCard Demo Page
 * Shows all order card variants for testing and review
 */

import React, { useState } from 'react';
import UnifiedOrderCard, { ORDER_TYPES } from '../../components/orders/UnifiedOrderCard';
import { logger } from '../../utils/logger';

// Sample order data for each type
const SAMPLE_ORDERS = {
  [ORDER_TYPES.CUSTOMER_ORDER]: {
    id: 1,
    orderNumber: 'CUST-2026-0001',
    status: 'CONFIRMED',
    triggerScenario: 'WAREHOUSE_ORDER_NEEDED',
    items: [
      { id: 1, name: 'Dump Truck Model', itemType: 'PRODUCT', quantity: 10, stockAvailable: true },
      { id: 2, name: 'Garbage Truck', itemType: 'PRODUCT', quantity: 5, fulfilledQuantity: 3, stockAvailable: 'partial' },
    ],
    createdAt: '2026-02-05T10:30:00',
    notes: 'Urgent customer request - needs delivery by end of day.',
  },
  
  [ORDER_TYPES.WAREHOUSE_ORDER]: {
    id: 42,
    orderNumber: 'WO-2026-0042',
    status: 'AWAITING_PRODUCTION',
    triggerScenario: 'PRODUCTION_REQUIRED',
    customerOrderId: 1,
    items: [
      { id: 1, name: 'Gear Module A', itemType: 'MODULE', quantity: 20, stockAvailable: false },
      { id: 2, name: 'Motor Module B', itemType: 'MODULE', quantity: 20, stockAvailable: false },
      { id: 3, name: 'Chassis Module', itemType: 'MODULE', quantity: 10, stockAvailable: false },
      { id: 4, name: 'Wheel Assembly', itemType: 'MODULE', quantity: 40, stockAvailable: false },
    ],
    createdAt: '2026-02-05T10:35:00',
  },
  
  [ORDER_TYPES.PRODUCTION_ORDER]: {
    id: 15,
    orderNumber: 'PO-2026-0015',
    status: 'IN_PRODUCTION',
    priority: 'HIGH',
    warehouseOrderId: 42,
    items: [
      { id: 1, name: 'Dump Truck Model (Red)', itemType: 'PRODUCT', quantity: 10 },
    ],
    createdAt: '2026-02-05T10:40:00',
    dueDate: '2026-02-05T14:00:00',
  },
  
  [ORDER_TYPES.PRODUCTION_CONTROL_ORDER]: {
    id: 31,
    orderNumber: 'PCO-2026-0031',
    status: 'IN_PROGRESS',
    priority: 'URGENT',
    productionOrderId: 15,
    items: [
      { id: 1, name: 'Chassis Part A', itemType: 'PART', quantity: 30, fulfilledQuantity: 15 },
    ],
    targetStart: '2026-02-05T10:45:00',
    targetCompletion: '2026-02-05T12:30:00',
    actualStart: '2026-02-05T10:48:00',
  },
  
  [ORDER_TYPES.ASSEMBLY_CONTROL_ORDER]: {
    id: 22,
    orderNumber: 'ACO-2026-0022',
    status: 'PENDING',
    priority: 'NORMAL',
    productionOrderId: 15,
    items: [
      { id: 1, name: 'Gear Assembly', itemType: 'MODULE', quantity: 10 },
    ],
    targetStart: '2026-02-05T12:30:00',
    targetCompletion: '2026-02-05T14:00:00',
  },
  
  [ORDER_TYPES.SUPPLY_ORDER]: {
    id: 88,
    orderNumber: 'SO-2026-0088',
    status: 'PENDING',
    priority: 'HIGH',
    sourceOrderType: 'PCO',
    sourceOrderId: 31,
    fromWorkstation: 9,
    toWorkstation: 1,
    items: [
      { id: 1, name: 'Plastic Pellets (Red)', itemType: 'PART', quantity: 500 },
      { id: 2, name: 'Metal Axles', itemType: 'PART', quantity: 40 },
    ],
    createdAt: '2026-02-05T10:42:00',
    neededBy: '2026-02-05T10:45:00',
  },
  
  [ORDER_TYPES.WORKSTATION_ORDER]: {
    id: 156,
    orderNumber: 'IM-2026-0156',
    status: 'IN_PROGRESS',
    workstationId: 1,
    workstationName: 'Injection Molding',
    controlOrderId: 31,
    outputName: 'Chassis Body (Red)',
    outputQuantity: 10,
    completedQuantity: 8,
    targetStart: '2026-02-05T10:50:00',
    targetCompletion: '2026-02-05T11:20:00',
    actualStart: '2026-02-05T10:52:00',
  },
  
  [ORDER_TYPES.FINAL_ASSEMBLY_ORDER]: {
    id: 73,
    orderNumber: 'FA-2026-0073',
    status: 'COMPLETED',
    warehouseOrderId: 40,
    productName: 'Dump Truck Model (Blue)',
    quantity: 5,
    completedQuantity: 5,
    startTime: '2026-02-05T11:00:00',
    completionTime: '2026-02-05T11:45:00',
  },
};

const OrderCardDemo = () => {
  const [notifications, setNotifications] = useState({});
  
  const handleAction = (action, orderId, payload) => {
    logger.debug('OrderCardDemo', `Action: ${action}`, { orderId, payload });
    
    // Show notification for demo
    const orderType = payload.order.orderNumber?.split('-')[0] || 'ORDER';
    setNotifications(prev => ({
      ...prev,
      [orderId]: {
        type: 'info',
        message: `Action "${action}" triggered for ${orderType} #${orderId}`,
      },
    }));
    
    // Clear notification after 3 seconds
    setTimeout(() => {
      setNotifications(prev => {
        const next = { ...prev };
        delete next[orderId];
        return next;
      });
    }, 3000);
  };
  
  return (
    <div style={{ padding: '1.5rem', minHeight: '100vh', background: '#f3f4f6' }}>
      <h1 style={{ marginBottom: '0.5rem', fontSize: '1.5rem', color: '#1f2937' }}>
        🎨 Unified Order Card Demo
      </h1>
      <p style={{ marginBottom: '1.5rem', color: '#6b7280' }}>
        Single component rendering all 8 order types with consistent styling
      </p>
      
      <h2 style={{ marginBottom: '1rem', fontSize: '1rem', color: '#374151' }}>
        📦 All Order Types
      </h2>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        {Object.entries(SAMPLE_ORDERS).map(([orderType, order]) => (
          <UnifiedOrderCard
            key={orderType}
            orderType={orderType}
            order={order}
            onAction={handleAction}
            notification={notifications[order.id]}
          />
        ))}
      </div>
      
      <h2 style={{ marginTop: '2rem', marginBottom: '1rem', fontSize: '1rem', color: '#374151' }}>
        📐 Compact Variant
      </h2>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <UnifiedOrderCard
          orderType={ORDER_TYPES.CUSTOMER_ORDER}
          order={{
            ...SAMPLE_ORDERS[ORDER_TYPES.CUSTOMER_ORDER],
            id: 100,
            triggerScenario: 'DIRECT_FULFILLMENT',
            items: [{ id: 1, name: 'Fire Truck', itemType: 'PRODUCT', quantity: 2, stockAvailable: true }],
          }}
          onAction={handleAction}
          compact
        />
        <UnifiedOrderCard
          orderType={ORDER_TYPES.WORKSTATION_ORDER}
          order={{
            ...SAMPLE_ORDERS[ORDER_TYPES.WORKSTATION_ORDER],
            id: 101,
            status: 'PENDING',
          }}
          onAction={handleAction}
          compact
        />
        <UnifiedOrderCard
          orderType={ORDER_TYPES.SUPPLY_ORDER}
          order={{
            ...SAMPLE_ORDERS[ORDER_TYPES.SUPPLY_ORDER],
            id: 102,
            status: 'FULFILLED',
            items: [],
          }}
          onAction={handleAction}
          compact
        />
      </div>
      
      <h2 style={{ marginTop: '2rem', marginBottom: '1rem', fontSize: '1rem', color: '#374151' }}>
        🔔 With Notifications
      </h2>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
        <UnifiedOrderCard
          orderType={ORDER_TYPES.FINAL_ASSEMBLY_ORDER}
          order={SAMPLE_ORDERS[ORDER_TYPES.FINAL_ASSEMBLY_ORDER]}
          onAction={handleAction}
          notification={{ type: 'success', message: 'Ready to submit - will credit 5 units to WS-7' }}
        />
        <UnifiedOrderCard
          orderType={ORDER_TYPES.WAREHOUSE_ORDER}
          order={SAMPLE_ORDERS[ORDER_TYPES.WAREHOUSE_ORDER]}
          onAction={handleAction}
          notification={{ type: 'info', message: 'Production in progress - PO-2026-0015' }}
        />
        <UnifiedOrderCard
          orderType={ORDER_TYPES.SUPPLY_ORDER}
          order={SAMPLE_ORDERS[ORDER_TYPES.SUPPLY_ORDER]}
          onAction={handleAction}
          notification={{ type: 'warning', message: 'Parts urgently needed at WS-1' }}
        />
      </div>
    </div>
  );
};

export default OrderCardDemo;
