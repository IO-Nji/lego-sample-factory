/**
 * AdminDashboard - Compact Operational Overview
 * 
 * Dense, information-rich dashboard for monitoring current operations.
 * Focus on: Orders, Production, Inventory Alerts, Workstation Status
 */

import { useState, useEffect, useCallback } from "react";
import api from "../../api/api";
import { CompactScheduleTimeline, Footer } from "../../components";
import DashboardHeader from "../../components/dashboard/DashboardHeader";
import AdminActivityLog from "../../components/AdminActivityLog";
import { getWorkstationIcon, WORKSTATION_CONFIG } from "../../config/workstationConfig";
import "../../styles/DashboardHeader.css";
import "./AdminDashboard.css";

// Workstation colors
const WS_COLORS = {
  1: { bg: '#dbeafe', accent: '#3b82f6' },
  2: { bg: '#ede9fe', accent: '#8b5cf6' },
  3: { bg: '#cffafe', accent: '#06b6d4' },
  4: { bg: '#fef3c7', accent: '#f59e0b' },
  5: { bg: '#fee2e2', accent: '#ef4444' },
  6: { bg: '#d1fae5', accent: '#10b981' },
  7: { bg: '#dbeafe', accent: '#2c5aa0' },
  8: { bg: '#fce7f3', accent: '#ec4899' },
  9: { bg: '#ecfccb', accent: '#84cc16' },
};

// Workstation type categories for metrics selection
const WS_TYPES = {
  MANUFACTURING: [1, 2, 3],  // Injection Molding, Parts Pre-Prod, Part Finishing
  ASSEMBLY: [4, 5, 6],       // Gear, Motor, Final Assembly
  WAREHOUSE: [7, 8, 9],      // Plant WH, Modules, Parts Supply
};

