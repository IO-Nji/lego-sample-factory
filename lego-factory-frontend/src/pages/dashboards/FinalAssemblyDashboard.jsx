import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout, StatsCard, Notification } from "../../components";
import FinalAssemblyOrderCard from "../../components/FinalAssemblyOrderCard";
import { getProductDisplayName } from "../../utils/dashboardHelpers";

function FinalAssemblyDashboard() {
  const { session } = useAuth();
  const [finalAssemblyOrders, setFinalAssemblyOrders] = useState([]);
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
      station: session?.user?.workstation?.name || 'FINAL-ASSY'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchFinalAssemblyOrders();
    }
    const interval = setInterval(() => {
      if (session?.user?.workstationId) {
        fetchFinalAssemblyOrders();
      }
    }, 30000); // Refresh every 30 seconds
    return () => clearInterval(interval);
  }, [session?.user?.workstationId]);

  useEffect(() => {
    applyFilter(finalAssemblyOrders, filterStatus);
  }, [filterStatus, finalAssemblyOrders]);

  const applyFilter = (ordersList, status) => {
    if (status === "ALL") {
      setFilteredOrders(ordersList);
    } else {
      setFilteredOrders(ordersList.filter(order => order.status === status));
    }
  };

  const fetchFinalAssemblyOrders = async () => {
    try {
      const workstationId = session?.user?.workstationId || 6; // WS-6 = Final Assembly
      const response = await axios.get(`/api/final-assembly-orders/workstation/${workstationId}`);
      const data = response.data;
      console.log('[FinalAssemblyOrders] Fetched data:', JSON.stringify(data, null, 2));
      if (Array.isArray(data)) {
        setFinalAssemblyOrders(data);
        applyFilter(data, filterStatus);
        setError(null);
      } else {
        setFinalAssemblyOrders([]);
        setFilteredOrders([]);
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setFinalAssemblyOrders([]);
        setFilteredOrders([]);
        setError(null);
      } else {
        setError("Failed to load final assembly orders: " + (err.response?.data?.message || err.message));
      }
    }
  };

  const handleStartOrder = async (orderId, orderNumber) => {
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await axios.post(`/api/final-assembly-orders/${orderId}/start`);
      addNotification(`Order ${orderNumber} started - assembly in progress`, 'success');
      await fetchFinalAssemblyOrders();
    } catch (err) {
      setError("Failed to start order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to start order", 'error');
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCompleteOrder = async (orderId, orderNumber) => {
    if (!globalThis.confirm("Complete this assembly? This will credit the Plant Warehouse with finished products.")) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    try {
      await axios.post(`/api/final-assembly-orders/${orderId}/complete`);
      addNotification(`Order ${orderNumber} completed - Plant Warehouse credited`, 'success');
      await fetchFinalAssemblyOrders();
    } catch (err) {
      setError("Failed to complete order: " + (err.response?.data?.message || err.message));
      addNotification("Failed to complete order", 'error');
    } finally {
      setProcessingOrderId(null);
    }
  };

  // Render Stats Cards
  const renderStatsCards = () => (
    <>
      <StatsCard 
        value={finalAssemblyOrders.length} 
        label="Total Orders" 
        variant="default"
      />
      <StatsCard 
        value={finalAssemblyOrders.filter(o => o.status === "PENDING").length} 
        label="Pending" 
        variant="pending"
      />
      <StatsCard 
        value={finalAssemblyOrders.filter(o => o.status === "IN_PROGRESS").length} 
        label="In Progress" 
        variant="processing"
      />
      <StatsCard 
        value={finalAssemblyOrders.filter(o => o.status === "COMPLETED").length} 
        label="Completed" 
        variant="completed"
      />
    </>
  );

  // Render Final Assembly Orders Section
  const renderFinalAssemblyOrdersSection = () => (
    <>
      <div className="dashboard-box-header dashboard-box-header-purple">
        <h2 className="dashboard-box-header-title">🔧 Final Assembly Orders</h2>
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
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button 
            onClick={fetchFinalAssemblyOrders} 
            disabled={loading} 
            className="dashboard-box-header-action"
          >
            {loading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>
      <div className="dashboard-box-content">
        {Array.isArray(filteredOrders) && filteredOrders.length > 0 ? (
          <div className="dashboard-orders-grid">
            {filteredOrders.map((order) => (
              <FinalAssemblyOrderCard
                key={order.id}
                order={order}
                onStart={handleStartOrder}
                onComplete={handleCompleteOrder}
                isProcessing={processingOrderId === order.id}
                getProductDisplayName={getProductDisplayName}
              />
            ))}
          </div>
        ) : (
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">No final assembly orders found</p>
            <p className="dashboard-empty-state-text">
              {filterStatus !== "ALL" 
                ? `No orders with status: ${filterStatus}` 
                : "Orders will appear here when created from warehouse orders"}
            </p>
          </div>
        )}
      </div>
    </>
  );

  // Render Info Box
  const renderInfoBox = () => (
    <div className="dashboard-info-box">
      <h3 style={{ fontWeight: 'bold', fontSize: '1rem', marginBottom: '1rem', color: '#5b21b6' }}>
        About Final Assembly (WS-6)
      </h3>
      <ul style={{ marginLeft: '1.5rem', fontSize: '0.875rem', lineHeight: '1.75', color: '#5b21b6' }}>
        <li><strong>SCENARIO 2:</strong> Assemble finished products from modules</li>
        <li><strong>Start Order:</strong> Begin assembly work (PENDING → IN_PROGRESS)</li>
        <li><strong>Complete Order:</strong> Finish assembly and credit Plant Warehouse (WS-7) with products</li>
        <li><strong>Order Flow:</strong> CustomerOrder → WarehouseOrder → FinalAssemblyOrder</li>
        <li><strong>Critical:</strong> Completion credits the Plant Warehouse, allowing customer orders to be fulfilled</li>
        <li>Orders are automatically fetched every 30 seconds for real-time updates</li>
      </ul>
    </div>
  );

  return (
    <>
      <DashboardLayout
        title="Final Assembly Dashboard"
        subtitle={`Assemble finished products from modules${session?.user?.workstationId ? ` | Workstation ID: ${session.user.workstationId}` : ''}`}
        icon="🔧"
        layout="compact"
        statsCards={renderStatsCards()}
        notifications={
          <Notification 
            notifications={notifications}
            title="Assembly Activity"
            maxVisible={5}
            onClear={clearNotifications}
          />
        }
        ordersSection={renderFinalAssemblyOrdersSection()}
        infoBox={renderInfoBox()}
      />
    </>
  );
}

export default FinalAssemblyDashboard;
