import { useState, useEffect, useRef, useCallback } from "react";
import api from "../../api/api";
import { StatCard, Button, ActivityLog, PageHeader, WorkstationCard, StatisticsGrid, CompactScheduleTimeline, Footer } from "../../components";
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
  
  // Gantt Chart Data
  const [scheduledTasks, setScheduledTasks] = useState([]);
  const [scheduledOrders, setScheduledOrders] = useState([]);

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
  
  // Fetch scheduled tasks for Gantt chart
  const fetchScheduledData = useCallback(async () => {
    try {
      const response = await api.get("/simal/scheduled-orders");
      const orders = Array.isArray(response?.data) ? response.data : [];
      
      setScheduledOrders(orders);
      
      // Extract and format all tasks for timeline (same as ProductionPlanningDashboard)
      const allTasks = [];
      orders.forEach(order => {
        if (order.scheduledTasks && Array.isArray(order.scheduledTasks)) {
          order.scheduledTasks.forEach(task => {
            allTasks.push({
              ...task,
              id: task.taskId,
              workstationName: task.workstationName,
              startTime: task.startTime,
              endTime: task.endTime,
              status: order.status || 'SCHEDULED',
              orderId: order.id,
              scheduleId: order.scheduleId
            });
          });
        }
      });
      setScheduledTasks(allTasks);
    } catch (err) {
      console.error('Failed to fetch scheduled data:', err);
    }
  }, []);

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
        api.get("/masterdata/products").catch((err) => { console.error("Products error:", err.response?.data || err.message); return { data: [] }; }),
      ]);

      const wsData = Array.isArray(wsResponse?.data) ? wsResponse.data : [];
      const usersData = Array.isArray(usersResponse?.data) ? usersResponse.data : [];
      const prodData = Array.isArray(prodResponse?.data) ? prodResponse.data : [];
      const asmData = Array.isArray(asmResponse?.data) ? asmResponse.data : [];
      const supData = Array.isArray(supResponse?.data) ? supResponse.data : [];
      const customerData = Array.isArray(customerResponse?.data) ? customerResponse.data : [];
      const productsData = Array.isArray(productsRes?.data) ? productsRes.data : [];

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
      
      // Fetch scheduled data for Gantt chart
      fetchScheduledData();

      // OPTIMIZATION: Only update state if data has actually changed
      // Use JSON comparison for deep equality check
      const newDataString = JSON.stringify(newData);
      const hasChanged = !lastDataRef.current || lastDataRef.current !== newDataString;

      if (hasChanged) {
        setDashboardData(newData);
        lastDataRef.current = newDataString;
      }
    } catch (err) {
      setError("Failed to load dashboard data: " + (err.message || "Unknown error"));
      console.error("Dashboard fetch error:", err);
    } finally {
      setLoading(false);
    }
  }, [dashboardData.totalOrders, fetchAuditLogs, fetchScheduledData]);

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

      {/* Row 1: Activity Log (flex) | Order Status Chart | Production Type Chart */}
      <div style={{ 
        display: 'flex',
        gap: '1.25rem',
        marginBottom: '1.5rem',
        alignItems: 'stretch',
        height: '320px'
      }}>
        {/* Column 1: System Activity Log */}
        <div style={{ 
          flex: '1',
          minWidth: '0',
          height: '100%',
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <ActivityLog 
            notifications={notifications}
            title="SYSTEM ACTIVITY LOG"
            maxVisible={10}
            onClear={clearNotifications}
            titleStyle={{ textAlign: 'left' }}
          />
        </div>

        {/* Column 2: Order Status Distribution */}
        <div style={{ 
          flexShrink: 0,
          width: '260px',
          height: '100%',
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <PieChart 
            title="ORDER STATUS DISTRIBUTION"
            data={orderStatusData}
            size={135}
          />
        </div>

        {/* Column 3: Production by Type */}
        <div style={{ 
          flexShrink: 0,
          width: '260px',
          height: '100%',
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <PieChart 
            title="PRODUCTION STATUS TYPE"
            data={productionTypeData}
            size={135}
          />
        </div>
      </div>

      {/* Row 2: Statistics Grid | Gantt Chart */}
      <div style={{ 
        display: 'flex',
        gap: '1.25rem',
        marginBottom: '1.5rem',
        alignItems: 'stretch',
        height: '360px'
      }}>
        {/* Column 1: Statistics Grid */}
        <div style={{ 
          flexShrink: 0,
          height: '100%',
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
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

        {/* Column 2: Gantt Chart */}
        <div style={{ 
          flex: '1',
          minWidth: '0',
          height: '100%',
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <CompactScheduleTimeline
            scheduledTasks={scheduledTasks}
            onTaskClick={(task) => {
              console.log('Task clicked:', task);
              addNotification(`Viewing task: ${task.taskType || task.operationType || 'Task'}`, 'info');
            }}
            title="Production Schedule Timeline"
          />
        </div>
      </div>

      {/* Row 3: Workstation Monitor + User Roles + Recent Orders */}
      <div style={{ display: 'flex', gap: '1.25rem', alignItems: 'stretch' }}>
        {/* Workstation Status Monitor */}
        <div style={{ 
          flex: '0 0 calc(40% - 0.83rem)',
          minWidth: 0,
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem'
        }}>
          <h3 className="component-title" style={{ marginBottom: '1rem' }}>WORKSTATION STATUS MONITOR</h3>
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '0.6rem',
            padding: '0',
            justifyItems: 'center',
            alignItems: 'center',
            justifyContent: 'center',
            alignContent: 'center'
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
                      onClick={() => {}}
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
          
        {/* User Roles Distribution - Horizontal Layout */}
        <div style={{ 
          flex: '1',
          minWidth: 0,
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <PieChart 
            title="USER ROLES DISTRIBUTION"
            data={userRoleData}
            size={120}
            layout="horizontal"
          />
        </div>
          
        {/* Recent Orders */}
        <div style={{ 
          flex: '0 0 calc(30% - 0.83rem)',
          minWidth: 0,
          backgroundColor: 'white',
          borderRadius: '0.5rem',
          border: '1px solid var(--color-border)',
          padding: '1rem'
        }}>
          <h3 className="component-title" style={{ marginBottom: '1rem' }}>RECENT ORDERS</h3>
          {dashboardData.recentOrders.length > 0 ? (
            <div style={{ overflowY: 'auto', maxHeight: '280px' }}>
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

      {/* Footer */}
      <Footer />
    </div>
  );
}

export default AdminDashboard;
