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
import '../styles/OverviewPage.css';

/**
 * AdminOverviewPage - System-wide Overview for Administrators
 * 
 * Displays comprehensive system statistics including:
 * - Total orders (customer, production, warehouse, supply)
 * - Inventory status and low stock alerts
 * - User activity and system health
 * - Recent orders and critical alerts
 * 
 * Uses new design system components for consistency
 */
function AdminOverviewPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  // State management
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    totalCustomerOrders: 0,
    totalProductionOrders: 0,
    totalWarehouseOrders: 0,
    totalSupplyOrders: 0,
    pendingOrders: 0,
    completedOrders: 0,
    totalInventoryItems: 0,
    lowStockItems: 0,
    totalUsers: 0,
    activeWorkstations: 0,
  });
  const [recentOrders, setRecentOrders] = useState([]);
  const [lowStockInventory, setLowStockInventory] = useState([]);
  const [ordersByStatus, setOrdersByStatus] = useState({});

  // Fetch all data on mount
  useEffect(() => {
    fetchOverviewData();
  }, []);

  const fetchOverviewData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch all data in parallel
      const [
        customerOrders,
        productionOrders,
        warehouseOrders,
        supplyOrders,
        inventory,
      ] = await Promise.all([
        api.get('/customer-orders').catch(() => ({ data: [] })),
        api.get('/production-orders').catch(() => ({ data: [] })),
        api.get('/warehouse-orders').catch(() => ({ data: [] })),
        api.get('/supply-orders').catch(() => ({ data: [] })),
        api.get('/inventory').catch(() => ({ data: [] })),
      ]);

      // Process customer orders
      const customerOrdersData = Array.isArray(customerOrders.data) ? customerOrders.data : [];
      const productionOrdersData = Array.isArray(productionOrders.data) ? productionOrders.data : [];
      const warehouseOrdersData = Array.isArray(warehouseOrders.data) ? warehouseOrders.data : [];
      const supplyOrdersData = Array.isArray(supplyOrders.data) ? supplyOrders.data : [];
      const inventoryData = Array.isArray(inventory.data) ? inventory.data : [];

      // Calculate statistics
      const allOrders = [
        ...customerOrdersData,
        ...productionOrdersData,
        ...warehouseOrdersData,
        ...supplyOrdersData,
      ];

      const statusCounts = allOrders.reduce((acc, order) => {
        acc[order.status] = (acc[order.status] || 0) + 1;
        return acc;
      }, {});

      // Low stock items (threshold: 10)
      const lowStock = inventoryData.filter(item => item.quantity <= 10);

      // Recent orders (last 10)
      const recent = [...customerOrdersData]
        .sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
        .slice(0, 10);

      setStats({
        totalCustomerOrders: customerOrdersData.length,
        totalProductionOrders: productionOrdersData.length,
        totalWarehouseOrders: warehouseOrdersData.length,
        totalSupplyOrders: supplyOrdersData.length,
        pendingOrders: (statusCounts.PENDING || 0) + (statusCounts.CONFIRMED || 0),
        completedOrders: statusCounts.COMPLETED || 0,
        totalInventoryItems: inventoryData.length,
        lowStockItems: lowStock.length,
        totalUsers: 0, // Would need separate API call
        activeWorkstations: 0, // Would need separate API call
      });

      setRecentOrders(recent);
      setLowStockInventory(lowStock);
      setOrdersByStatus(statusCounts);

    } catch (err) {
      console.error('Error fetching overview data:', err);
      setError('Failed to load overview data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Table columns for recent orders
  const orderColumns = [
    { 
      key: 'orderNumber', 
      label: 'Order #', 
      sortable: true 
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
    { 
      key: 'actions', 
      label: 'Actions',
      render: (row) => (
        <Button 
          size="small" 
          variant="outline"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/orders/${row.id}`);
          }}
        >
          View
        </Button>
      )
    },
  ];

  // Table columns for low stock
  const inventoryColumns = [
    { 
      key: 'itemId', 
      label: 'Item ID', 
      sortable: true 
    },
    { 
      key: 'itemName', 
      label: 'Item Name', 
      sortable: true 
    },
    { 
      key: 'quantity', 
      label: 'Quantity', 
      sortable: true,
      render: (row) => (
        <span style={{ color: row.quantity <= 5 ? 'var(--color-danger-600)' : 'var(--color-warning-600)' }}>
          {row.quantity}
        </span>
      )
    },
    { 
      key: 'actions', 
      label: 'Actions',
      render: (row) => (
        <Button 
          size="small" 
          variant="warning"
          onClick={(e) => {
            e.stopPropagation();
            navigate('/control/inventory');
          }}
        >
          Restock
        </Button>
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

  // Tabs for different views
  const tabs = [
    {
      id: 'overview',
      label: 'Overview',
      icon: '📊',
      content: (
        <div className="overview-grid">
          <div className="stats-row">
            <StatCard
              value={stats.totalCustomerOrders}
              label="CUSTOMER"
              icon="🛒"
              variant="primary"
              onClick={() => navigate('/control/orders')}
            />
            <StatCard
              value={stats.totalProductionOrders}
              label="PRODUCTION"
              icon="🏭"
              variant="secondary"
              onClick={() => navigate('/control/production-orders')}
            />
            <StatCard
              value={stats.totalWarehouseOrders}
              label="WAREHOUSE"
              icon="📦"
              variant="info"
              onClick={() => navigate('/control/warehouse-orders')}
            />
            <StatCard
              value={stats.totalSupplyOrders}
              label="Supply Orders"
              icon="🚚"
              variant="success"
            />
          </div>

          <div className="stats-row">
            <StatCard
              value={stats.pendingOrders}
              label="Pending Orders"
              icon="⏳"
              variant="warning"
              threshold={10}
              thresholdType="high"
            />
            <StatCard
              value={stats.completedOrders}
              label="Completed Orders"
              icon="✓"
              variant="success"
            />
            <StatCard
              value={stats.lowStockItems}
              label="Low Stock Items"
              icon="⚠️"
              variant="danger"
              threshold={5}
              thresholdType="high"
              onClick={() => navigate('/control/inventory')}
            />
            <StatCard
              value={stats.totalInventoryItems}
              label="Total Inventory Items"
              icon="📋"
              variant="primary"
            />
          </div>
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
            emptyMessage="No recent orders found"
          />
        </Card>
      ),
    },
    {
      id: 'low-stock',
      label: 'Low Stock Alerts',
      badge: lowStockInventory.length,
      content: (
        <Card>
          {lowStockInventory.length > 0 && (
            <Alert variant="warning" title="Low Stock Alert" style={{ marginBottom: '1rem' }}>
              {lowStockInventory.length} items are running low on stock. Please review and restock.
            </Alert>
          )}
          <Table
            columns={inventoryColumns}
            data={lowStockInventory}
            striped
            hoverable
            emptyMessage="No low stock items"
          />
        </Card>
      ),
    },
  ];

  if (loading) {
    return <LoadingSpinner overlay text="Loading admin overview..." />;
  }

  return (
    <div className="overview-page">
      <div className="overview-header">
        <div>
          <h1>Admin Overview</h1>
          <p className="overview-subtitle">
            System-wide statistics and monitoring • Welcome, {user?.username}
          </p>
        </div>
        <Button 
          variant="primary" 
          onClick={fetchOverviewData}
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

      <Tabs tabs={tabs} variant="default" />
      <Footer />
    </div>
  );
}

export default AdminOverviewPage;
