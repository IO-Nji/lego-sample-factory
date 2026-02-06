import { useState, useEffect, useCallback } from 'react';
import api from '../api/api';
import { getWorkstationConfig, calculateOrderStats } from '../config/workstationConfig';
import { 
  ACTIVITY_TYPES, 
  WORKSTATIONS,
  createOrderEntry 
} from '../utils/activityLogConfig';

/**
 * useWorkstationOrders - Custom hook for workstation order management
 * 
 * Provides common functionality for manufacturing and assembly dashboards:
 * - Order fetching with auto-refresh
 * - Start/Complete order handlers
 * - Notifications management with consistent message formatting
 * - Statistics calculation
 * 
 * Message Format: "[ORDER_NUMBER] [action]" e.g., "ORD-1234 started"
 * 
 * @param {number} workstationId - The workstation ID (1-9)
 * @param {Object} options - Optional overrides
 * @returns {Object} Dashboard state and handlers
 */
export function useWorkstationOrders(workstationId, options = {}) {
  const config = getWorkstationConfig(workstationId);
  
  if (!config) {
    throw new Error(`Invalid workstationId: ${workstationId}`);
  }

  const {
    apiEndpoint = config.apiEndpoint,
    refreshInterval = 30000,
    stationCode = config.stationCode,
  } = options;

  // Get workstation info for activity logging
  const wsKey = `WS-${workstationId}`;
  const wsInfo = WORKSTATIONS[wsKey] || { code: stationCode, name: config.name, color: '#64748b' };

  // State
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true); // Only true on initial load
  const [error, setError] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [isInitialLoad, setIsInitialLoad] = useState(true); // Track first load

  /**
   * Add a notification using the standardized activity format
   * @param {string} action - Action verb (started, completed, confirmed, etc.)
   * @param {string} orderNumber - Order number
   * @param {string} type - Notification type (success, error, warning, info)
   * @param {string} customMessage - Override the auto-generated message
   */
  const addNotification = useCallback((action, orderNumber, type = 'info', customMessage = null) => {
    // Map action to activity type
    const activityTypeMap = {
      'started': 'ORDER_STARTED',
      'completed': 'ORDER_COMPLETED',
      'confirmed': 'ORDER_CONFIRMED',
      'submitted': 'ORDER_SUBMITTED',
      'halted': 'ORDER_HALTED',
      'resumed': 'ORDER_RESUMED',
      'created': 'ORDER_CREATED',
      'error': 'SYSTEM_ERROR',
    };
    
    const activityType = activityTypeMap[action.toLowerCase()] || 'SYSTEM_INFO';
    const actConfig = ACTIVITY_TYPES[activityType] || ACTIVITY_TYPES.SYSTEM_INFO;
    
    // Build message: "ORD-1234 started" or custom message
    const message = customMessage || `${orderNumber} ${action}`;
    
    const newNotification = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      type,
      activityType,
      message,
      icon: actConfig.icon,
      color: actConfig.color,
      station: wsInfo.code,
      stationName: wsInfo.name,
      stationColor: wsInfo.color,
      timestamp: new Date().toISOString(),
    };
    
    setNotifications(prev => [newNotification, ...prev]);
  }, [wsInfo]);

  /**
   * Add a simple text notification (for backward compatibility)
   */
  const addSimpleNotification = useCallback((message, type = 'info') => {
    const newNotification = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      type,
      message,
      station: wsInfo.code,
      stationName: wsInfo.name,
      stationColor: wsInfo.color,
      timestamp: new Date().toISOString(),
    };
    setNotifications(prev => [newNotification, ...prev]);
  }, [wsInfo]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  // Fetch orders - only shows loading state on initial load
  const fetchOrders = useCallback(async (showLoading = false) => {
    try {
      // Only show loading spinner on initial load, not on refreshes
      if (isInitialLoad || showLoading) {
        setLoading(true);
      }
      const response = await api.get(`${apiEndpoint}/workstation/${workstationId}`);
      const data = response.data;
      if (Array.isArray(data)) {
        setOrders(data);
        setError(null);
      } else {
        setOrders([]);
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setOrders([]);
        setError(null);
      } else {
        const errorMessage = `Failed to load ${config.name.toLowerCase()} orders: ${err.response?.data?.message || err.message}`;
        setError(errorMessage);
      }
    } finally {
      setLoading(false);
      setIsInitialLoad(false);
    }
  }, [apiEndpoint, workstationId, config.name, isInitialLoad]);

  // Refresh orders without showing loading state (for background updates)
  const refreshOrders = useCallback(async () => {
    try {
      const response = await api.get(`${apiEndpoint}/workstation/${workstationId}`);
      const data = response.data;
      if (Array.isArray(data)) {
        setOrders(data);
        setError(null);
      } else {
        setOrders([]);
      }
    } catch (err) {
      if (err.response?.status !== 404) {
        console.error('Background refresh failed:', err);
      }
    }
  }, [apiEndpoint, workstationId]);

  // Start order handler
  const handleStartOrder = useCallback(async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list if not provided
    const orderNum = orderNumber || orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.post(`${apiEndpoint}/${orderId}/start`);
      addNotification('started', orderNum, 'success');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to start order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('error', orderNum, 'error', 'Failed to start order');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, refreshOrders, orders]);

  // Complete order handler
  const handleCompleteOrder = useCallback(async (orderId, orderNumber) => {
    if (!globalThis.confirm(config.completeConfirm)) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list if not provided
    const orderNum = orderNumber || orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.post(`${apiEndpoint}/${orderId}/complete`);
      addNotification('completed', orderNum, 'success');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to complete order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('error', orderNum, 'error', 'Failed to complete order');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, config, addNotification, refreshOrders, orders]);

  // Confirm order handler (for Final Assembly 4-step workflow)
  const handleConfirmOrder = useCallback(async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list if not provided
    const orderNum = orderNumber || orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.put(`${apiEndpoint}/${orderId}/confirm`);
      addNotification('confirmed', orderNum, 'success');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to confirm order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('error', orderNum, 'error', 'Failed to confirm order');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, refreshOrders, orders]);

  // Submit order handler (for Final Assembly 4-step workflow)
  const handleSubmitOrder = useCallback(async (orderId, orderNumber) => {
    if (!globalThis.confirm('Submit completed product to Plant Warehouse?')) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list if not provided
    const orderNum = orderNumber || orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.post(`${apiEndpoint}/${orderId}/submit`);
      addNotification('submitted', orderNum, 'success');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to submit order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('error', orderNum, 'error', 'Failed to submit order');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, refreshOrders, orders]);

  // Halt order handler - pause work in progress
  const handleHaltOrder = useCallback(async (orderId, reason = 'Operator initiated halt') => {
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list
    const orderNum = orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.post(`${apiEndpoint}/${orderId}/halt`, { reason });
      addNotification('halted', orderNum, 'warning');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to halt order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addSimpleNotification('Failed to halt order', 'error');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, addSimpleNotification, refreshOrders, orders]);

  // Resume order handler - continue halted work
  const handleResumeOrder = useCallback(async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    // Get order number from orders list if not provided
    const orderNum = orderNumber || orders.find(o => o.id === orderId)?.orderNumber || `ORD-${orderId}`;

    try {
      await api.post(`${apiEndpoint}/${orderId}/resume`);
      addNotification('resumed', orderNum, 'success');
      await refreshOrders(); // Use silent refresh
    } catch (err) {
      const errorMessage = `Failed to resume order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('error', orderNum, 'error', 'Failed to resume order');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, refreshOrders, orders]);

  // Clear error
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Initial fetch on mount
  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Auto-refresh using silent refresh (no loading state)
  useEffect(() => {
    const interval = setInterval(refreshOrders, refreshInterval);
    return () => clearInterval(interval);
  }, [refreshOrders, refreshInterval]);

  // Calculate stats
  const statsData = calculateOrderStats(orders);

  return {
    // State
    orders,
    loading,
    error,
    processingOrderId,
    notifications,
    statsData,
    
    // Handlers
    handleConfirmOrder,
    handleStartOrder,
    handleCompleteOrder,
    handleSubmitOrder,
    handleHaltOrder,
    handleResumeOrder,
    fetchOrders,
    refreshOrders, // Add silent refresh
    clearError,
    addNotification,
    clearNotifications,
    
    // Config (for reference)
    config,
  };
}

export default useWorkstationOrders;
