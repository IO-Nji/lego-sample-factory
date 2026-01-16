import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout, Button, Notification } from "../../components";
import "../../styles/DashboardLayout.css";

function AssemblyWorkstationDashboard() {
  const { session } = useAuth();
  const [assemblyOrders, setAssemblyOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: session?.user?.workstation?.name || 'ASSM-CN'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  // Determine assembly type from workstation name
  const getAssemblyType = () => {
    const workstationName = session?.user?.workstationName || "";
    if (workstationName.includes("Gear")) return "gear-assembly";
    if (workstationName.includes("Motor")) return "motor-assembly";
    if (workstationName.includes("Final")) return "final-assembly";
    return "final-assembly"; // default
  };

  const assemblyType = getAssemblyType();
  const apiEndpoint = `/api/assembly/${assemblyType}`;

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchAssemblyOrders();
      const interval = setInterval(fetchAssemblyOrders, 30000); // Increased to 30s to reduce page jump
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  useEffect(() => {
    applyFilter(assemblyOrders, filterStatus);
  }, [filterStatus, assemblyOrders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const fetchAssemblyOrders = async () => {
    if (!session?.user?.workstationId) {
      setAssemblyOrders([]);
      setFilteredOrders([]);
      return;
    }
    
    try {
      setLoading(true);
      const response = await axios.get(`${apiEndpoint}/workstation/${session.user.workstationId}`);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setAssemblyOrders(ordersList);
      applyFilter(ordersList, filterStatus);
      setError(null);
    } catch (err) {
      if (err.response?.status === 404) {
        setAssemblyOrders([]);
        setFilteredOrders([]);
      } else {
        setError("Failed to load assembly orders: " + (err.response?.data?.message || err.message));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleStartAssembly = async (orderId) => {
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await axios.put(`${apiEndpoint}/${orderId}/start`);
      addNotification(`Assembly task ${orderId} started`, 'success');
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to start assembly: " + (err.response?.data?.message || err.message));
      addNotification("Failed to start assembly", 'error');
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCompleteAssembly = async (orderId) => {
    setProcessingOrderId(orderId);
    setError(null);

    try {
      const isFinalAssembly = assemblyType === "final-assembly";
      const response = await axios.put(`${apiEndpoint}/${orderId}/complete`);
      
      addNotification(
        isFinalAssembly 
          ? `Assembly ${orderId} completed - Product credited` 
          : `Assembly ${orderId} completed - Module credited`, 
        'success'
      );
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to complete assembly: " + (err.response?.data?.message || err.message));
    } finally {
      setProcessingOrderId(null);
    }
  };

  const getStatusBadgeClass = (status) => {
    const statusMap = {
      'ASSIGNED': 'pending',
      'IN_PROGRESS': 'processing',
      'COMPLETED': 'completed',
      'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'default';
  };

  const getStationTitle = () => {
    const titles = {
      'gear-assembly': '⚙️ Gear Assembly',
      'motor-assembly': '🔌 Motor Assembly',
      'final-assembly': '📦 Final Assembly'
    };
    return titles[assemblyType] || 'Assembly Workstation';
  };

  const renderAssemblyOrders = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-green">
        <h2 className="dashboard-box-header-title">🔩 Assembly Tasks</h2>
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          <select 
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            style={{ 
              padding: "0.5rem", 
              borderRadius: "0.375rem", 
              border: "1px solid #d1d5db",
              fontSize: "0.875rem"
            }}
          >
            <option value="ALL">All Orders</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
            <option value="HALTED">Halted</option>
            <option value="ABANDONED">Abandoned</option>
          </select>
          <button 
            onClick={fetchAssemblyOrders} 
            disabled={loading} 
            className="dashboard-box-header-action"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>
      <div className="dashboard-box-content">
        {loading ? (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-text">Loading assembly tasks...</p>
          </div>
        ) : filteredOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <div key={order.id} className="order-card">
                <div className="order-card-header">
                  <span className="order-card-number">#{order.controlOrderNumber || order.id}</span>
                  <span className={`order-card-status status-${getStatusBadgeClass(order.status)}`}>
                    {order.status}
                  </span>
                </div>
                <div className="order-card-body">
                  <div className="order-card-detail">
                    <span className="order-card-label">Priority:</span>
                    <span className="order-card-value">{order.priority || 'NORMAL'}</span>
                  </div>
                  {order.targetCompletionTime && (
                    <div className="order-card-detail">
                      <span className="order-card-label">Target:</span>
                      <span className="order-card-value">
                        {new Date(order.targetCompletionTime).toLocaleString()}
                      </span>
                    </div>
                  )}
                </div>
                <div className="order-card-actions">
                  {order.status === 'ASSIGNED' && (
                    <Button 
                      variant="primary" 
                      size="small"
                      onClick={() => handleStartAssembly(order.id)}
                      disabled={processingOrderId === order.id}
                    >
                      {processingOrderId === order.id ? 'Starting...' : 'Start'}
                    </Button>
                  )}
                  {order.status === 'IN_PROGRESS' && (
                    <Button 
                      variant="success" 
                      size="small"
                      onClick={() => handleCompleteAssembly(order.id)}
                      disabled={processingOrderId === order.id}
                    >
                      {processingOrderId === order.id ? 'Completing...' : 'Complete'}
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No assembly tasks found</p>
            <p className="dashboard-empty-state-text">
              {filterStatus !== "ALL" 
                ? `No orders with status: ${filterStatus}` 
                : "Orders will appear here when assigned to your workstation"}
            </p>
          </div>
        )}
      </div>
    </>
  );

  return (
    <DashboardLayout
      title={getStationTitle()}
      subtitle={`Workstation ${session?.user?.workstationId || 'N/A'} - Process assembly tasks`}
      icon="🔩"
      layout="default"
      secondaryContent={
        <Notification 
          notifications={notifications}
          title="Assembly Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={renderAssemblyOrders()}
    />
  );
}

export default AssemblyWorkstationDashboard;
