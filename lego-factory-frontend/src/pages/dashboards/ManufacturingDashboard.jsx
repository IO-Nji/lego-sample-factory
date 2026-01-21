import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  ProductionControlOrderCard 
} from "../../components";
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

  const statsData = [
    { value: controlOrders.length, label: 'Total Orders', variant: 'default', icon: '📦' },
    { value: controlOrders.filter(o => o.status === 'ASSIGNED').length, label: 'Assigned', variant: 'warning', icon: '📝' },
    { value: controlOrders.filter(o => o.status === 'IN_PROGRESS').length, label: 'In Progress', variant: 'info', icon: '⚙️' },
    { value: controlOrders.filter(o => o.status === 'COMPLETED').length, label: 'Completed', variant: 'success', icon: '✅' },
  ];

  // Activity log rendering
  const renderActivity = () => (
    <ActivityLog 
      notifications={notifications}
      onClear={clearNotifications}
    />
  );

  // Production orders rendering using OrdersSection
  const renderProductionOrders = () => (
    <OrdersSection
      title="Production Orders"
      icon="🔧"
      orders={controlOrders}
      filterOptions={[
        { value: 'ALL', label: 'All Orders' },
        { value: 'ASSIGNED', label: 'Assigned' },
        { value: 'IN_PROGRESS', label: 'In Progress' },
        { value: 'COMPLETED', label: 'Completed' }
      ]}
      sortOptions={[
        { value: 'orderNumber', label: 'Order Number' },
        { value: 'status', label: 'Status' }
      ]}
      searchKeys={['controlOrderNumber']}
      sortKey="controlOrderNumber"
      renderCard={(order) => (
        <ProductionControlOrderCard
          key={order.id}
          order={order}
          onStart={() => handleStartProduction(order.id)}
          onComplete={() => handleCompleteProduction(order.id)}
        />
      )}
      emptyMessage="No production orders assigned to this workstation yet"
      searchPlaceholder="Search by order number..."
    />
  );

  return (
    <StandardDashboardLayout
      title="Manufacturing Workstation"
      subtitle={`Production orders for ${session?.user?.workstation?.name || 'Manufacturing'}`}
      icon="🔧"
      activityContent={renderActivity()}
      statsContent={<StatisticsGrid stats={statsData} />}
      formContent={null}
      contentGrid={renderProductionOrders()}
      inventoryContent={null}
      messages={{ error, success: null }}
      onDismissError={() => setError(null)}
      onDismissSuccess={() => {}}
    />
  );
}

export default ManufacturingDashboard;
