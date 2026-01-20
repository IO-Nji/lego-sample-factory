import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { DashboardLayout, Notification, ProductionControlOrderCard, StatCard } from "../../components";
import "../../styles/DashboardLayout.css";

function ManufacturingDashboard() {
  const { session } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [controlOrders, setControlOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filterStatus, setFilterStatus] = useState("ALL");

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'MANFCT'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  // Fetch production control orders for this manufacturing workstation
  const fetchControlOrders = async () => {
    const workstationId = session?.user?.workstation?.id;
    if (!workstationId) {
      setControlOrders([]);
      setFilteredOrders([]);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await api.get(`/production-control-orders/workstation/${workstationId}`);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setControlOrders(ordersList);
      applyFilter(ordersList, filterStatus);
      if (ordersList.length > 0) {
        addNotification(`Loaded ${ordersList.length} production orders`, 'success');
      }
    } catch (err) {
      const errorMsg = "Failed to load production orders: " + (err.response?.data?.message || err.message);
      setError(errorMsg);
      addNotification(errorMsg, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (session?.user?.workstation?.id) {
      fetchControlOrders();
      const interval = setInterval(fetchControlOrders, 30000); // Refresh every 30 seconds
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstation?.id]);

  useEffect(() => {
    applyFilter(controlOrders, filterStatus);
  }, [filterStatus, controlOrders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const handleStartProduction = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/start`);
      addNotification(`Production order started`, 'success');
      fetchControlOrders();
    } catch (err) {
      addNotification(`Failed to start production: ${err.response?.data?.message || err.message}`, 'error');
    }
  };

  const handleCompleteProduction = async (orderId) => {
    try {
      await api.post(`/production-control-orders/${orderId}/complete`);
      addNotification(`Production order completed`, 'success');
      fetchControlOrders();
    } catch (err) {
      addNotification(`Failed to complete production: ${err.response?.data?.message || err.message}`, 'error');
    }
  };

  const statsCards = (
    <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
      <StatCard
        value={controlOrders.length}
        label="Total Orders"
        variant="primary"
      />
      <StatCard
        value={controlOrders.filter(o => o.status === 'ASSIGNED').length}
        label="Assigned"
        variant="warning"
      />
      <StatCard
        value={controlOrders.filter(o => o.status === 'IN_PROGRESS').length}
        label="In Progress"
        variant="info"
      />
      <StatCard
        value={controlOrders.filter(o => o.status === 'COMPLETED').length}
        label="Completed"
        variant="success"
      />
    </div>
  );

  const primaryContent = (
    <div className="dashboard-box-content">
      <div style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
        <label htmlFor="filterStatus" style={{ fontWeight: 500 }}>Filter by Status:</label>
        <select 
          id="filterStatus"
          value={filterStatus} 
          onChange={(e) => setFilterStatus(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '4px', border: '1px solid #ccc' }}
        >
          <option value="ALL">All Orders</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
        </select>
      </div>
      
      {loading && <p>Loading production orders...</p>}
      {error && <div style={{ color: 'red', padding: '1rem', background: '#fee', borderRadius: '4px' }}>{error}</div>}
      
      {!loading && !error && filteredOrders.length === 0 && (
        <div className="dashboard-empty-state">
          <p className="dashboard-empty-state-title">No Production Orders</p>
          <p className="dashboard-empty-state-text">
            {filterStatus === "ALL" 
              ? "No production orders assigned to this workstation yet" 
              : `No orders with status: ${filterStatus}`}
          </p>
        </div>
      )}
    </div>
  );

  const ordersSection = (
    <div className="orders-grid">
      {filteredOrders.map(order => (
        <ProductionControlOrderCard
          key={order.id}
          order={order}
          onStart={() => handleStartProduction(order.id)}
          onComplete={() => handleCompleteProduction(order.id)}
        />
      ))}
    </div>
  );

  return (
    <DashboardLayout
      title="Manufacturing Workstation"
      subtitle={`Production orders for ${session?.user?.workstation?.name || 'Manufacturing'}`}
      icon="🔧"
      layout="default"
      statsCards={statsCards}
      primaryContent={primaryContent}
      secondaryContent={
        <Notification 
          notifications={notifications}
          title="Manufacturing Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={ordersSection}
    />
  );
}

export default ManufacturingDashboard;
