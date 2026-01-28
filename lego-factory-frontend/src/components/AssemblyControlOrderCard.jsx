import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import BaseOrderCard from './BaseOrderCard';
import api from '../api/api';
import '../styles/CustomerOrderCard.css';

/**
 * AssemblyControlOrderCard Component
 * 
 * Displays an assembly control order with context-aware action buttons.
 * Uses BaseOrderCard for consistent layout with assembly workstations (WS-4, WS-5, WS-6).
 * 
 * Features:
 * - Priority badge display
 * - Item and quantity display
 * - Target and actual timing (TS, TC, AS, AF)
 * - Workstation information
 * - Supply order status checking (auto-fetch on mount)
 * - Status-aware actions (Request Parts, Dispatch, Start, Complete)
 * 
 * Button Logic:
 * - PENDING status:
 *   - If no supply order: Show "Request Parts"
 *   - If supply order PENDING/IN_PROGRESS: Show disabled "Waiting for Parts..."
 *   - If supply order FULFILLED: Show "Dispatch to Workstation"
 * - IN_PROGRESS status: Show "Start Assembly" → "Complete Assembly"
 * 
 * @param {Object} order - Assembly control order object
 * @param {Function} onStart - Handler for starting assembly
 * @param {Function} onComplete - Handler for completing assembly
 * @param {Function} onHalt - Handler for halting assembly
 * @param {Function} onRequestParts - Handler for requesting parts supply
 * @param {Function} onDispatch - Handler for dispatching to workstation
 * @param {Function} onViewDetails - Handler for viewing order details
 */
