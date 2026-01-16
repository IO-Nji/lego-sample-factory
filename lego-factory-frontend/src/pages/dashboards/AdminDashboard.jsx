import { useState, useEffect, useRef, useCallback } from "react";
import api from "../../api/api";
import { StatCard, Button, Notification, PageHeader } from "../../components";
import PieChart from "../../components/PieChart";
import BarChart from "../../components/BarChart";
import StatusMonitor from "../../components/StatusMonitor";
import "../../styles/Chart.css";
import "../../styles/DashboardLayout.css";

/**
 * AdminDashboard - Administrator dashboard with system-wide monitoring
 * Displays order statistics, workstation status, user analytics, and production metrics
 * 
 * OPTIMIZATION: Uses shallow comparison to prevent unnecessary re-renders
 * Only updates state when data actually changes
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
        
        return {
          id: ws.id,
          name: shortName,
          status: ws.status || 'ACTIVE',
          details: ws.description || `Type: ${ws.type || 'N/A'}`,
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

      {/* Row 1: 8 StatCards in a single row */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(8, 1fr)', 
        gap: '1rem',
        marginBottom: '1.5rem',
        justifyItems: 'center'
      }}>
        <StatCard 
          title="TOTAL"
          value={dashboardData.totalOrders}
          icon="📦"
          color="primary"
        />
        <StatCard 
          title="PENDING"
          value={dashboardData.pendingOrders}
          icon="⏳"
          color="warning"
          threshold={10}
          thresholdType="high"
        />
        <StatCard 
          title="Processing"
          value={dashboardData.processingOrders}
          icon="⚙️"
          color="info"
        />
        <StatCard 
          title="Completed"
          value={dashboardData.completedOrders}
          icon="✓"
          color="success"
        />
        <StatCard 
          title="Workstations"
          value={dashboardData.activeWorkstations}
          icon="🏭"
          color="primary"
        />
        <StatCard 
          title="Users"
          value={dashboardData.totalUsers}
          icon="👥"
          color="info"
        />
        <StatCard 
          title="Products"
          value={dashboardData.totalProducts}
          icon="🎨"
          color="success"
        />
        <StatCard 
          title="Low Stock"
          value={dashboardData.lowStockItems}
          icon="⚠️"
          color="danger"
          threshold={1}
          thresholdType="low"
        />
      </div>

      {/* Row 2: 3 Columns - Notifications, 2 Charts, User Roles Chart */}
      <div style={{ 
        display: 'flex',
        gap: '1rem',
        marginBottom: '1.5rem',
        alignItems: 'flex-start'
      }}>
        {/* Column 1: Notifications - Takes remaining space */}
        <div style={{ 
          flex: '1',
          minHeight: '400px'
        }}>
          <Notification 
            notifications={notifications}
            title="System Activity"
            maxVisible={10}
            onClear={clearNotifications}
          />
        </div>

        {/* Column 2: Existing Charts - Fixed width */}
        <div style={{ 
          display: 'flex',
          flexDirection: 'column',
          gap: '1rem',
          flexShrink: 0
        }}>
          <PieChart 
            title="Order Status Distribution"
            data={orderStatusData}
            size={110}
          />
          <PieChart 
            title="Production by Type"
            data={productionTypeData}
            size={110}
          />
        </div>

        {/* Column 3: User Roles Chart - Fixed width, vertically centered */}
        <div style={{ 
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '400px',
          flexShrink: 0
        }}>
          <PieChart 
            title="User Roles Distribution"
            data={userRoleData}
            size={180}
          />
        </div>
      </div>

      {/* Row 3: Workstation Monitor + Recent Orders (unchanged) */}
      <div style={{ padding: '0' }}>
        <div style={{ 
          display: 'flex', 
          gap: '1rem',
          alignItems: 'stretch'
        }}>
          {/* Workstation Status Monitor - 50% */}
          <div style={{ flex: '0 0 calc(50% - 0.5rem)', minWidth: 0 }}>
            <StatusMonitor 
              title="Workstation Status Monitor"
              items={dashboardData.workstations}
              onItemClick={(item) => console.log('Workstation clicked:', item)}
              compact={true}
              gridLayout={true}
            />
          </div>
          
          {/* Recent Orders - 50% */}
          <div className="chart-container" style={{ flex: '0 0 calc(50% - 0.5rem)', minWidth: 0, minHeight: '400px' }}>
            <h3 className="chart-title">RECENT ORDERS</h3>
            {dashboardData.recentOrders.length > 0 ? (
              <div style={{ overflowY: 'auto', maxHeight: '350px' }}>
                <table style={{ 
                  width: '100%', 
                  borderCollapse: 'collapse',
                  fontSize: '0.75rem'
                }}>
                  <thead style={{ position: 'sticky', top: 0, background: 'white', zIndex: 1 }}>
                    <tr style={{ borderBottom: '2px solid var(--color-border)' }}>
                      <th style={{ padding: '0.4rem 0.3rem', textAlign: 'left', fontWeight: 600, fontSize: '0.7rem', width: '15%' }}>ID</th>
                      <th style={{ padding: '0.4rem 0.3rem', textAlign: 'left', fontWeight: 600, fontSize: '0.7rem', width: '30%' }}>Order No.</th>
                      <th style={{ padding: '0.4rem 0.3rem', textAlign: 'left', fontWeight: 600, fontSize: '0.7rem', width: '25%' }}>Type</th>
                      <th style={{ padding: '0.4rem 0.3rem', textAlign: 'left', fontWeight: 600, fontSize: '0.7rem', width: '30%' }}>Status</th>
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
                          <td style={{ padding: '0.4rem 0.3rem', fontSize: '0.7rem' }}>#{order.id}</td>
                          <td style={{ padding: '0.4rem 0.3rem', fontSize: '0.7rem', fontFamily: 'monospace' }}>{order.orderNumber || 'N/A'}</td>
                          <td style={{ padding: '0.4rem 0.3rem', fontSize: '0.7rem' }}>{orderType}</td>
                          <td style={{ padding: '0.4rem 0.3rem' }}>
                            <span style={{ 
                              padding: '0.15rem 0.35rem',
                              borderRadius: '3px',
                              fontSize: '0.65rem',
                              fontWeight: 600,
                              color: 'white',
                              backgroundColor: statusColors[order.status] || '#6b7280',
                              display: 'inline-block'
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
