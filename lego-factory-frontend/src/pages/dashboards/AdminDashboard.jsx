import { useState, useEffect, useRef, useCallback } from "react";
import api from "../../api/api";
import { StatCard, Button, ActivityLog, PageHeader, WorkstationCard, StatisticsGrid } from "../../components";
import PieChart from "../../components/PieChart";
import BarChart from "../../components/BarChart";
import "../../styles/Chart.css";
import "../../styles/DashboardLayout.css";

/**
 * AdminDashboard - Administrator dashboard with system-wide monitoring
 * Displays order statistics, workstation status, user analytics, and production metrics
 * 
 * OPTIMIZATION: Uses shallow comparison to prevent unnecessary re-renders
 * Only updates state when data actually changes
 * STANDARDIZED: Uses ActivityLog, StatisticsGrid, and custom layout for complex admin features
 */
function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [dashboardData, setDashboardData] = useState({
    totalOrders: 0,
    pendingOrders: 0,
    completedOrders: 0,
    processingOrders: 0,
    cancelledOrders: 0,
    activeWorkstations: 0,
    totalUsers: 0,
    totalProducts: 0,
    lowStockItems: 0,
    workstations: [],
    users: [],
    recentOrders: [],
    productionByType: {},
  });

  // Track last data to prevent unnecessary updates
  const lastDataRef = useRef(null);
  // previousOrdersRef removed - now using backend audit logs

  const addNotification = useCallback((message, type = 'info', userRole = 'ADMIN') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: getRoleAcronym(userRole)
    };
    setNotifications(prev => [newNotification, ...prev]);
  }, []);

  const clearNotifications = () => {
    setNotifications([]);
  };

  // Function to convert audit logs to notifications
  const auditLogToNotification = (audit) => {
    // Remove "Order " prefix from description if it exists
    let message = audit.description;
    if (message && message.startsWith('Order ')) {
      message = message.substring(6); // Remove "Order " (6 characters)
    }
    
    // Determine notification type based on event
    let type = 'info';
    if (audit.eventType.includes('COMPLETED')) {
      type = 'success';
    } else if (audit.eventType.includes('CANCELLED') || audit.eventType.includes('ERROR')) {
      type = 'error';
    } else if (audit.eventType.includes('PROCESSING') || audit.eventType.includes('IN_PROGRESS')) {
      type = 'info';
    } else if (audit.eventType.includes('PENDING') || audit.eventType.includes('CREATED')) {
      type = 'warning';
    }
    
    return {
      id: audit.id,
      message: message || `${audit.eventType} for ${audit.orderType} order ${audit.orderId}`,
      type: type,
      timestamp: audit.createdAt,
      station: getRoleAcronym(audit.userRole || 'ADMIN')
    };
  };

  // Fetch audit logs from backend
  const fetchAuditLogs = useCallback(async () => {
    try {
      const response = await api.get('/orders/audit/recent?limit=50');
      const auditLogs = response.data || [];
      
      // Convert audit logs to notifications
      const auditNotifications = auditLogs.map(auditLogToNotification);
      
      // Only update if there are new logs
      if (auditNotifications.length > 0) {
        setNotifications(auditNotifications);
      }
    } catch (err) {
      console.error('Failed to fetch audit logs:', err);
    }
  }, []);

  // Function to detect and log order status changes (DEPRECATED - now using backend audit logs)
  /* DEPRECATED: detectOrderChanges
  const detectOrderChanges = (allOrders) => {
    const currentOrdersMap = new Map();
    
    // Build current orders map
    allOrders.forEach(order => {
      const key = `${order.orderNumber || order.id}`;
      currentOrdersMap.set(key, order);
    });

    // Compare with previous orders to detect changes
    if (previousOrdersRef.current.size > 0) {
      currentOrdersMap.forEach((currentOrder, key) => {
        const previousOrder = previousOrdersRef.current.get(key);
        
        if (!previousOrder) {
          // New order created
          const orderType = getOrderType(currentOrder.orderNumber);
          addNotification(
            `New ${orderType} order created: ${currentOrder.orderNumber || \`#${currentOrder.id}\`}`,
            'info'
          );
        } else if (previousOrder.status !== currentOrder.status) {
          // Status changed
          const orderType = getOrderType(currentOrder.orderNumber);
          const statusType = currentOrder.status === 'COMPLETED' ? 'success' : 
                           currentOrder.status === 'CANCELLED' ? 'error' :
                           currentOrder.status === 'PROCESSING' ? 'info' : 'warning';
          
          addNotification(
            \`${orderType} order ${currentOrder.orderNumber || \`#${currentOrder.id}\`}: ${previousOrder.status} → ${currentOrder.status}\`,
            statusType
          );
        }
      });
    }

    // Update previous orders reference
    previousOrdersRef.current = currentOrdersMap;
  };
  */

  // Map role names to acronyms for compact display
  const getRoleAcronym = (role) => {
    const roleMap = {
      'ADMIN_Dashboard': 'ADMIN',
      'ADMIN': 'ADMIN',
      'PLANT_WAREHOUSE': 'PLANT-WH',
      'MODULES_SUPERMARKET': 'MODS-SP',
      'PRODUCTION_PLANNING': 'PROD-PL',
      'PRODUCTION_CONTROL': 'PROD-CN',
      'ASSEMBLY_CONTROL': 'ASSM-CN',
      'MANUFACTURING': 'MANFCT',
      'PARTS_SUPPLY': 'PARTS-SP',
      'VIEWER': 'VIEWER'
    };
    return roleMap[role] || role;
  };

  // Determine order type from order number prefix
  const getOrderType = (orderNumber) => {
    if (!orderNumber) return 'Order';
    const prefix = orderNumber.split('-')[0];
    if (prefix === 'CUST') return 'Customer';
    if (prefix === 'PROD') return 'Production';
    if (prefix === 'ASM') return 'Assembly';
    if (prefix === 'WH' || prefix === 'SUP') return 'Supply';
    return 'Order';
  };

  const fetchDashboardData = useCallback(async () => {
    try {
      // Only set loading on initial load, not on refreshes
      if (dashboardData.totalOrders === 0) {
        setLoading(true);
      }
      setError(null);

      // Fetch all data from APIs
      const [wsResponse, usersResponse, prodResponse, asmResponse, supResponse, customerResponse, lowAlertsRes, productsRes] = await Promise.all([
        api.get("/masterdata/workstations").catch((err) => { console.error("WS error:", err.response?.data || err.message); return { data: [] }; }),
        api.get("/users").catch((err) => { console.error("Users error:", err.response?.data || err.message); return { data: [] }; }),
        api.get("/production-control-orders").catch((err) => { console.error("Prod orders error:", err.response?.status, err.response?.data || err.message); return { data: [] }; }),
        api.get("/assembly-control-orders").catch((err) => { console.error("Asm orders error:", err.response?.status, err.response?.data || err.message); return { data: [] }; }),
        api.get("/supply-orders/warehouse").catch((err) => { console.error("Supply orders error:", err.response?.status, err.response?.data || err.message); return { data: [] }; }),
        api.get("/customer-orders").catch((err) => { console.error("Customer orders error:", err.response?.status, err.response?.data || err.message); return { data: [] }; }),
        api.get("/stock/alerts/low").catch((err) => { console.error("Stock alerts error:", err.response?.status, err.response?.data || err.message); return { data: [] }; }),
        api.get("/masterdata/product-variants").catch((err) => { console.error("Products error:", err.response?.data || err.message); return { data: [] }; }),
      ]);

      const wsData = Array.isArray(wsResponse?.data) ? wsResponse.data : [];
      const usersData = Array.isArray(usersResponse?.data) ? usersResponse.data : [];
      const prodData = Array.isArray(prodResponse?.data) ? prodResponse.data : [];
      const asmData = Array.isArray(asmResponse?.data) ? asmResponse.data : [];
      const supData = Array.isArray(supResponse?.data) ? supResponse.data : [];
      const customerData = Array.isArray(customerResponse?.data) ? customerResponse.data : [];
      const productsData = Array.isArray(productsRes?.data) ? productsRes.data : [];

      console.log("Dashboard Data Fetched:", {
        workstations: wsData.length,
        users: usersData.length,
        productionOrders: prodData.length,
        assemblyOrders: asmData.length,
        supplyOrders: supData.length,
        customerOrders: customerData.length,
        products: productsData.length,
        lowStockAlerts: Array.isArray(lowAlertsRes?.data) ? lowAlertsRes.data.length : 0
      });

      // Combine all orders
      const allOrders = [...customerData, ...prodData, ...asmData, ...supData];
      const pendingOrders = allOrders.filter((o) => o.status === "PENDING").length;
      const completedOrders = allOrders.filter((o) => o.status === "COMPLETED").length;
      const processingOrders = allOrders.filter((o) => o.status === "PROCESSING").length;
      const cancelledOrders = allOrders.filter((o) => o.status === "CANCELLED").length;

      // Production by type
      const productionByType = {
        customer: customerData.length,
        production: prodData.length,
        assembly: asmData.length,
        supply: supData.length,
      };

      // Workstation status mapping with shortened names
      const workstationStatus = wsData.map(ws => {
        // Shorten names for compact display
        let shortName = ws.name || `WS-${ws.id}`;
        shortName = shortName
          .replace('Workstation', 'WS')
          .replace('Station', 'Stn')
          .replace('Assembly', 'Assy')
          .replace('Production', 'Prod')
          .replace('Manufacturing', 'Mfg')
          .replace('Warehouse', 'WH')
          .replace('Control', 'Ctrl');
        
        // Check for active/pending orders at this workstation
        const activeOrdersAtStation = allOrders.filter(order => {
          // Check if order is assigned to this workstation and not completed/cancelled
          return order.workstationId === ws.id && 
                 ['PENDING', 'PROCESSING', 'CONFIRMED', 'IN_PROGRESS'].includes(order.status);
        }).length;
        
        return {
          id: ws.id,
          name: shortName,
          status: ws.status || 'ACTIVE',
          details: ws.description || `Type: ${ws.type || 'N/A'}`,
          hasActiveOrders: activeOrdersAtStation > 0,
        };
      });

      // Recent orders (last 6)
      const recentOrders = [...allOrders]
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        .slice(0, 6);

      const newData = {
        totalOrders: allOrders.length,
        pendingOrders,
        completedOrders,
        processingOrders,
        cancelledOrders,
        activeWorkstations: wsData.length,
        totalUsers: usersData.length,
        totalProducts: productsData.length,
        lowStockItems: Array.isArray(lowAlertsRes?.data) ? lowAlertsRes.data.length : 0,
        workstations: workstationStatus,
        users: usersData,
        recentOrders,
        productionByType,
      };

      // Fetch audit logs for notifications (replaced detectOrderChanges)
      fetchAuditLogs();

      // OPTIMIZATION: Only update state if data has actually changed
      // Use JSON comparison for deep equality check
      const newDataString = JSON.stringify(newData);
      const hasChanged = !lastDataRef.current || lastDataRef.current !== newDataString;

      if (hasChanged) {
        console.log('Dashboard data changed - updating UI');
        setDashboardData(newData);
        lastDataRef.current = newDataString;
      } else {
        console.log('Dashboard data unchanged - skipping render');
      }
    } catch (err) {
      setError("Failed to load dashboard data: " + (err.message || "Unknown error"));
      console.error("Dashboard fetch error:", err);
    } finally {
      setLoading(false);
    }
  }, [dashboardData.totalOrders, fetchAuditLogs]);

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 30000); // Increased to 30s to reduce page jump
    return () => clearInterval(interval);
  }, [fetchDashboardData]);

  const isInitialLoad = loading && dashboardData.totalOrders === 0;

  if (isInitialLoad) {
    return (
      <div className="standard-page-container">
        <PageHeader title="Admin Dashboard" subtitle="Loading..." icon="🏭" />
        <section style={{ padding: '2rem', textAlign: 'center' }}>
          <div className="loading-state">Loading dashboard data...</div>
        </section>
      </div>
    );
  }

  // Prepare chart data
  const orderStatusData = [
    { label: 'Pending', value: dashboardData.pendingOrders, color: '#f59e0b' },
    { label: 'Processing', value: dashboardData.processingOrders, color: '#3b82f6' },
    { label: 'Completed', value: dashboardData.completedOrders, color: '#10b981' },
    { label: 'Cancelled', value: dashboardData.cancelledOrders, color: '#6b7280' },
  ].filter(item => item.value > 0);

  const productionTypeData = [
    { label: 'Customer', value: dashboardData.productionByType.customer, color: '#2c5aa0' },
    { label: 'Production', value: dashboardData.productionByType.production, color: '#f59e0b' },
    { label: 'Assembly', value: dashboardData.productionByType.assembly, color: '#10b981' },
    { label: 'Supply', value: dashboardData.productionByType.supply, color: '#6366f1' },
  ].filter(item => item.value > 0);

  const userRoleData = dashboardData.users.reduce((acc, user) => {
    const role = user.role || 'Unknown';
    const roleLabel = getRoleAcronym(role);
    const existing = acc.find(item => item.label === roleLabel);
    if (existing) {
      existing.value += 1;
    } else {
      acc.push({ 
        label: roleLabel, 
        value: 1, 
        color: `hsl(${acc.length * 60}, 70%, 50%)` 
      });
    }
    return acc;
  }, []);

  return (
    <div className="dashboard-layout">
      {/* Page Header */}
      <PageHeader
        title="Admin Dashboard"
        subtitle="Real-time monitoring and control of factory operations"
        icon="🏭"
      />

      {/* Error Message */}
      {error && (
        <div className="dashboard-message dashboard-message-error">
          <p className="dashboard-message-title">Error</p>
          <p className="dashboard-message-text">{error}</p>
          <button onClick={() => setError(null)} className="dashboard-message-dismiss">
            Dismiss
          </button>
        </div>
      )}

      {/* Refresh Button */}
      <div style={{ marginBottom: '1.5rem', display: 'flex', justifyContent: 'flex-end' }}>
        <Button 
          variant="primary" 
          onClick={fetchDashboardData} 
          disabled={loading}
          size="small"
        >
          {loading ? "⟳ Refreshing..." : "⟳ Refresh"}
        </Button>
      </div>

      {/* Row 1: System Activity Log (flex) | Statistics Grid (fixed) | Stacked Charts (fixed) */}
      <div style={{ 
        display: 'flex',
        gap: '1.25rem',
        marginBottom: '2rem',
        alignItems: 'stretch',
        height: '380px'
      }}>
        {/* Column 1: System Activity Log - Takes remaining space, constrained height */}
        <div style={{ 
          flex: '1',
          height: '100%',
          maxHeight: '380px',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <div style={{ height: '100%', overflow: 'hidden' }}>
            <ActivityLog 
              notifications={notifications}
              title="SYSTEM ACTIVITY LOG"
              maxVisible={10}
              onClear={clearNotifications}
            />
          </div>
        </div>

        {/* Column 2: Statistics Grid - Fixed width, compact 4x2 layout, centered vertically */}
        <div style={{ 
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%'
        }}>
          <StatisticsGrid 
            stats={[
              { value: dashboardData.totalOrders, label: "TOTAL", variant: "primary", icon: "📦" },
              { value: dashboardData.pendingOrders, label: "PENDING", variant: "warning", icon: "⏳" },
              { value: dashboardData.processingOrders, label: "PROCESSING", variant: "info", icon: "⚙️" },
              { value: dashboardData.completedOrders, label: "COMPLETED", variant: "success", icon: "✓" },
              { value: dashboardData.activeWorkstations, label: "WORKSTATIONS", variant: "primary", icon: "🏭" },
              { value: dashboardData.totalUsers, label: "USERS", variant: "info", icon: "👥" },
              { value: dashboardData.totalProducts, label: "PRODUCTS", variant: "success", icon: "🎨" },
              { value: dashboardData.lowStockItems, label: "LOW STOCK", variant: "danger", icon: "⚠️" },
            ]}
          />
        </div>

        {/* Column 3: Stacked Charts - Fixed width, centered vertically */}
        <div style={{ 
          display: 'flex',
          flexDirection: 'column',
          gap: '1rem',
          flexShrink: 0,
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%'
        }}>
          <PieChart 
            title="ORDER STATUS DISTRIBUTION"
            data={orderStatusData}
            size={110}
          />
          <PieChart 
            title="PRODUCTION BY TYPE"
            data={productionTypeData}
            size={110}
          />
        </div>
      </div>

      {/* Row 2: Workstation Monitor + User Roles + Recent Orders */}
      <div style={{ padding: '0', marginTop: '2rem' }}>
        <div style={{ 
          display: 'flex', 
          gap: '1.25rem',
          alignItems: 'stretch'
        }}>
          {/* Workstation Status Monitor - 33% */}
          <div style={{ flex: '0 0 calc(33.33% - 0.67rem)', minWidth: 0 }}>
            <div className="chart-container" style={{ padding: '0.5rem' }}>
              <h3 className="component-title">WORKSTATION STATUS MONITOR</h3>
              <div style={{ 
                display: 'grid', 
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '0.35rem',
                padding: '0'
              }}>
                {dashboardData.workstations.map((ws) => {
                  // Determine status based on actual orders at workstation
                  // 'active' (yellow) only when workstation has pending/processing orders
                  // 'idle' (blue) when no active orders
                  const status = ws.hasActiveOrders ? 'active' : 'idle';
                  
                  // Get icon based on workstation name
                  const getWorkstationIcon = (name) => {
                    const nameLower = (name || '').toLowerCase();
                    if (nameLower.includes('plant') && nameLower.includes('wh')) return '🏭';
                    if (nameLower.includes('modules') || nameLower.includes('mods')) return '🏢';
                    if (nameLower.includes('parts')) return '📦';
                    if (nameLower.includes('final') && nameLower.includes('assy')) return '🔨';
                    if (nameLower.includes('gear')) return '⚙️';
                    if (nameLower.includes('motor')) return '🔧';
                    if (nameLower.includes('injection')) return '💉';
                    if (nameLower.includes('pre')) return '⚡';
                    if (nameLower.includes('finishing')) return '✨';
                    return '⚙️';
                  };
                  
                  return (
                    <WorkstationCard
                      key={ws.id}
                      icon={getWorkstationIcon(ws.name)}
                      name={ws.name}
                      tooltip={`${ws.details || 'Workstation details'} | Status: ${ws.status || 'ACTIVE'}`}
                      status={status}
                      onClick={() => console.log('Workstation clicked:', ws)}
                    />
                  );
                })}
                {dashboardData.workstations.length === 0 && (
                  <div style={{ gridColumn: '1 / -1', textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                    No workstations available
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* User Roles Distribution - 33% */}
          <div style={{ flex: '0 0 calc(33.33% - 0.67rem)', minWidth: 0 }}>
            <div className="chart-container" style={{ padding: '0.5rem', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
              <PieChart 
                title="USER ROLES DISTRIBUTION"
                data={userRoleData}
                size={180}
              />
            </div>
          </div>
          
          {/* Recent Orders - 33% */}
          <div className="chart-container" style={{ flex: '0 0 calc(33.33% - 0.67rem)', minWidth: 0, padding: '0.5rem' }}>
            <h3 className="component-title">RECENT ORDERS</h3>
            {dashboardData.recentOrders.length > 0 ? (
              <div style={{ overflowY: 'auto', maxHeight: '320px' }}>
                <table style={{ 
                  width: '100%', 
                  borderCollapse: 'collapse',
                  fontSize: '0.7rem',
                  tableLayout: 'fixed'
                }}>
                  <thead style={{ position: 'sticky', top: 0, background: 'white', zIndex: 1 }}>
                    <tr style={{ borderBottom: '2px solid var(--color-border)' }}>
                      <th style={{ padding: '0.3rem 0.2rem', textAlign: 'left', fontWeight: 600, fontSize: '0.65rem', width: '35px' }}>ID</th>
                      <th style={{ padding: '0.3rem 0.2rem', textAlign: 'left', fontWeight: 600, fontSize: '0.65rem', width: '90px' }}>Order No.</th>
                      <th style={{ padding: '0.3rem 0.2rem', textAlign: 'left', fontWeight: 600, fontSize: '0.65rem', width: '70px' }}>Type</th>
                      <th style={{ padding: '0.3rem 0.2rem', textAlign: 'left', fontWeight: 600, fontSize: '0.65rem', width: '85px' }}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboardData.recentOrders.map((order) => {
                      const statusColors = {
                        PENDING: '#f59e0b',
                        PROCESSING: '#3b82f6',
                        COMPLETED: '#10b981',
                        CANCELLED: '#6b7280',
                      };
                      
                      // Determine order type based on orderNumber prefix
                      let orderType = 'Unknown';
                      if (order.orderNumber) {
                        const prefix = order.orderNumber.split('-')[0];
                        if (prefix === 'CUST') orderType = 'Customer';
                        else if (prefix === 'PROD') orderType = 'Production';
                        else if (prefix === 'ASM') orderType = 'Assembly';
                        else if (prefix === 'WH' || prefix === 'SUP') orderType = 'Warehouse';
                      }
                      
                      return (
                        <tr key={order.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                          <td style={{ padding: '0.25rem 0.2rem', fontSize: '0.65rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>#{order.id}</td>
                          <td style={{ padding: '0.25rem 0.2rem', fontSize: '0.65rem', fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{order.orderNumber || 'N/A'}</td>
                          <td style={{ padding: '0.25rem 0.2rem', fontSize: '0.65rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{orderType}</td>
                          <td style={{ padding: '0.25rem 0.2rem' }}>
                            <span style={{ 
                              padding: '0.1rem 0.3rem',
                              borderRadius: '3px',
                              fontSize: '0.6rem',
                              fontWeight: 600,
                              color: 'white',
                              backgroundColor: statusColors[order.status] || '#6b7280',
                              display: 'inline-block',
                              whiteSpace: 'nowrap'
                            }}>
                              {order.status}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="chart-empty">No recent orders</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;
