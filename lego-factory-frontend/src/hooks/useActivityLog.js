import { useState, useCallback, useEffect } from 'react';
import { 
  formatActivityMessage, 
  extractOrderType, 
  createLoginMessage 
} from '../utils/activityLogConfig';

/**
 * Custom hook for managing activity log notifications
 * 
 * Features:
 * - Automatic login event tracking
 * - Message formatting (removes redundant words)
 * - Order type detection for color coding
 * - Workstation-specific notifications
 * 
 * @param {object} session - Auth session object
 * @param {string} defaultStation - Default workstation identifier
 * @returns {object} - { notifications, addNotification, clearNotifications }
 */
export function useActivityLog(session, defaultStation = 'SYSTEM') {
  const [notifications, setNotifications] = useState([]);
  const [loginLogged, setLoginLogged] = useState(false);

  // Add login event when user first accesses the dashboard
  useEffect(() => {
    if (session?.user && !loginLogged) {
      const loginMessage = createLoginMessage(session.user);
      const station = session.user.workstation?.name || 
                     session.user.workstationName || 
                     defaultStation;
      
      const loginNotification = {
        id: `login-${Date.now()}`,
        message: loginMessage,
        type: 'info',
        timestamp: new Date().toISOString(),
        station: station,
        orderType: null,
        userName: session.user.username
      };
      
      setNotifications([loginNotification]);
      setLoginLogged(true);
    }
  }, [session?.user, loginLogged, defaultStation]);

  /**
   * Add a notification to the activity log
   * @param {string} message - Raw notification message
   * @param {string} type - Notification type (success, error, warning, info)
   * @param {object} options - Additional options (orderNumber, skipFormatting)
   */
  const addNotification = useCallback((message, type = 'info', options = {}) => {
    const {
      orderNumber = null,
      skipFormatting = false,
      station = null
    } = options;

    // Format message unless explicitly skipped
    const formattedMessage = skipFormatting ? message : formatActivityMessage(message);
    
    // Extract order type for color coding
    const orderType = extractOrderType(formattedMessage, orderNumber || '');
    
    // Determine station
    const notificationStation = station || 
                                session?.user?.workstation?.name || 
                                session?.user?.workstationName || 
                                defaultStation;
    
    const newNotification = {
      id: Date.now() + Math.random(),
      message: formattedMessage,
      type,
      timestamp: new Date().toISOString(),
      station: notificationStation,
      orderType,
      orderNumber,
      userName: session?.user?.username
    };
    
    setNotifications(prev => [newNotification, ...prev]);
  }, [session?.user, defaultStation]);

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
    clearNotifications,
    removeNotification
  };
}

export default useActivityLog;
