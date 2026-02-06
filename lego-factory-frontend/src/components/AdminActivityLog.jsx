/**
 * AdminActivityLog - System-Wide Activity Feed
 * 
 * Fetches and displays activity from all workstations and order types.
 * Uses unified ActivityPanel styling for consistency.
 * 
 * Features:
 * - Aggregates activity from all order types
 * - Shows station-colored entries
 * - Auto-refreshes every 30 seconds
 * - Displays all user activities across the system
 */

import { useState, useEffect, useCallback } from "react";
import api from "../api/api";
import { 
  ACTIVITY_TYPES, 
  WORKSTATIONS,
  createActivityEntry 
} from "../utils/activityLogConfig";
import "../styles/ActivityPanel.css";

// Short station names for compact display
const WS_SHORT = {
  1: 'Inj', 2: 'Pre', 3: 'Fin', 4: 'Gear', 5: 'Motor', 6: 'Final', 7: 'Plant', 8: 'Mods', 9: 'Parts'
};

function AdminActivityLog({ maxItems = 25, showStationBadge = true }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Determine activity type based on order status
   */
  const getActivityTypeFromStatus = (status) => {
    const statusLower = (status || '').toLowerCase();
    if (statusLower.includes('completed') || statusLower.includes('fulfilled')) return 'ORDER_COMPLETED';
    if (statusLower.includes('confirmed')) return 'ORDER_CONFIRMED';
    if (statusLower.includes('progress')) return 'ORDER_STARTED';
    if (statusLower.includes('cancelled')) return 'ORDER_CANCELLED';
    if (statusLower.includes('submitted')) return 'ORDER_SUBMITTED';
    if (statusLower.includes('scheduled')) return 'PRODUCTION_SCHEDULED';
    if (statusLower.includes('halted')) return 'ORDER_HALTED';
    return 'ORDER_CREATED';
  };

  /**
   * Get workstation info from ID
   */
  const getWorkstationInfo = (wsId) => {
    if (!wsId) return null;
    const wsKey = `WS-${wsId}`;
    if (WORKSTATIONS[wsKey]) {
      return {
        code: WORKSTATIONS[wsKey].code,
        name: WS_SHORT[wsId] || WORKSTATIONS[wsKey].shortName,
        color: WORKSTATIONS[wsKey].color
      };
    }
    return { code: `WS-${wsId}`, name: `WS-${wsId}`, color: '#94a3b8' };
  };

  /**
   * Generate activity entries from order data
   */
  const generateActivityFromOrders = useCallback((orders, orderPrefix) => {
    return orders.slice(0, 8).map((order, idx) => {
      const activityType = getActivityTypeFromStatus(order.status);
      const config = ACTIVITY_TYPES[activityType] || ACTIVITY_TYPES.ORDER_CREATED;
      
      const orderNum = order.orderNumber || `${orderPrefix}-${order.id}`;
      const wsId = order.workstationId || order.targetWorkstationId;
      const wsInfo = getWorkstationInfo(wsId);
      
      // Build message: "ORD-1234 confirmed"
      const message = `${orderNum} ${config.action}`;
      
      return {
        id: `${orderPrefix}-${order.id}-${idx}`,
        type: activityType.includes('COMPLETED') || activityType.includes('SUBMITTED') ? 'success' :
              activityType.includes('CANCELLED') || activityType.includes('ERROR') ? 'error' :
              activityType.includes('HALTED') ? 'warning' : 'info',
        activityType,
        message,
        icon: config.icon,
        color: config.color,
        station: wsInfo?.code || orderPrefix.toUpperCase(),
        stationName: wsInfo?.name || orderPrefix,
        stationColor: wsInfo?.color || '#64748b',
        timestamp: order.updatedAt || order.createdAt || new Date().toISOString(),
      };
    });
  }, []);

  /**
   * Fetch activity logs from all order sources
   */
  const fetchLogs = useCallback(async () => {
    try {
      const [custRes, whRes, prodRes, supplyRes] = await Promise.allSettled([
        api.get("/customer-orders"),
        api.get("/warehouse-orders"),
        api.get("/production-control-orders"),
        api.get("/supply-orders/warehouse"),
      ]);

      const extract = (res) => res.status === 'fulfilled' ? res.value.data || [] : [];
      
      let activities = [
        ...generateActivityFromOrders(extract(custRes), 'CUST'),
        ...generateActivityFromOrders(extract(whRes), 'WH'),
        ...generateActivityFromOrders(extract(prodRes), 'PROD'),
        ...generateActivityFromOrders(extract(supplyRes), 'SUP'),
      ];

      // Sort by timestamp descending
      activities.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

      setLogs(activities.slice(0, maxItems));
      setLoading(false);
      setError(null);
    } catch (err) {
      console.error("Activity fetch error:", err);
      setError(err.message || 'Failed to load');
      setLoading(false);
    }
  }, [maxItems, generateActivityFromOrders]);

  useEffect(() => {
    fetchLogs();
    const interval = setInterval(fetchLogs, 30000);
    return () => clearInterval(interval);
  }, [fetchLogs]);

  /**
   * Format timestamp as HH:MM:SS
   */
  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    if (isNaN(date.getTime())) return '';
    
    const now = new Date();
    const diff = now - date;
    
    // Show relative time for recent entries
    if (diff < 60000) return 'now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m`;
    
    // Show HH:MM for same day
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString('en-US', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false 
      });
    }
    
    // Show date for older entries
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  /**
   * Get icon from activity type
   */
  const getIcon = (log) => {
    if (log.icon) return log.icon;
    if (log.activityType && ACTIVITY_TYPES[log.activityType]) {
      return ACTIVITY_TYPES[log.activityType].icon;
    }
    return 'ℹ';
  };

  return (
    <div className="activity-panel">
      {/* Header */}
      <div className="activity-panel__header">
        <h4 className="activity-panel__title">
          <span className="activity-panel__icon">📜</span>
          System Activity
          {logs.length > 0 && (
            <span className="activity-panel__count">{logs.length}</span>
          )}
        </h4>
      </div>
      
      {/* Activity List */}
      <div className="activity-panel__list">
        {loading ? (
          <div className="activity-panel__empty">
            <span>Loading...</span>
          </div>
        ) : error ? (
          <div className="activity-panel__empty">
            <span className="activity-panel__empty-icon">⚠️</span>
            <span>{error}</span>
          </div>
        ) : logs.length > 0 ? (
          logs.map((log) => (
            <div 
              key={log.id}
              className={`activity-item activity-item--${log.type || 'info'}`}
              style={{ '--accent-color': log.stationColor || log.color || '#64748b' }}
            >
              <span className="activity-item__icon">
                {getIcon(log)}
              </span>
              <div className="activity-item__content">
                <span className="activity-item__message">
                  {log.message}
                </span>
                {showStationBadge && log.stationName && (
                  <span className="activity-item__station">
                    {log.stationName}
                  </span>
                )}
              </div>
              <span className="activity-item__time">
                {formatTime(log.timestamp)}
              </span>
            </div>
          ))
        ) : (
          <div className="activity-panel__empty">
            <span className="activity-panel__empty-icon">📭</span>
            <span>No recent activity</span>
          </div>
        )}
      </div>
    </div>
  );
}

export default AdminActivityLog;
