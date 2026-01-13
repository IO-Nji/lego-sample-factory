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
  Button 
} from '../components';
import '../styles/OverviewPages.css';

/**
 * WarehouseOverviewPage - Plant Warehouse Overview
 * 
 * Displays warehouse-specific metrics including:
 * - Warehouse orders (pending, processing, completed)
 * - Inventory status and alerts
 * - Supply orders status
 * - Stock levels and restock needs
 */
function WarehouseOverviewPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    totalWarehouseOrders: 0,
    pendingWarehouseOrders: 0,
    completedWarehouseOrders: 0,
    totalSupplyOrders: 0,
    lowStockItems: 0,
    totalInventoryItems: 0,
  });
  const [recentOrders, setRecentOrders] = useState([]);
  const [lowStockInventory, setLowStockInventory] = useState([]);

  useEffect(() => {
    fetchWarehouseData();
  }, []);

  const fetchWarehouseData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [warehouseOrders, supplyOrders, inventory] = await Promise.all([
        api.get('/warehouse-orders').catch(() => ({ data: [] })),
        api.get('/supply-orders').catch(() => ({ data: [] })),
        api.get('/inventory').catch(() => ({ data: [] })),
      ]);

      const warehouseOrdersData = Array.isArray(warehouseOrders.data) ? warehouseOrders.data : [];
      const supplyOrdersData = Array.isArray(supplyOrders.data) ? supplyOrders.data : [];
      const inventoryData = Array.isArray(inventory.data) ? inventory.data : [];

      const pendingCount = warehouseOrdersData.filter(o => o.status === 'PENDING' || o.status === 'CONFIRMED').length;
      const completedCount = warehouseOrdersData.filter(o => o.status === 'COMPLETED').length;
      const lowStock = inventoryData.filter(item => item.quantity <= 10);

      const recent = [...warehouseOrdersData]
        .sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
        .slice(0, 10);

      setStats({
        totalWarehouseOrders: warehouseOrdersData.length,
        pendingWarehouseOrders: pendingCount,
        completedWarehouseOrders: completedCount,
        totalSupplyOrders: supplyOrdersData.length,
        lowStockItems: lowStock.length,
        totalInventoryItems: inventoryData.length,
      });

      setRecentOrders(recent);
      setLowStockInventory(lowStock);

    } catch (err) {
      console.error('Error fetching warehouse data:', err);
      setError('Failed to load warehouse overview data.');
    } finally {
      setLoading(false);
    }
  };

  const orderColumns = [
    { key: 'orderNumber', label: 'Order #', sortable: true },
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

  const inventoryColumns = [
    { key: 'itemId', label: 'Item ID', sortable: true },
    { key: 'itemName', label: 'Item Name', sortable: true },
    { 
      key: 'quantity', 
      label: 'Quantity', 
      sortable: true,
      render: (row) => (
        <span style={{ 
          color: row.quantity <= 5 ? 'var(--color-danger-600)' : 'var(--color-warning-600)',
          fontWeight: 'var(--font-weight-semibold)'
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
              value={stats.totalWarehouseOrders}
              label="WAREHOUSE"
              icon="📦"
              variant="primary"
            />
            <StatCard
              value={stats.pendingWarehouseOrders}
              label="PENDING"
              icon="⏳"
              variant="warning"
              threshold={10}
              thresholdType="high"
            />
            <StatCard
              value={stats.completedWarehouseOrders}
              label="COMPLETED"
              icon="✓"
              variant="success"
            />
            <StatCard
              value={stats.totalSupplyOrders}
              label="SUPPLY"
              icon="🚚"
              variant="info"
            />
          </div>

          <div className="stats-row">
            <StatCard
              value={stats.lowStockItems}
              label="Low Stock Items"
              icon="⚠️"
              variant="danger"
              threshold={5}
              thresholdType="high"
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
            emptyMessage="No recent warehouse orders"
          />
        </Card>
      ),
    },
    {
      id: 'low-stock',
      label: 'Low Stock',
      badge: lowStockInventory.length,
      content: (
        <Card>
          {lowStockInventory.length > 0 && (
            <Alert variant="warning" title="Low Stock Alert" style={{ marginBottom: '1rem' }}>
              {lowStockInventory.length} items need restocking
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
    return <LoadingSpinner overlay text="Loading warehouse overview..." />;
  }

  return (
    <div className="overview-page">
      <div className="overview-header">
        <div>
          <h1>Warehouse Overview</h1>
          <p className="overview-subtitle">
            Warehouse operations and inventory status • Welcome, {user?.username}
          </p>
        </div>
        <Button 
          variant="primary" 
          onClick={fetchWarehouseData}
          icon="🔄"
        >
          Refresh
        </Button>
      </div>

      {error && (
        <Alert variant="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Tabs tabs={tabs} variant="default" />
    </div>
  );
}

export default WarehouseOverviewPage;
