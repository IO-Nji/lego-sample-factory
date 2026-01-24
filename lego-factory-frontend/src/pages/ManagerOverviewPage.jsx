import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import { 
  StatCard, 
  Card, 
  Table, 
  Tabs, 
  LoadingSpinner, 
  Alert, 
  Badge,
  Button,
  Footer
} from '../components';
import '../styles/OverviewPages.css';

/**
 * ManagerOverviewPage - Operational Management Overview
 * 
 * Displays comprehensive operational metrics for managers including:
 * - All order types (customer, production, warehouse, supply, assembly)
 * - Inventory status and alerts
 * - Order status distribution
 * - Critical alerts and pending items
 */
function ManagerOverviewPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    totalOrders: 0,
    pendingOrders: 0,
    processingOrders: 0,
    completedOrders: 0,
    customerOrders: 0,
    productionOrders: 0,
    warehouseOrders: 0,
    supplyOrders: 0,
    assemblyOrders: 0,
    lowStockItems: 0,
    criticalAlerts: 0,
  });
  const [recentOrders, setRecentOrders] = useState([]);
  const [criticalItems, setCriticalItems] = useState([]);
  const [statusDistribution, setStatusDistribution] = useState({});

  useEffect(() => {
    fetchManagerData();
  }, []);

  const fetchManagerData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [
        customerOrders,
        productionOrders,
        warehouseOrders,
        supplyOrders,
        assemblyOrders,
        inventory,
      ] = await Promise.all([
        api.get('/customer-orders').catch(() => ({ data: [] })),
        api.get('/production-orders').catch(() => ({ data: [] })),
        api.get('/warehouse-orders').catch(() => ({ data: [] })),
        api.get('/supply-orders').catch(() => ({ data: [] })),
        api.get('/assembly-control-orders').catch(() => ({ data: [] })),
        api.get('/inventory').catch(() => ({ data: [] })),
      ]);

      const customerData = Array.isArray(customerOrders.data) ? customerOrders.data : [];
      const productionData = Array.isArray(productionOrders.data) ? productionOrders.data : [];
      const warehouseData = Array.isArray(warehouseOrders.data) ? warehouseOrders.data : [];
      const supplyData = Array.isArray(supplyOrders.data) ? supplyOrders.data : [];
      const assemblyData = Array.isArray(assemblyOrders.data) ? assemblyOrders.data : [];
      const inventoryData = Array.isArray(inventory.data) ? inventory.data : [];

      // Combine all orders
      const allOrders = [
        ...customerData.map(o => ({ ...o, type: 'Customer' })),
        ...productionData.map(o => ({ ...o, type: 'Production' })),
        ...warehouseData.map(o => ({ ...o, type: 'Warehouse' })),
        ...supplyData.map(o => ({ ...o, type: 'Supply' })),
        ...assemblyData.map(o => ({ ...o, type: 'Assembly' })),
      ];

      // Calculate status distribution
      const statusCounts = allOrders.reduce((acc, order) => {
        acc[order.status] = (acc[order.status] || 0) + 1;
        return acc;
      }, {});

      // Critical items (stock <= 5)
      const critical = inventoryData.filter(item => item.quantity <= 5);
      
      // Low stock items (stock <= 10)
      const lowStock = inventoryData.filter(item => item.quantity <= 10);

      // Recent orders
      const recent = allOrders
        .sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
        .slice(0, 15);

      setStats({
        totalOrders: allOrders.length,
        pendingOrders: (statusCounts.PENDING || 0) + (statusCounts.CONFIRMED || 0),
        processingOrders: statusCounts.PROCESSING || 0,
        completedOrders: statusCounts.COMPLETED || 0,
        customerOrders: customerData.length,
        productionOrders: productionData.length,
        warehouseOrders: warehouseData.length,
        supplyOrders: supplyData.length,
        assemblyOrders: assemblyData.length,
        lowStockItems: lowStock.length,
        criticalAlerts: critical.length,
      });

      setRecentOrders(recent);
      setCriticalItems(critical);
      setStatusDistribution(statusCounts);

    } catch (err) {
      console.error('Error fetching manager data:', err);
      setError('Failed to load manager overview data.');
    } finally {
      setLoading(false);
    }
  };

  const orderColumns = [
    { key: 'orderNumber', label: 'Order #', sortable: true },
    { 
      key: 'type', 
      label: 'Type',
      render: (row) => (
        <Badge variant="secondary" size="small">
          {row.type}
        </Badge>
      )
    },
    { 
      key: 'orderDate', 
      label: 'Date', 
      sortable: true,
      render: (row) => new Date(row.orderDate).toLocaleDateString()
    },
    { 
      key: 'status', 
      label: 'Status',
      render: (row) => (
        <Badge variant={getStatusVariant(row.status)}>
          {row.status}
        </Badge>
      )
    },
  ];

  const criticalColumns = [
    { key: 'itemId', label: 'Item ID', sortable: true },
    { key: 'itemName', label: 'Item Name', sortable: true },
    { 
      key: 'quantity', 
      label: 'Quantity', 
      sortable: true,
      render: (row) => (
        <span style={{ 
          color: 'var(--color-danger-600)',
          fontWeight: 'var(--font-weight-bold)',
          animation: 'pulse 2s ease-in-out infinite'
        }}>
          {row.quantity}
        </span>
      )
    },
  ];

  const getStatusVariant = (status) => {
    const variants = {
      PENDING: 'warning',
      CONFIRMED: 'info',
      PROCESSING: 'primary',
      COMPLETED: 'success',
      CANCELLED: 'danger',
    };
    return variants[status] || 'gray';
  };

  const tabs = [
    {
      id: 'overview',
      label: 'Overview',
      icon: '📊',
      content: (
        <div className="overview-grid">
          <div className="stats-row">
            <StatCard
              value={stats.totalOrders}
              label="TOTAL"
              icon="📋"
              variant="primary"
            />
            <StatCard
              value={stats.pendingOrders}
              label="PENDING"
              icon="⏳"
              variant="warning"
              threshold={20}
              thresholdType="high"
            />
            <StatCard
              value={stats.processingOrders}
              label="PROCESSING"
              icon="⚙️"
              variant="primary"
            />
            <StatCard
              value={stats.completedOrders}
              label="COMPLETED"
              icon="✓"
              variant="success"
            />
          </div>

          <div className="stats-row">
            <StatCard
              value={stats.customerOrders}
              label="CUSTOMER"
              icon="🛒"
              variant="info"
            />
            <StatCard
              value={stats.productionOrders}
              label="PRODUCTION"
              icon="🏭"
              variant="secondary"
            />
            <StatCard
              value={stats.warehouseOrders}
              label="WAREHOUSE"
              icon="📦"
              variant="primary"
            />
            <StatCard
              value={stats.assemblyOrders}
              label="ASSEMBLY"
              icon="🔧"
              variant="info"
            />
          </div>

          <div className="stats-row">
            <StatCard
              value={stats.criticalAlerts}
              label="Critical Stock Alerts"
              icon="🚨"
              variant="danger"
              threshold={3}
              thresholdType="high"
            />
            <StatCard
              value={stats.lowStockItems}
              label="Low Stock Items"
              icon="⚠️"
              variant="warning"
            />
          </div>

          {Object.keys(statusDistribution).length > 0 && (
            <Card header={<h3 className="chart-title">Order Status Distribution</h3>}>
              <div className="status-distribution">
                {Object.entries(statusDistribution).map(([status, count]) => (
                  <div key={status} className="status-item">
                    <div className="status-count">{count}</div>
                    <div className="status-label">{status}</div>
                  </div>
                ))}
              </div>
            </Card>
          )}
        </div>
      ),
    },
    {
      id: 'recent-orders',
      label: 'Recent Orders',
      badge: recentOrders.length,
      content: (
        <Card>
          <Table
            columns={orderColumns}
            data={recentOrders}
            striped
            hoverable
            emptyMessage="No recent orders"
          />
        </Card>
      ),
    },
    {
      id: 'critical',
      label: 'Critical Alerts',
      badge: criticalItems.length,
      content: (
        <Card>
          {criticalItems.length > 0 && (
            <Alert variant="danger" title="Critical Stock Alert" style={{ marginBottom: '1rem' }}>
              {criticalItems.length} items are critically low (≤5 units). Immediate action required!
            </Alert>
          )}
          <Table
            columns={criticalColumns}
            data={criticalItems}
            striped
            hoverable
            emptyMessage="No critical stock alerts"
          />
        </Card>
      ),
    },
  ];

  if (loading) {
    return <LoadingSpinner overlay text="Loading manager overview..." />;
  }

  return (
    <div className="overview-page">
      <div className="overview-header">
        <div>
          <h1>Manager Overview</h1>
          <p className="overview-subtitle">
            Operational metrics and system-wide monitoring • Welcome, {user?.username}
          </p>
        </div>
        <Button 
          variant="primary" 
          onClick={fetchManagerData}
          icon="🔄"
        >
          Refresh Data
        </Button>
      </div>

      {error && (
        <Alert variant="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {stats.criticalAlerts > 0 && (
        <Alert 
          variant="danger" 
          title="⚠️ Critical Alerts" 
          style={{ marginBottom: '1.5rem' }}
        >
          You have {stats.criticalAlerts} critical stock items requiring immediate attention!
        </Alert>
      )}

      <Tabs tabs={tabs} variant="default" />
      <Footer />
    </div>
  );
}

export default ManagerOverviewPage;
