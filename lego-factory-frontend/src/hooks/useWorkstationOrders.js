import { useState, useEffect, useCallback } from 'react';
import api from '../api/api';
import { getWorkstationConfig, calculateOrderStats } from '../config/workstationConfig';

/**
 * useWorkstationOrders - Custom hook for workstation order management
 * 
 * Provides common functionality for manufacturing and assembly dashboards:
 * - Order fetching with auto-refresh
 * - Start/Complete order handlers
 * - Notifications management
 * - Statistics calculation
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

  // State
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [notifications, setNotifications] = useState([]);

  // Notification helpers
  const addNotification = useCallback((message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: stationCode,
    };
    setNotifications(prev => [newNotification, ...prev]);
  }, [stationCode]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  // Fetch orders
  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
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
    }
  }, [apiEndpoint, workstationId, config.name]);

  // Start order handler
  const handleStartOrder = useCallback(async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await api.post(`${apiEndpoint}/${orderId}/start`);
      addNotification(config.startMessage(orderNumber), 'success');
      await fetchOrders();
    } catch (err) {
      const errorMessage = `Failed to start order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('Failed to start order', 'error');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, config, addNotification, fetchOrders]);

  // Complete order handler
  const handleCompleteOrder = useCallback(async (orderId, orderNumber) => {
    if (!globalThis.confirm(config.completeConfirm)) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await api.post(`${apiEndpoint}/${orderId}/complete`);
      addNotification(config.completeMessage(orderNumber), 'success');
      await fetchOrders();
    } catch (err) {
      const errorMessage = `Failed to complete order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('Failed to complete order', 'error');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, config, addNotification, fetchOrders]);

  // Confirm order handler (for Final Assembly 4-step workflow)
  const handleConfirmOrder = useCallback(async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await api.put(`${apiEndpoint}/${orderId}/confirm`);
      addNotification(`Order ${orderNumber} confirmed - ready to start`, 'success');
      await fetchOrders();
    } catch (err) {
      const errorMessage = `Failed to confirm order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('Failed to confirm order', 'error');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, fetchOrders]);

  // Submit order handler (for Final Assembly 4-step workflow)
  const handleSubmitOrder = useCallback(async (orderId, orderNumber) => {
    if (!globalThis.confirm('Submit completed product to Plant Warehouse?')) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await api.post(`${apiEndpoint}/${orderId}/submit`);
      addNotification(`Order ${orderNumber} submitted - Plant Warehouse credited`, 'success');
      await fetchOrders();
    } catch (err) {
      const errorMessage = `Failed to submit order: ${err.response?.data?.message || err.message}`;
      setError(errorMessage);
      addNotification('Failed to submit order', 'error');
    } finally {
      setProcessingOrderId(null);
    }
  }, [apiEndpoint, addNotification, fetchOrders]);

  // Clear error
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Auto-fetch and refresh
  useEffect(() => {
    fetchOrders();
    const interval = setInterval(fetchOrders, refreshInterval);
    return () => clearInterval(interval);
  }, [fetchOrders, refreshInterval]);

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
    fetchOrders,
    clearError,
    addNotification,
    clearNotifications,
    
    // Config (for reference)
    config,
  };
}

export default useWorkstationOrders;
