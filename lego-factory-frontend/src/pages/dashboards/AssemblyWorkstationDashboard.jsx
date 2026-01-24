import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { 
  StandardDashboardLayout,
  OrdersSection,
  ActivityLog,
  StatisticsGrid,
  AssemblyControlOrderCard,
  FinalAssemblyOrderCard,
  Button, 
  Card, 
  Badge 
} from "../../components";
import "../../styles/DashboardLayout.css";

function AssemblyWorkstationDashboard() {
  const { session } = useAuth();
  const [assemblyOrders, setAssemblyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'ASSY-WS'
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

  // Get workstation-specific order endpoint
  const getOrderEndpoint = () => {
    const workstationId = session?.user?.workstation?.id;
    if (workstationId === 4) return '/gear-assembly-orders';
    if (workstationId === 5) return '/motor-assembly-orders';
    if (workstationId === 6) return '/final-assembly-orders';
    return `/api/assembly/${getAssemblyType()}`; // Fallback to old generic endpoint
  };

  const assemblyType = getAssemblyType();
  const apiEndpoint = getOrderEndpoint();

  useEffect(() => {
    if (session?.user?.workstationId) {
      fetchAssemblyOrders();
      const interval = setInterval(fetchAssemblyOrders, 30000); // Increased to 30s to reduce page jump
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  const fetchAssemblyOrders = async () => {
    if (!session?.user?.workstation?.id) {
      setAssemblyOrders([]);
      return;
    }
    
    try {
      setLoading(true);
      const response = await api.get(`${apiEndpoint}/workstation/${session.user.workstation.id}`);
      const ordersList = Array.isArray(response.data) ? response.data : [];
      setAssemblyOrders(ordersList);
      setError(null);
    } catch (err) {
      if (err.response?.status === 404) {
        setAssemblyOrders([]);
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
      const endpoint = getOrderEndpoint();
      await api.post(`${endpoint}/${orderId}/start`);
      const message = `Assembly task started successfully!`;
      setSuccess(message);
      addNotification({ message, type: 'success' });
      fetchAssemblyOrders();
    } catch (err) {
      const errorMsg = "Failed to start assembly: " + (err.response?.data?.message || err.message);
      setError(errorMsg);
      addNotification({ message: errorMsg, type: 'error' });
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCompleteAssembly = async (orderId) => {
    if (!window.confirm("Are you sure the assembly task is complete and quality checks have passed?")) return;
    
    setProcessingOrderId(orderId);
    setError(null);

    try {
      const isFinalAssembly = assemblyType === "final-assembly";
      const endpoint = getOrderEndpoint();
      await api.post(`${endpoint}/${orderId}/complete`);
      
      const message = isFinalAssembly 
        ? "Assembly completed! Product credited to Plant Warehouse." 
        : "Assembly completed! Module credited to Modules Supermarket.";
      setSuccess(message);
      addNotification({ message, type: 'success' });
      fetchAssemblyOrders();
    } catch (err) {
      const errorMsg = "Failed to complete assembly: " + (err.response?.data?.message || err.message);
      setError(errorMsg);
      addNotification({ message: errorMsg, type: 'error' });
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleHaltAssembly = async (orderId) => {
    const reason = prompt("Enter reason for halting assembly:");
    if (!reason) return;

    setProcessingOrderId(orderId);
    setError(null);

    try {
      const endpoint = getOrderEndpoint();
      await api.post(`${endpoint}/${orderId}/halt`, { reason });
      setSuccess("Assembly task halted and logged");
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to halt assembly: " + (err.response?.data?.message || err.message));
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  const getStationTitle = () => {
    const titles = {
      'gear-assembly': '⚙️ Gear Assembly',
      'motor-assembly': '🔌 Motor Assembly',
      'final-assembly': '📦 Final Assembly'
    };
    return titles[assemblyType] || 'Assembly Workstation';
  };

  // Stats cards
  // Stats data for StatisticsGrid
  const statsData = (() => {
    const total = assemblyOrders.length;
    const assigned = assemblyOrders.filter(o => o.status === "ASSIGNED").length;
    const inProgress = assemblyOrders.filter(o => o.status === "IN_PROGRESS").length;
    const completed = assemblyOrders.filter(o => o.status === "COMPLETED").length;
    const pending = assemblyOrders.filter(o => o.status === "PENDING").length;
    const rejected = assemblyOrders.filter(o => o.status === "REJECTED").length;

    return [
      { value: total, label: 'Total Tasks', variant: 'default', icon: '📦' },
      { value: pending, label: 'Pending', variant: 'pending', icon: '⏳' },
      { value: assigned, label: 'Assigned', variant: 'info', icon: '📝' },
      { value: inProgress, label: 'In Progress', variant: 'warning', icon: '⚙️' },
      { value: completed, label: 'Completed', variant: 'success', icon: '✅' },
      { value: rejected, label: 'Rejected', variant: 'danger', icon: '❌' },
      { value: 0, label: 'On Hold', variant: 'default', icon: '⏸️' },
      { value: 0, label: 'Delayed', variant: 'warning', icon: '⏱️' },
    ];
  })();

  // Render functions for StandardDashboardLayout
  const renderActivity = () => (
    <ActivityLog notifications={notifications} onClear={clearNotifications} />
  );

  const renderAssemblyOrders = () => {
    const isFinalAssembly = assemblyType === 'final-assembly';
    
    return (
      <OrdersSection
        title={isFinalAssembly ? "Final Assembly Orders" : "Assembly Tasks"}
        orders={assemblyOrders}
        loading={loading}
        filterOptions={[
          { value: 'ALL', label: 'All Tasks' },
          { value: 'PENDING', label: 'Pending' },
          { value: 'CONFIRMED', label: 'Confirmed' },
          { value: 'IN_PROGRESS', label: 'In Progress' },
          { value: 'COMPLETED_ASSEMBLY', label: 'Completed Assembly' },
          { value: 'COMPLETED', label: 'Completed' },
          { value: 'HALTED', label: 'Halted' }
        ]}
        sortOptions={[
          { value: 'orderNumber', label: 'Order Number' },
          { value: 'status', label: 'Status' },
          { value: 'priority', label: 'Priority' }
        ]}
        searchKeys={isFinalAssembly ? ['orderNumber', 'outputProductVariantName'] : ['controlOrderNumber', 'assemblyInstructions']}
        sortKey={isFinalAssembly ? 'orderNumber' : 'controlOrderNumber'}
        emptyMessage="Assembly tasks will appear here when assigned to your workstation"
        onRefresh={fetchAssemblyOrders}
        renderCard={(order) => {
          if (isFinalAssembly) {
            return (
              <FinalAssemblyOrderCard
                key={order.id}
                order={order}
                onRefresh={fetchAssemblyOrders}
              />
            );
          }
          
          return (
            <AssemblyControlOrderCard
              key={order.id}
              order={order}
              onStart={() => handleStartAssembly(order.id)}
              onComplete={() => handleCompleteAssembly(order.id)}
              onHalt={() => handleHaltAssembly(order.id)}
              onViewDetails={() => handleViewDetails(order)}
              processing={processingOrderId === order.id}
            />
          );
        }}
      />
    );
  };

  // Details modal
  const renderDetailsModal = () => {
    if (!showDetailsModal || !selectedOrder) return null;

    return (
      <div className="modal">
        <div className="modal-overlay" onClick={() => setShowDetailsModal(false)} />
        <div className="modal-content">
          <div className="modal-header">
            <h2>Assembly Task: {selectedOrder.controlOrderNumber || `#${selectedOrder.id}`}</h2>
            <button onClick={() => setShowDetailsModal(false)} className="modal-close">×</button>
          </div>
          <div className="modal-body">
            <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "1rem" }}>
              <div><strong>Status:</strong> <Badge variant={
                selectedOrder.status === "COMPLETED" ? "success" :
                selectedOrder.status === "IN_PROGRESS" ? "warning" :
                "default"
              }>{selectedOrder.status}</Badge></div>
              <div><strong>Priority:</strong> {selectedOrder.priority || "MEDIUM"}</div>
              <div><strong>Production Order:</strong> #{selectedOrder.sourceProductionOrderId}</div>
              <div><strong>SimAL Schedule ID:</strong> {selectedOrder.simalScheduleId}</div>
              <div><strong>Target Start:</strong> {new Date(selectedOrder.targetStartTime).toLocaleString()}</div>
              <div><strong>Target Completion:</strong> {new Date(selectedOrder.targetCompletionTime).toLocaleString()}</div>
              {selectedOrder.actualStartTime && (
                <div><strong>Actual Start:</strong> {new Date(selectedOrder.actualStartTime).toLocaleString()}</div>
              )}
              {selectedOrder.actualCompletionTime && (
                <div><strong>Actual Completion:</strong> {new Date(selectedOrder.actualCompletionTime).toLocaleString()}</div>
              )}
              {selectedOrder.actualDurationMinutes && (
                <div><strong>Duration:</strong> {selectedOrder.actualDurationMinutes} minutes</div>
              )}
            </div>
            {selectedOrder.assemblyInstructions && (
              <div style={{ marginTop: "1rem" }}>
                <strong>Assembly Instructions:</strong>
                <p style={{ marginTop: "0.5rem", padding: "0.75rem", backgroundColor: "#f3f4f6", borderRadius: "0.375rem" }}>
                  {selectedOrder.assemblyInstructions}
                </p>
              </div>
            )}
            {selectedOrder.qualityCheckpoints && (
              <div style={{ marginTop: "1rem" }}>
                <strong>Quality Checkpoints:</strong>
                <p style={{ marginTop: "0.5rem", padding: "0.75rem", backgroundColor: "#f3f4f6", borderRadius: "0.375rem" }}>
                  {selectedOrder.qualityCheckpoints}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <StandardDashboardLayout
      title={getStationTitle()}
      subtitle={`Execute assembly tasks for ${session?.user?.workstation?.name || 'workstation'}`}
      icon="🔩"
      activityContent={renderActivity()}
      statsContent={<StatisticsGrid stats={statsData} />}
      formContent={null}
      contentGrid={renderAssemblyOrders()}
      inventoryContent={null}
      messages={{ error, success }}
      onDismissError={() => setError(null)}
      onDismissSuccess={() => setSuccess(null)}
    >
      {renderDetailsModal()}
    </StandardDashboardLayout>
  );
}

export default AssemblyWorkstationDashboard;
