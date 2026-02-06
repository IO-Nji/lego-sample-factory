import { useState, useCallback, useEffect } from 'react';
import { 
  ACTIVITY_TYPES,
  WORKSTATIONS,
  createLoginEntry,
  createOrderEntry,
  createSystemEntry
} from '../utils/activityLogConfig';

/**
 * Custom hook for managing activity log notifications
 * 
 * Features:
 * - Automatic login event tracking with station info
 * - Standardized message format: "ORD-1234 started" / "User logged in"
 * - Activity type detection with icons and colors
 * - Workstation-specific notifications
 * 
 * Message Format Standards:
 * - Order actions: "[orderNumber] [action]" (e.g., "ORD-1234 started")
 * - Login: "User logged in" 
 * - System: Custom message
 * 
 * @param {object} session - Auth session object
 * @param {number} workstationId - Workstation ID for this dashboard
 * @returns {object} - { notifications, addNotification, addOrderNotification, clearNotifications }
 */
export function useActivityLog(session, workstationId = null) {
  const [notifications, setNotifications] = useState([]);
  const [loginLogged, setLoginLogged] = useState(false);
  
  // Get workstation info
  const wsInfo = workstationId ? WORKSTATIONS[workstationId] : null;

  // Add login event when user first accesses the dashboard
  useEffect(() => {
    if (session?.user && !loginLogged) {
      const username = session.user.username || session.user.name || 'User';
      const loginEntry = createLoginEntry(username, wsInfo);
      
      setNotifications([loginEntry]);
      setLoginLogged(true);
    }
  }, [session?.user, loginLogged, wsInfo]);

  /**
   * Add an order-related notification with standardized format
   * @param {string} action - Action verb: 'started', 'completed', 'confirmed', 'submitted', 'halted', 'resumed', 'created', 'fulfilled'
   * @param {string} orderNumber - Order number (e.g., 'ORD-1234')
   * @param {string} type - Notification type: 'success', 'error', 'warning', 'info'
   * @param {string} customMessage - Optional custom message override
   */
  const addOrderNotification = useCallback((action, orderNumber, type = 'success', customMessage = null) => {
    const entry = createOrderEntry(action, orderNumber, type, customMessage, wsInfo);
    setNotifications(prev => [entry, ...prev]);
  }, [wsInfo]);

  /**
   * Add a simple notification (for backward compatibility and system messages)
   * @param {string} message - Notification message
   * @param {string} type - Notification type (success, error, warning, info)
   */
  const addNotification = useCallback((message, type = 'info') => {
    const entry = createSystemEntry(message, type, wsInfo);
    setNotifications(prev => [entry, ...prev]);
  }, [wsInfo]);

  /**
   * Clear all notifications
   */
  const clearNotifications = useCallback(() => {
    setNotifications([]);
    setLoginLogged(false); // Allow login to be logged again if user refreshes
  }, []);

  /**
   * Remove a specific notification by ID
   */
  const removeNotification = useCallback((id) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  return {
    notifications,
    addNotification,
    addOrderNotification,
    clearNotifications,
    removeNotification,
    ACTIVITY_TYPES // Export for component usage
  };
}

export default useActivityLog;