function AssemblyControlOrderCard({ 
  order, 
  onStart,
  onComplete,
  onHalt,
  onRequestParts,
  onDispatch,
  onViewDetails
}) {
  const [supplyOrders, setSupplyOrders] = useState([]);
  const [loadingSupply, setLoadingSupply] = useState(false);

  // Fetch supply orders when component mounts, order changes, or periodically
  useEffect(() => {
    if (order.id && (order.status === 'PENDING' || order.status === 'ASSIGNED')) {
      fetchSupplyOrders();
      
      // Set up periodic refresh to catch new supply orders
      const interval = setInterval(fetchSupplyOrders, 5000);
      return () => clearInterval(interval);
    }
  }, [order.id, order.status]);

  const fetchSupplyOrders = async () => {
    try {
      setLoadingSupply(true);
      const response = await api.get(`/supply-orders/source/${order.id}?type=ASSEMBLY`);
      setSupplyOrders(response.data || []);
    } catch (error) {
      console.error('Error fetching supply orders:', error);
      setSupplyOrders([]);
    } finally {
      setLoadingSupply(false);
    }
  };

  // Determine supply order status
  const hasSupplyOrder = supplyOrders.length > 0;
  const hasFulfilledSupply = supplyOrders.some(so => so.status === 'FULFILLED');
  const hasActiveSupply = supplyOrders.some(so => so.status === 'PENDING' || so.status === 'IN_PROGRESS');
  
  // Get status CSS class
  const getStatusClass = (status) => {
    const statusMap = {
      'COMPLETED': 'completed',
      'IN_PROGRESS': 'in-progress',
      'HALTED': 'halted',
      'ABANDONED': 'abandoned',
      'ASSIGNED': 'assigned',
      'PENDING': 'pending'
    };
    return statusMap[status] || 'default';
  };

  // Format date/time for display
  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  // Build subtitle with source order reference
  const subtitle = order.sourceAssemblyOrderId ? 
    `Assembly Order #${order.sourceAssemblyOrderId}` : null;

  // Transform item to BaseOrderCard format
  const items = order.itemName ? [{
    name: order.itemName,
    quantity: `${order.quantity} units`
  }] : [];

  // Build info sections for workstation and timing
  const infoSections = [];
  
  // Workstation info
  if (order.workstationName) {
    infoSections.push({
      rows: [{
        label: 'WS',
        value: order.workstationName
      }]
    });
  }
  
  // Schedule information
  infoSections.push({
    rows: [
      { label: 'TS', value: formatDateTime(order.targetStartTime) },
      { label: 'TC', value: formatDateTime(order.targetCompletionTime) }
    ]
  });
  
  // Actual times (if available)
  const actualTimeRows = [];
  if (order.actualStartTime) {
    actualTimeRows.push({ label: 'AS', value: formatDateTime(order.actualStartTime) });
  }
  if (order.actualFinishTime) {
    actualTimeRows.push({ label: 'AF', value: formatDateTime(order.actualFinishTime) });
  }
  if (actualTimeRows.length > 0) {
    infoSections.push({ rows: actualTimeRows });
  }

  // Determine which actions to show based on order status and supply order state
  const getActions = () => {
    const status = order.status;
    const actions = [];
    
    switch(status) {
      case 'PENDING':
        // Check supply order status to determine button
        if (loadingSupply) {
          actions.push({
            label: 'Loading...',
            variant: 'outline',
            size: 'small',
            disabled: true,
            show: true
          });
        } else if (hasFulfilledSupply) {
          actions.push({
            label: '🚀 Dispatch to Workstation',
            variant: 'success',
            size: 'small',
            onClick: () => onDispatch(order.id),
            show: !!onDispatch
          });
          // Show view supply order button
          actions.push({
            label: '📋 View Supply Order',
            variant: 'outline',
            size: 'small',
            onClick: () => onViewDetails({ ...order, supplyOrders }),
            show: !!onViewDetails
          });
        } else if (hasActiveSupply) {
          actions.push({
            label: '⏳ Awaiting Parts...',
            variant: 'warning',
            size: 'small',
            disabled: true,
            show: true
          });
          // Show view supply order button
          actions.push({
            label: '📋 View Supply Order',
            variant: 'outline',
            size: 'small',
            onClick: () => onViewDetails({ ...order, supplyOrders }),
            show: !!onViewDetails
          });
        } else {
          actions.push({
            label: '📦 Request Parts',
            variant: 'primary',
            size: 'small',
            onClick: () => onRequestParts(order),
            show: !!onRequestParts
          });
          actions.push({
            label: 'Details',
            variant: 'ghost',
            size: 'small',
            onClick: () => onViewDetails(order),
            show: !!onViewDetails
          });
        }
        break;

      case 'ASSIGNED':
        actions.push({
          label: '▶️ Start Assembly',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id),
          show: !!onStart
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'IN_PROGRESS':
        actions.push({
          label: '✅ Complete Assembly',
          variant: 'primary',
          size: 'small',
          onClick: () => onComplete(order.id),
          show: !!onComplete
        });
        actions.push({
          label: '⏸️ Halt',
          variant: 'warning',
          size: 'small',
          onClick: () => onHalt(order.id),
          show: !!onHalt
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'HALTED':
        actions.push({
          label: 'Resume',
          variant: 'success',
          size: 'small',
          onClick: () => onStart(order.id),
          show: !!onStart
        });
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      case 'COMPLETED':
      case 'ABANDONED':
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
        break;
      
      default:
        actions.push({
          label: 'Details',
          variant: 'ghost',
          size: 'small',
          onClick: () => onViewDetails(order),
          show: !!onViewDetails
        });
    }
    
    return actions;
  };

  return (
    <BaseOrderCard
      orderNumber={`#${order.controlOrderNumber}`}
      status={order.status}
      statusClass={getStatusClass(order.status)}
      cardType="assembly-control-order-card"
      subtitle={subtitle}
      items={items}
      infoSections={infoSections}
      priority={order.priority}
      notes={order.notes}
      actions={getActions()}
    />
  );
}

AssemblyControlOrderCard.propTypes = {
  order: PropTypes.shape({
    id: PropTypes.number.isRequired,
    controlOrderNumber: PropTypes.string.isRequired,
    sourceAssemblyOrderId: PropTypes.number,
    status: PropTypes.string.isRequired,
    priority: PropTypes.string,
    itemName: PropTypes.string,
    quantity: PropTypes.number,
    workstationName: PropTypes.string,
    targetStartTime: PropTypes.string,
    targetCompletionTime: PropTypes.string,
    actualStartTime: PropTypes.string,
    actualFinishTime: PropTypes.string,
    notes: PropTypes.string
  }).isRequired,
  onStart: PropTypes.func,
  onComplete: PropTypes.func,
  onHalt: PropTypes.func,
  onRequestParts: PropTypes.func,
  onDispatch: PropTypes.func,
  onViewDetails: PropTypes.func
};

AssemblyControlOrderCard.defaultProps = {
  onStart: () => {},
  onComplete: () => {},
  onHalt: () => {},
  onRequestParts: () => {},
  onDispatch: () => {},
  onViewDetails: () => {}
};

export default AssemblyControlOrderCard;
