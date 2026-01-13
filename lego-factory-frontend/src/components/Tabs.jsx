import { useState } from 'react';
import PropTypes from 'prop-types';
import styles from './Tabs.module.css';

/**
 * Tabs Component - Design System Compliant
 * 
 * A tabbed interface component for organizing content into separate views.
 * Supports controlled and uncontrolled modes, with keyboard navigation.
 * 
 * @component
 * @example
 * // Basic tabs
 * <Tabs
 *   tabs={[
 *     { id: 'orders', label: 'Orders', content: <OrdersView /> },
 *     { id: 'products', label: 'Products', content: <ProductsView /> },
 *     { id: 'inventory', label: 'Inventory', content: <InventoryView /> }
 *   ]}
 * />
 * 
 * // Controlled tabs with badge
 * <Tabs
 *   activeTab={activeTab}
 *   onTabChange={setActiveTab}
 *   tabs={[
 *     { id: 'pending', label: 'Pending', badge: 5 },
 *     { id: 'completed', label: 'Completed', badge: 12 }
 *   ]}
 * />
 */
function Tabs({
  tabs = [],
  activeTab: controlledActiveTab,
  onTabChange,
  variant = 'default',
  fullWidth = false,
  className = '',
}) {
  const [uncontrolledActiveTab, setUncontrolledActiveTab] = useState(tabs[0]?.id);
  
  // Use controlled or uncontrolled state
  const activeTab = controlledActiveTab !== undefined ? controlledActiveTab : uncontrolledActiveTab;
  const setActiveTab = onTabChange || setUncontrolledActiveTab;

  const handleTabClick = (tabId) => {
    setActiveTab(tabId);
  };

  const handleKeyDown = (e, tabId, index) => {
    let newIndex = index;
    
    if (e.key === 'ArrowLeft') {
      newIndex = index > 0 ? index - 1 : tabs.length - 1;
    } else if (e.key === 'ArrowRight') {
      newIndex = index < tabs.length - 1 ? index + 1 : 0;
    } else if (e.key === 'Home') {
      newIndex = 0;
    } else if (e.key === 'End') {
      newIndex = tabs.length - 1;
    } else {
      return;
    }
    
    e.preventDefault();
    setActiveTab(tabs[newIndex].id);
  };

  const tabListClasses = [
    styles.tabList,
    styles[variant],
    fullWidth && styles.fullWidth,
    className
  ].filter(Boolean).join(' ');

  const activeTabContent = tabs.find((tab) => tab.id === activeTab)?.content;

  return (
    <div className={styles.container}>
      <div
        className={tabListClasses}
        role="tablist"
        aria-label="Content tabs"
      >
        {tabs.map((tab, index) => {
          const isActive = tab.id === activeTab;
          const tabClasses = [
            styles.tab,
            isActive && styles.active,
            tab.disabled && styles.disabled
          ].filter(Boolean).join(' ');

          return (
            <button
              key={tab.id}
              id={`tab-${tab.id}`}
              className={tabClasses}
              role="tab"
              aria-selected={isActive}
              aria-controls={`panel-${tab.id}`}
              tabIndex={isActive ? 0 : -1}
              onClick={() => !tab.disabled && handleTabClick(tab.id)}
              onKeyDown={(e) => !tab.disabled && handleKeyDown(e, tab.id, index)}
              disabled={tab.disabled}
            >
              {tab.icon && <span className={styles.icon}>{tab.icon}</span>}
              <span>{tab.label}</span>
              {tab.badge !== undefined && (
                <span className={styles.badge}>{tab.badge}</span>
              )}
            </button>
          );
        })}
      </div>
      
      <div
        id={`panel-${activeTab}`}
        className={styles.panel}
        role="tabpanel"
        aria-labelledby={`tab-${activeTab}`}
      >
        {activeTabContent}
      </div>
    </div>
  );
}

Tabs.propTypes = {
  /** Array of tab definitions */
  tabs: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      content: PropTypes.node,
      icon: PropTypes.node,
      badge: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      disabled: PropTypes.bool,
    })
  ).isRequired,
  
  /** Active tab ID (controlled mode) */
  activeTab: PropTypes.string,
  
  /** Tab change handler (controlled mode) */
  onTabChange: PropTypes.func,
  
  /** Visual style variant */
  variant: PropTypes.oneOf(['default', 'pills', 'underline']),
  
  /** Make tabs full width */
  fullWidth: PropTypes.bool,
  
  /** Additional CSS classes */
  className: PropTypes.string,
};

export default Tabs;
