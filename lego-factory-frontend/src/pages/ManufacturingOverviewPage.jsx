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
 * ManufacturingOverviewPage - Manufacturing Workstation Overview
 * 
 * Displays manufacturing-specific metrics including:
 * - Production orders status
 * - Assembly orders progress
 * - Workstation performance
 * - Module availability
 */
function ManufacturingOverviewPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stats, setStats] = useState({
    totalProductionOrders: 0,
    pendingProduction: 0,
    completedProduction: 0,
    totalAssemblyOrders: 0,
    pendingAssembly: 0,
    completedAssembly: 0,
  });
  const [recentProduction, setRecentProduction] = useState([]);
  const [recentAssembly, setRecentAssembly] = useState([]);

  useEffect(() => {
    fetchManufacturingData();
  }, []);

  const fetchManufacturingData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [productionOrders, assemblyOrders] = await Promise.all([
        api.get('/production-orders').catch(() => ({ data: [] })),
        api.get('/assembly-orders').catch(() => ({ data: [] })),
      ]);

      const productionData = Array.isArray(productionOrders.data) ? productionOrders.data : [];
      const assemblyData = Array.isArray(assemblyOrders.data) ? assemblyOrders.data : [];

      const prodPending = productionData.filter(o => o.status === 'PENDING' || o.status === 'PROCESSING').length;
      const prodCompleted = productionData.filter(o => o.status === 'COMPLETED').length;
      const assemblyPending = assemblyData.filter(o => o.status === 'PENDING' || o.status === 'PROCESSING').length;
      const assemblyCompleted = assemblyData.filter(o => o.status === 'COMPLETED').length;

      const recentProd = [...productionData]
        .sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
        .slice(0, 10);

      const recentAsm = [...assemblyData]
        .sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
        .slice(0, 10);

      setStats({
        totalProductionOrders: productionData.length,
        pendingProduction: prodPending,
        completedProduction: prodCompleted,
        totalAssemblyOrders: assemblyData.length,
        pendingAssembly: assemblyPending,
        completedAssembly: assemblyCompleted,
      });

      setRecentProduction(recentProd);
      setRecentAssembly(recentAsm);

    } catch (err) {
      console.error('Error fetching manufacturing data:', err);
      setError('Failed to load manufacturing overview data.');
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
              value={stats.totalProductionOrders}
              label="Production Orders"
              icon="🏭"
              variant="primary"
            />
            <StatCard
              value={stats.pendingProduction}
              label="Pending Production"
              icon="⏳"
              variant="warning"
            />
            <StatCard
              value={stats.completedProduction}
              label="Completed Production"
              icon="✓"
              variant="success"
            />
          </div>

          <div className="stats-row">
            <StatCard
              value={stats.totalAssemblyOrders}
              label="Assembly Orders"
              icon="🔧"
              variant="secondary"
            />
            <StatCard
              value={stats.pendingAssembly}
              label="Pending Assembly"
              icon="⏳"
              variant="warning"
            />
            <StatCard
              value={stats.completedAssembly}
              label="Completed Assembly"
              icon="✓"
              variant="success"
            />
          </div>
        </div>
      ),
    },
    {
      id: 'production',
      label: 'Production Orders',
      badge: recentProduction.length,
      content: (
        <Card>
          <Table
            columns={orderColumns}
            data={recentProduction}
            striped
            hoverable
            emptyMessage="No production orders"
          />
        </Card>
      ),
    },
    {
      id: 'assembly',
      label: 'Assembly Orders',
      badge: recentAssembly.length,
      content: (
        <Card>
          <Table
            columns={orderColumns}
            data={recentAssembly}
            striped
            hoverable
            emptyMessage="No assembly orders"
          />
        </Card>
      ),
    },
  ];

  if (loading) {
    return <LoadingSpinner overlay text="Loading manufacturing overview..." />;
  }

  return (
    <div className="overview-page">
      <div className="overview-header">
        <div>
          <h1>Manufacturing Overview</h1>
          <p className="overview-subtitle">
            Production and assembly operations • Welcome, {user?.username}
          </p>
        </div>
        <Button 
          variant="primary" 
          onClick={fetchManufacturingData}
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
      <Footer />
    </div>
  );
}

export default ManufacturingOverviewPage;