function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [scheduledTasks, setScheduledTasks] = useState([]);
  const [data, setData] = useState({
    orders: { total: 0, pending: 0, processing: 0, completed: 0, cancelled: 0 },
    production: { controlOrders: 0, assemblyOrders: 0, supplyOrders: 0, activeJobs: 0 },
    inventory: { lowStock: 0, criticalStock: 0, alerts: [] },
    system: { users: 0, products: 0, modules: 0 },
    workstations: [],
    workstationOrders: {}, // { wsId: { pending, active, completed, total } }
    workstationStock: {},  // { wsId: { total, low, critical } }
  });

  const fetchData = useCallback(async () => {
    try {
      setRefreshing(true);
      setError(null);

      // Core data fetches
      const [
        workstationsRes, usersRes, productsRes, customerOrdersRes,
        productionControlRes, assemblyControlRes, supplyOrdersRes,
        lowStockRes, scheduledRes, warehouseOrdersRes,
        // Workstation-specific orders
        injectionOrdersRes, partPrepOrdersRes, partFinishOrdersRes,
        gearOrdersRes, motorOrdersRes, finalOrdersRes,
        // Stock data
        stockAllRes
      ] = await Promise.allSettled([
        api.get("/masterdata/workstations"),
        api.get("/users"),
        api.get("/masterdata/products"),
        api.get("/customer-orders"),
        api.get("/production-control-orders"),
        api.get("/assembly-control-orders"),
        api.get("/supply-orders/warehouse"),
        api.get("/stock/alerts/low"),
        api.get("/simal/scheduled-orders"),
        api.get("/warehouse-orders"),
        // Workstation order endpoints
        api.get("/injection-molding-orders"),
        api.get("/part-preproduction-orders"),
        api.get("/part-finishing-orders"),
        api.get("/gear-assembly-orders"),
        api.get("/motor-assembly-orders"),
        api.get("/final-assembly-orders"),
        // All stock
        api.get("/stock"),
      ]);

      const extract = (res) => res.status === 'fulfilled' ? res.value.data || [] : [];
      
      const workstations = extract(workstationsRes);
      const users = extract(usersRes);
      const products = extract(productsRes);
      const customerOrders = extract(customerOrdersRes);
      const productionControl = extract(productionControlRes);
      const assemblyControl = extract(assemblyControlRes);
      const supplyOrders = extract(supplyOrdersRes);
      const lowStock = extract(lowStockRes);
      const scheduled = extract(scheduledRes);
      const warehouseOrders = extract(warehouseOrdersRes);
      
      // Workstation orders
      const wsOrders = {
        1: extract(injectionOrdersRes),
        2: extract(partPrepOrdersRes),
        3: extract(partFinishOrdersRes),
        4: extract(gearOrdersRes),
        5: extract(motorOrdersRes),
        6: extract(finalOrdersRes),
        7: customerOrders,
        8: warehouseOrders,
        9: supplyOrders,
      };
      
      const allStock = extract(stockAllRes);

      // Helper to count orders by status
      const countOrderStatus = (orders) => {
        return orders.reduce((acc, o) => {
          const s = (o.status || '').toUpperCase();
          acc.total++;
          if (s.includes('PENDING') || s.includes('CREATED') || s.includes('CONFIRMED')) acc.pending++;
          else if (s.includes('PROGRESS') || s.includes('PROCESSING')) acc.active++;
          else if (s.includes('COMPLETED') || s.includes('FULFILLED') || s.includes('SUBMITTED')) acc.completed++;
          return acc;
        }, { pending: 0, active: 0, completed: 0, total: 0 });
      };

      // Process workstation orders
      const workstationOrders = {};
      Object.entries(wsOrders).forEach(([wsId, orders]) => {
        workstationOrders[wsId] = countOrderStatus(Array.isArray(orders) ? orders : []);
      });

      // Process stock by workstation
      const workstationStock = {};
      [7, 8, 9].forEach(wsId => {
        const wsStock = allStock.filter(s => s.workstationId === wsId);
        const lowCount = wsStock.filter(s => s.quantity > 2 && s.quantity <= 10).length;
        const criticalCount = wsStock.filter(s => s.quantity <= 2).length;
        workstationStock[wsId] = {
          total: wsStock.length,
          items: wsStock.reduce((sum, s) => sum + (s.quantity || 0), 0),
          low: lowCount,
          critical: criticalCount,
        };
      });

      // Order counts (global)
      const allOrders = [...customerOrders, ...warehouseOrders];
      const countByStatus = (orders) => orders.reduce((acc, o) => {
        const s = (o.status || '').toUpperCase();
        if (s.includes('PENDING') || s.includes('CREATED')) acc.pending++;
        else if (s.includes('PROGRESS') || s.includes('PROCESSING')) acc.processing++;
        else if (s.includes('COMPLETED') || s.includes('FULFILLED')) acc.completed++;
        else if (s.includes('CANCELLED')) acc.cancelled++;
        return acc;
      }, { pending: 0, processing: 0, completed: 0, cancelled: 0 });
      
      const orderCounts = countByStatus(allOrders);

      // Production counts
      const activeProduction = productionControl.filter(o => 
        (o.status || '').toUpperCase().includes('PROGRESS')
      ).length;
      const activeAssembly = assemblyControl.filter(o => 
        (o.status || '').toUpperCase().includes('PROGRESS')
      ).length;

      // Process workstations with real data
      const processedWS = workstations.map(ws => {
        const wsOrderData = workstationOrders[ws.id] || { pending: 0, active: 0, completed: 0, total: 0 };
        const wsStockData = workstationStock[ws.id] || null;
        const hasActivity = wsOrderData.active > 0 || wsOrderData.pending > 0;
        
        return {
          id: ws.id,
          name: ws.name,
          code: `WS-${ws.id}`,
          icon: getWorkstationIcon(ws.id),
          status: hasActivity ? 'active' : 'idle',
          colors: WS_COLORS[ws.id] || { bg: '#f3f4f6', accent: '#6b7280' },
          type: WORKSTATION_CONFIG[ws.id]?.type || 'UNKNOWN',
          orders: wsOrderData,
          stock: wsStockData,
        };
      });

      // Inventory alerts
      const criticalItems = lowStock.filter(item => (item.quantity || 0) <= 2);
      const alerts = [];
      if (criticalItems.length > 0) alerts.push({ type: 'critical', msg: `${criticalItems.length} critical stock items` });
      if (lowStock.length > criticalItems.length) alerts.push({ type: 'warning', msg: `${lowStock.length - criticalItems.length} low stock warnings` });
      if (orderCounts.pending > 10) alerts.push({ type: 'info', msg: `${orderCounts.pending} orders awaiting action` });

      setData({
        orders: { 
          total: allOrders.length, 
          ...orderCounts,
          customer: customerOrders.length,
          warehouse: warehouseOrders.length,
        },
        production: {
          controlOrders: productionControl.length,
          assemblyOrders: assemblyControl.length,
          supplyOrders: supplyOrders.length,
          activeJobs: activeProduction + activeAssembly,
        },
        inventory: {
          lowStock: lowStock.length,
          criticalStock: criticalItems.length,
          alerts,
        },
        system: {
          users: users.length,
          products: products.length,
        },
        workstations: processedWS,
        workstationOrders,
        workstationStock,
      });

      // Extract tasks from scheduled orders (same format as ProductionPlanningDashboard)
      // scheduled is an array of orders, each with a scheduledTasks array
      const allTasks = [];
      scheduled.forEach(order => {
        if (order.scheduledTasks && Array.isArray(order.scheduledTasks)) {
          order.scheduledTasks.forEach(task => {
            allTasks.push({
              ...task,
              id: task.taskId || task.id,
              workstationName: task.workstationName,
              startTime: task.startTime,
              endTime: task.endTime,
              status: task.status || 'SCHEDULED',
              orderId: order.id,
              scheduleId: order.scheduleId
            });
          });
        }
      });
      setScheduledTasks(allTasks);

      setLoading(false);
    } catch (err) {
      console.error("Dashboard error:", err);
      setError("Failed to load data");
      setLoading(false);
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 60000);
    return () => clearInterval(interval);
  }, [fetchData]);

  const completionRate = data.orders.total > 0 
    ? Math.round((data.orders.completed / data.orders.total) * 100) : 0;
  const activeWS = data.workstations.filter(ws => ws.status === 'active').length;

  if (loading) {
    return (
      <div className="admin-page">
        <div className="admin-loading">
          <div className="admin-spinner" />
          <p>Loading Dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="admin-bg" />
      
      {/* Header - Using Unified DashboardHeader Component */}
      <DashboardHeader
        icon="🏭"
        title="Admin Dashboard"
        subtitle="Operations Overview"
        onRefresh={fetchData}
        themeClass="admin-theme"
      />

      {error && (
        <div className="admin-error">
          ⚠️ {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {/* Main Grid - Compact Stats */}
      <div className="admin-grid">
        
        {/* Orders Overview Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📦</span>
            <h2 className="admin-panel__title">Orders</h2>
            <span className="admin-panel__badge">{data.orders.total}</span>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat admin-stat--pending">
              <span className="admin-stat__val">{data.orders.pending}</span>
              <span className="admin-stat__lbl">Pending</span>
            </div>
            <div className="admin-stat admin-stat--processing">
              <span className="admin-stat__val">{data.orders.processing}</span>
              <span className="admin-stat__lbl">Processing</span>
            </div>
            <div className="admin-stat admin-stat--completed">
              <span className="admin-stat__val">{data.orders.completed}</span>
              <span className="admin-stat__lbl">Completed</span>
            </div>
            <div className="admin-stat admin-stat--rate">
              <span className="admin-stat__val">{completionRate}%</span>
              <span className="admin-stat__lbl">Rate</span>
            </div>
          </div>
          <div className="admin-sub-stats">
            <span>📋 Customer: {data.orders.customer}</span>
            <span>📦 Warehouse: {data.orders.warehouse}</span>
          </div>
        </div>

        {/* Production Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">⚙️</span>
            <h2 className="admin-panel__title">Production</h2>
            <span className="admin-panel__badge admin-panel__badge--active">{data.production.activeJobs} active</span>
          </div>
          <div className="admin-stats-row">
            <div className="admin-stat">
              <span className="admin-stat__val">{data.production.controlOrders}</span>
              <span className="admin-stat__lbl">Control</span>
            </div>
            <div className="admin-stat">
              <span className="admin-stat__val">{data.production.assemblyOrders}</span>
              <span className="admin-stat__lbl">Assembly</span>
            </div>
            <div className="admin-stat">
              <span className="admin-stat__val">{data.production.supplyOrders}</span>
              <span className="admin-stat__lbl">Supply</span>
            </div>
            <div className="admin-stat admin-stat--highlight">
              <span className="admin-stat__val">{activeWS}/{data.workstations.length}</span>
              <span className="admin-stat__lbl">WS Active</span>
            </div>
          </div>
        </div>

        {/* Inventory & Alerts Panel */}
        <div className="admin-panel glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📊</span>
            <h2 className="admin-panel__title">Inventory & Alerts</h2>
          </div>
          <div className="admin-stats-row">
            <div className={`admin-stat ${data.inventory.criticalStock > 0 ? 'admin-stat--danger' : ''}`}>
              <span className="admin-stat__val">{data.inventory.criticalStock}</span>
              <span className="admin-stat__lbl">Critical</span>
            </div>
            <div className={`admin-stat ${data.inventory.lowStock > 0 ? 'admin-stat--warning' : ''}`}>
              <span className="admin-stat__val">{data.inventory.lowStock}</span>
              <span className="admin-stat__lbl">Low Stock</span>
            </div>
            <div className="admin-stat">
              <span className="admin-stat__val">{data.system.products}</span>
              <span className="admin-stat__lbl">Products</span>
            </div>
            <div className="admin-stat">
              <span className="admin-stat__val">{data.system.users}</span>
              <span className="admin-stat__lbl">Users</span>
            </div>
          </div>
          {data.inventory.alerts.length > 0 && (
            <div className="admin-alerts-mini">
              {data.inventory.alerts.map((a, i) => (
                <div key={i} className={`admin-alert-mini admin-alert-mini--${a.type}`}>
                  {a.type === 'critical' ? '🔴' : a.type === 'warning' ? '🟠' : '🔵'} {a.msg}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Actions */}
        <div className="admin-panel glass admin-panel--actions">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">⚡</span>
            <h2 className="admin-panel__title">Quick Actions</h2>
          </div>
          <div className="admin-actions">
            <button className="admin-action admin-action--primary">📋 New Order</button>
            <button className="admin-action">📊 Reports</button>
            <button className="admin-action">⚙️ Settings</button>
            <button className="admin-action">📤 Export</button>
          </div>
        </div>
      </div>

      {/* Timeline + Activity Row */}
      <div className="admin-timeline-row">
        <div className="admin-timeline-section glass">
          <div className="admin-panel__head">
            <span className="admin-panel__icon">📅</span>
            <h2 className="admin-panel__title">Production Schedule</h2>
          </div>
          <div className="admin-timeline">
            <CompactScheduleTimeline
              scheduledTasks={scheduledTasks}
              showTitle={false}
              showCurrentTime={true}
              showMiniControls={true}
              onTaskClick={(task) => logger.debug('Admin', 'Task clicked', task)}
            />
          </div>
        </div>
        
        <div className="admin-activity-section glass">
          <AdminActivityLog maxItems={20} />
        </div>
      </div>

      {/* Bottom Row - Workstations + Analytics */}
      <div className="admin-bottom-row">
        {/* Workstation Grid - Left Column */}
        <div className="admin-ws-column glass">
          <div className="admin-ws-header">
            <h2 className="admin-ws-title">🏭 Workstations</h2>
            <span className="admin-ws-badge">{activeWS}/{data.workstations.length} Active</span>
          </div>
          <div className="admin-ws-grid">
            {data.workstations.map((ws) => {
              // Calculate completion rate for donut
              const total = ws.orders?.total || 0;
              const completed = ws.orders?.completed || 0;
              const pending = ws.orders?.pending || 0;
              const active = ws.orders?.active || 0;
              const completionPct = total > 0 ? Math.round((completed / total) * 100) : 0;
              
              // Determine workstation category
              const isWarehouse = WS_TYPES.WAREHOUSE.includes(ws.id);
              const isManufacturing = WS_TYPES.MANUFACTURING.includes(ws.id);
              const isAssembly = WS_TYPES.ASSEMBLY.includes(ws.id);
              
              // Stock data for warehouses
              const stock = ws.stock || { total: 0, items: 0, low: 0, critical: 0 };
              const hasStockAlert = stock.critical > 0 || stock.low > 0;
              
              return (
                <div 
                  key={ws.id} 
                  className={`admin-ws admin-ws--${ws.status} admin-ws--enhanced`}
                  style={{ '--ws-bg': ws.colors.bg, '--ws-accent': ws.colors.accent }}
                >
                  {/* Left Side - Identity & Stats */}
                  <div className="admin-ws__main">
                    <div className="admin-ws__top">
                      <span className="admin-ws__icon">{ws.icon}</span>
                      <span className="admin-ws__code">{ws.code}</span>
                      <span className={`admin-ws__dot admin-ws__dot--${ws.status}`} />
                    </div>
                    <div className="admin-ws__name">{ws.name}</div>
                    
                    {/* Compact Inline Stats */}
                    <div className="admin-ws__inline-stats">
                      {pending > 0 && (
                        <span className="admin-ws__chip admin-ws__chip--pending">
                          <span className="admin-ws__chip-icon">⏳</span>
                          {pending}
                        </span>
                      )}
                      {active > 0 && (
                        <span className="admin-ws__chip admin-ws__chip--active">
                          <span className="admin-ws__chip-icon">▶️</span>
                          {active}
                        </span>
                      )}
                      {isWarehouse && hasStockAlert && (
                        <span className={`admin-ws__chip ${stock.critical > 0 ? 'admin-ws__chip--critical' : 'admin-ws__chip--warning'}`}>
                          <span className="admin-ws__chip-icon">{stock.critical > 0 ? '🔴' : '🟠'}</span>
                          {stock.critical > 0 ? stock.critical : stock.low}
                        </span>
                      )}
                      {!pending && !active && !hasStockAlert && (
                        <span className="admin-ws__chip admin-ws__chip--idle">
                          <span className="admin-ws__chip-icon">✓</span>
                          Idle
                        </span>
                      )}
                    </div>
                  </div>
                  
                  {/* Right Side - Mini Donut Visualization */}
                  <div className="admin-ws__visual">
                    {/* For Manufacturing/Assembly: Order completion donut */}
                    {(isManufacturing || isAssembly) && (
                      <div className="admin-ws__mini-donut" title={`${completionPct}% complete (${completed}/${total})`}>
                        <svg viewBox="0 0 32 32" className="admin-ws__donut-svg">
                          <circle cx="16" cy="16" r="12" fill="none" stroke="rgba(0,0,0,0.08)" strokeWidth="3" />
                          {total > 0 && (
                            <circle 
                              cx="16" cy="16" r="12" fill="none" 
                              stroke={ws.colors.accent}
                              strokeWidth="3"
                              strokeDasharray={`${(completionPct / 100) * 75.4} ${75.4 - (completionPct / 100) * 75.4}`}
                              strokeDashoffset="18.85"
                              strokeLinecap="round"
                              style={{ transition: 'stroke-dasharray 0.5s ease' }}
                            />
                          )}
                        </svg>
                        <span className="admin-ws__donut-value">{total > 0 ? `${completionPct}%` : '—'}</span>
                        <span className="admin-ws__donut-label">Orders</span>
                      </div>
                    )}
                    
                    {/* For Warehouses: Stock level indicator */}
                    {isWarehouse && (
                      <div className="admin-ws__stock-gauge" title={`${stock.items} items in stock`}>
                        <div className="admin-ws__gauge-container">
                          <div className="admin-ws__gauge-bar">
                            <div 
                              className={`admin-ws__gauge-fill ${stock.critical > 0 ? 'admin-ws__gauge-fill--critical' : stock.low > 0 ? 'admin-ws__gauge-fill--warning' : 'admin-ws__gauge-fill--good'}`}
                              style={{ height: `${Math.min(100, Math.max(10, (stock.total > 0 ? 100 : 0)))}%` }}
                            />
                          </div>
                          <span className="admin-ws__gauge-value">{stock.items}</span>
                          <span className="admin-ws__gauge-label">Stock</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Analytics Column - Right */}
        <div className="admin-analytics-column">
          {/* Order Fulfillment Donut */}
          <div className="admin-chart-card glass">
            <div className="admin-chart-header">
              <span className="admin-chart-icon">📊</span>
              <h3 className="admin-chart-title">Order Fulfillment</h3>
            </div>
            <div className="admin-donut">
              <svg viewBox="0 0 36 36" className="admin-donut__svg">
                <circle cx="18" cy="18" r="15.9" fill="none" stroke="#e2e8f0" strokeWidth="3" />
                <circle 
                  cx="18" cy="18" r="15.9" fill="none" 
                  stroke="#10b981" strokeWidth="3"
                  strokeDasharray={`${completionRate} ${100 - completionRate}`}
                  strokeDashoffset="25"
                  strokeLinecap="round"
                />
              </svg>
              <div className="admin-donut__center">
                <span className="admin-donut__value">{completionRate}%</span>
                <span className="admin-donut__label">Complete</span>
              </div>
            </div>
            <div className="admin-donut-legend">
              <span className="admin-donut-legend__item">
                <span className="admin-donut-legend__dot" style={{ background: '#10b981' }} />
                Fulfilled ({data.orders.completed})
              </span>
              <span className="admin-donut-legend__item">
                <span className="admin-donut-legend__dot" style={{ background: '#e2e8f0' }} />
                Pending ({data.orders.pending + data.orders.processing})
              </span>
            </div>
          </div>

          {/* Threshold Alerts */}
          <div className="admin-alerts-card glass">
            <div className="admin-chart-header">
              <span className="admin-chart-icon">⚠️</span>
              <h3 className="admin-chart-title">Threshold Alerts</h3>
            </div>
            <div className="admin-threshold-list">
              {data.inventory.criticalStock > 0 && (
                <div className="admin-threshold admin-threshold--critical">
                  <span className="admin-threshold__icon">🔴</span>
                  <span className="admin-threshold__text">Critical Stock</span>
                  <span className="admin-threshold__value">{data.inventory.criticalStock}</span>
                </div>
              )}
              {data.inventory.lowStock > 0 && (
                <div className="admin-threshold admin-threshold--warning">
                  <span className="admin-threshold__icon">🟠</span>
                  <span className="admin-threshold__text">Low Stock</span>
                  <span className="admin-threshold__value">{data.inventory.lowStock}</span>
                </div>
              )}
              {data.orders.pending > 5 && (
                <div className="admin-threshold admin-threshold--info">
                  <span className="admin-threshold__icon">🔵</span>
                  <span className="admin-threshold__text">Pending Orders</span>
                  <span className="admin-threshold__value">{data.orders.pending}</span>
                </div>
              )}
              {data.production.activeJobs > 3 && (
                <div className="admin-threshold admin-threshold--active">
                  <span className="admin-threshold__icon">🟢</span>
                  <span className="admin-threshold__text">Active Jobs</span>
                  <span className="admin-threshold__value">{data.production.activeJobs}</span>
                </div>
              )}
              {data.inventory.criticalStock === 0 && data.inventory.lowStock === 0 && data.orders.pending <= 5 && data.production.activeJobs <= 3 && (
                <div className="admin-threshold admin-threshold--ok">
                  <span className="admin-threshold__icon">✅</span>
                  <span className="admin-threshold__text">All systems nominal</span>
                </div>
              )}
            </div>
          </div>

          {/* Sparkline Trends */}
          <div className="admin-trends-card glass">
            <div className="admin-chart-header">
              <span className="admin-chart-icon">📈</span>
              <h3 className="admin-chart-title">Today's Trends</h3>
            </div>
            <div className="admin-sparklines">
              <div className="admin-sparkline">
                <div className="admin-sparkline__info">
                  <span className="admin-sparkline__label">Orders</span>
                  <span className="admin-sparkline__value">{data.orders.total}</span>
                </div>
                <svg className="admin-sparkline__svg" viewBox="0 0 60 20">
                  <polyline 
                    fill="none" 
                    stroke="#3b82f6" 
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points="2,15 10,12 18,14 26,8 34,10 42,6 50,9 58,4"
                  />
                </svg>
              </div>
              <div className="admin-sparkline">
                <div className="admin-sparkline__info">
                  <span className="admin-sparkline__label">Production</span>
                  <span className="admin-sparkline__value">{data.production.controlOrders + data.production.assemblyOrders}</span>
                </div>
                <svg className="admin-sparkline__svg" viewBox="0 0 60 20">
                  <polyline 
                    fill="none" 
                    stroke="#10b981" 
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points="2,10 10,8 18,12 26,6 34,9 42,5 50,7 58,3"
                  />
                </svg>
              </div>
              <div className="admin-sparkline">
                <div className="admin-sparkline__info">
                  <span className="admin-sparkline__label">Fulfillment</span>
                  <span className="admin-sparkline__value">{data.orders.completed}</span>
                </div>
                <svg className="admin-sparkline__svg" viewBox="0 0 60 20">
                  <polyline 
                    fill="none" 
                    stroke="#8b5cf6" 
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    points="2,16 10,14 18,10 26,12 34,8 42,10 50,6 58,5"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default AdminDashboard;
