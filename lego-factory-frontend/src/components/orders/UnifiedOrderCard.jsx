/**
 * UnifiedOrderCard - A single component for all order types
 * 
 * Usage:
 *   <UnifiedOrderCard
 *     orderType={ORDER_TYPES.CUSTOMER_ORDER}
 *     order={orderData}
 *     onAction={(action, orderId, payload) => handleAction(action, orderId, payload)}
 *   />
 */

import React, { useState, useMemo } from 'react';
import styles from './UnifiedOrderCard.module.css';
import {
  ORDER_TYPES,
  ACTION_TYPES,
  getOrderTypeConfig,
  getStatusConfig,
  getPriorityConfig,
  getScenarioConfig,
  formatOrderNumber,
  getReferenceSubtitle,
  getOrderActions,
  getOrderTimestamps,
} from './orderCardConfig';

// ═══════════════════════════════════════════════════════════════
// COMPONENT
// ═══════════════════════════════════════════════════════════════

const UnifiedOrderCard = ({
  orderType,
  order,
  onAction,
  isProcessing = false,
  notification = null,
  compact = false,
  maxItems = 3,
  getItemName = null, // Optional function(itemId, itemType) => name
  getStockLevel = null, // Optional function(itemId, itemType) => stockQuantity
}) => {
  const [expanded, setExpanded] = useState(false);
  
  // Get configurations
  const typeConfig = useMemo(() => getOrderTypeConfig(orderType), [orderType]);
  const statusConfig = useMemo(() => getStatusConfig(order.status), [order.status]);
  const priorityConfig = useMemo(() => getPriorityConfig(order.priority), [order.priority]);
  const scenarioConfig = useMemo(() => getScenarioConfig(order.triggerScenario), [order.triggerScenario]);
  const actions = useMemo(() => getOrderActions(order, orderType), [order, orderType]);
  const timestamps = useMemo(() => getOrderTimestamps(order, orderType), [order, orderType]);
  
  // Get items to display
  const items = useMemo(() => {
    // Handle different item structures
    if (order.items && Array.isArray(order.items)) {
      return order.items;
    }
    if (order.orderItems && Array.isArray(order.orderItems)) {
      return order.orderItems;
    }
    // Single output item (workstation, final assembly)
    if (order.outputName || order.productName || order.partName || order.moduleName) {
      return [{
        name: order.outputName || order.productName || order.partName || order.moduleName,
        quantity: order.outputQuantity || order.quantity || 1,
        fulfilledQuantity: order.completedQuantity || order.fulfilledQuantity,
      }];
    }
    return [];
  }, [order]);
  
  const visibleItems = expanded ? items : items.slice(0, maxItems);
  const hasMoreItems = items.length > maxItems;
  
  // Build CSS classes
  const cardClasses = [
    styles.orderCard,
    styles[typeConfig.cssClass],
    compact && styles.compact,
    isProcessing && styles.processing,
  ].filter(Boolean).join(' ');
  
  // Handle action click
  const handleAction = (action) => {
    if (action.disabled) return;
    onAction?.(action.type, order.id, { order, action });
  };
  
  // Get button variant class
  const getButtonClass = (variant) => {
    const variantMap = {
      primary: styles.btnPrimary,
      secondary: styles.btnSecondary,
      success: styles.btnSuccess,
      warning: styles.btnWarning,
      danger: styles.btnDanger,
    };
    return `${styles.btn} ${variantMap[variant] || styles.btnPrimary}`;
  };
  
  // ═══════════════════════════════════════════════════════════════
  // RENDER
  // ═══════════════════════════════════════════════════════════════
  
  return (
    <div className={cardClasses}>
      {/* HEADER ZONE */}
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <div className={`${styles.orderIcon} ${styles[typeConfig.cssClass]}`}>
            {typeConfig.icon}
          </div>
          <div className={styles.orderInfo}>
            <div className={styles.orderNumber}>
              {formatOrderNumber(order, orderType)}
            </div>
            <div className={styles.orderSubtitle}>
              {getReferenceSubtitle(order, orderType)}
            </div>
          </div>
        </div>
        <div className={styles.headerRight}>
          <div className={styles.headerBadges}>
            {/* Priority badge */}
            {typeConfig.showPriority && priorityConfig && (
              <span className={`${styles.badge} ${styles.badgePriority} ${styles[priorityConfig.cssClass]}`}>
                {priorityConfig.label}
              </span>
            )}
            {/* Scenario badge */}
            {typeConfig.showScenario && scenarioConfig && (
              <span className={`${styles.badge} ${styles.badgeScenario} ${styles[scenarioConfig.cssClass]}`}>
                {scenarioConfig.label}
              </span>
            )}
            {/* Status badge */}
            <span className={`${styles.badge} ${styles.badgeStatus} ${styles[statusConfig.cssClass]}`}>
              {statusConfig.label}
            </span>
          </div>
        </div>
      </div>
      
      {/* WORKSTATION FLOW (Supply Orders) */}
      {typeConfig.showWorkstationFlow && order.fromWorkstation && order.toWorkstation && (
        <div className={styles.workstationFlow}>
          <span className={styles.wsBadge}>WS-{order.fromWorkstation}</span>
          <span className={styles.flowArrow}>→</span>
          <span className={styles.wsBadge}>WS-{order.toWorkstation}</span>
        </div>
      )}
      
      {/* ITEMS ZONE */}
      {typeConfig.showItems && items.length > 0 && (
        <div className={styles.items}>
          {visibleItems.map((item, index) => {
            // Resolve item name using lookup function or fallback to embedded fields
            const resolvedName = getItemName 
              ? getItemName(item.itemId, item.itemType)
              : (item.name || item.itemName || item.productName || item.moduleName || item.partName || `Item ${index + 1}`);
            
            // Resolve quantity (handle various field names)
            const qty = item.quantity ?? item.requestedQuantity ?? 1;
            const fulfilled = item.fulfilledQuantity;
            
            // Calculate stock availability using getStockLevel or fall back to item property
            let stockAvailable = item.stockAvailable;
            let stockQty = null;
            if (getStockLevel && item.itemId) {
              stockQty = getStockLevel(item.itemId, item.itemType);
              // Only update stockAvailable if we have real stock data (not null)
              if (stockQty !== null) {
                stockAvailable = stockQty >= qty ? true : (stockQty > 0 ? 'partial' : false);
              }
            }
            
            return (
              <div key={item.id || index} className={styles.itemRow}>
                <div className={styles.itemLeft}>
                  <span className={styles.itemIcon}>
                    {item.itemType === 'PRODUCT' ? '🚗' : item.itemType === 'MODULE' ? '⚙️' : '🔩'}
                  </span>
                  <span className={styles.itemName}>
                    {resolvedName}
                  </span>
                </div>
                <div className={styles.itemRight}>
                  <span className={styles.itemQty}>
                    {fulfilled != null && fulfilled !== qty ? (
                      <>
                        <span className={fulfilled > 0 ? styles.itemQtyFulfilled : styles.itemQtyPartial}>
                          {fulfilled}
                        </span>
                        /{qty}
                      </>
                    ) : stockQty !== null ? (
                      `${stockQty}/${qty}`
                    ) : (
                      `Qty: ${qty}`
                    )}
                  </span>
                  {stockAvailable !== undefined && (
                    <span className={`${styles.stockIndicator} ${
                      stockAvailable === true ? styles.stockAvailable :
                      stockAvailable === 'partial' ? styles.stockPartial :
                      styles.stockUnavailable
                    }`}>
                      {stockAvailable === true ? '✓' : stockAvailable === 'partial' ? '!' : '✗'}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
          {hasMoreItems && !expanded && (
            <div className={styles.itemsExpand} onClick={() => setExpanded(true)}>
              ▼ Show {items.length - maxItems} more items...
            </div>
          )}
          {hasMoreItems && expanded && (
            <div className={styles.itemsExpand} onClick={() => setExpanded(false)}>
              ▲ Show less
            </div>
          )}
        </div>
      )}
      
      {/* TIMESTAMPS ZONE */}
      {timestamps.length > 0 && (
        <div className={styles.timestamps}>
          {timestamps.map((ts, index) => (
            <div key={index} className={styles.timestampItem}>
              <span className={styles.timestampLabel}>{ts.label}:</span>
              <span className={`${styles.timestampValue} ${ts.isOverdue ? styles.timestampOverdue : ''}`}>
                {ts.value}
              </span>
            </div>
          ))}
        </div>
      )}
      
      {/* NOTES ZONE */}
      {order.notes && (
        <div className={styles.notes}>
          <div className={styles.notesContent}>
            <span className={styles.notesIcon}>📝</span>
            <span className={styles.notesText}>{order.notes}</span>
          </div>
        </div>
      )}
      
      {/* NOTIFICATION ZONE */}
      {notification && (
        <div className={`${styles.notification} ${
          notification.type === 'success' ? styles.notificationSuccess :
          notification.type === 'info' ? styles.notificationInfo :
          notification.type === 'warning' ? styles.notificationWarning :
          styles.notificationError
        }`}>
          <span>{notification.type === 'success' ? '✓' : notification.type === 'info' ? 'ℹ️' : notification.type === 'warning' ? '⚠️' : '✗'}</span>
          <span>{notification.message}</span>
        </div>
      )}
      
      {/* ACTIONS ZONE */}
      {actions.length > 0 && (
        <div className={styles.actions}>
          {actions.map((action, index) => (
            <button
              key={index}
              className={getButtonClass(action.variant)}
              onClick={() => handleAction(action)}
              disabled={action.disabled || isProcessing}
            >
              {action.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

// Export component and types
export { ORDER_TYPES, ACTION_TYPES };
export default UnifiedOrderCard;
