import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/api";
import { DashboardLayout, StatCard, Button, Card, Badge } from "../../components";
import "../../styles/DashboardLayout.css";

function AssemblyWorkstationDashboard() {
  const { session } = useAuth();
  const [assemblyOrders, setAssemblyOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);

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
    if (!session?.user?.workstation?.id) {
      setAssemblyOrders([]);
      setFilteredOrders([]);
      return;
    }
    
    try {
      setLoading(true);
      const response = await api.get(`${apiEndpoint}/workstation/${session.user.workstation.id}`);
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
      await api.put(`${apiEndpoint}/${orderId}/start`);
      setSuccess(`Assembly task started successfully!`);
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to start assembly: " + (err.response?.data?.message || err.message));
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
      await api.put(`${apiEndpoint}/${orderId}/complete`);
      
      setSuccess(
        isFinalAssembly 
          ? "Assembly completed! Product credited to Plant Warehouse." 
          : "Assembly completed! Module credited to Modules Supermarket."
      );
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to complete assembly: " + (err.response?.data?.message || err.message));
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
      await api.post(`${apiEndpoint}/${orderId}/halt`, { reason });
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
  const renderStatsCards = () => {
    const total = assemblyOrders.length;
    const assigned = assemblyOrders.filter(o => o.status === "ASSIGNED").length;
    const inProgress = assemblyOrders.filter(o => o.status === "IN_PROGRESS").length;
    const completed = assemblyOrders.filter(o => o.status === "COMPLETED").length;

    return (
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "1rem", marginBottom: "1.5rem" }}>
        <StatCard value={total} label="Total Tasks" variant="primary" />
        <StatCard value={assigned} label="Assigned" variant="info" />
        <StatCard value={inProgress} label="In Progress" variant="warning" />
        <StatCard value={completed} label="Completed" variant="success" />
      </div>
    );
  };

  // Filter controls
  const renderFilterControls = () => (
    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
      <select 
        value={filterStatus}
        onChange={(e) => setFilterStatus(e.target.value)}
        style={{ padding: "0.5rem", borderRadius: "0.375rem", border: "1px solid #d1d5db", fontSize: "0.875rem" }}
      >
        <option value="ALL">All Tasks</option>
        <option value="ASSIGNED">Assigned</option>
        <option value="IN_PROGRESS">In Progress</option>
        <option value="COMPLETED">Completed</option>
        <option value="HALTED">Halted</option>
      </select>
      <Button 
        variant="secondary"
        size="small"
        onClick={fetchAssemblyOrders} 
        disabled={loading}
      >
        {loading ? "Refreshing..." : "Refresh"}
      </Button>
    </div>
  );

  // Assembly orders display
  const renderAssemblyOrders = () => {
    if (loading && assemblyOrders.length === 0) {
      return <div className="dashboard-empty-state">Loading assembly tasks...</div>;
    }

    if (filteredOrders.length === 0) {
      return (
        <div className="dashboard-empty-state">
          <p className="dashboard-empty-state-title">No assembly tasks</p>
          <p className="dashboard-empty-state-text">
            {filterStatus !== "ALL" 
              ? `No tasks with status: ${filterStatus}` 
              : "Assembly tasks will appear here when assigned to your workstation"}
          </p>
        </div>
      );
    }

    return (
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1rem" }}>
        {filteredOrders.map((order) => (
          <Card key={order.id} style={{ padding: "1rem" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "0.75rem" }}>
              <div>
                <h3 style={{ fontSize: "1.125rem", fontWeight: "600", margin: "0 0 0.25rem 0" }}>
                  {order.controlOrderNumber || `Task #${order.id}`}
                </h3>
                <p style={{ fontSize: "0.875rem", color: "#6b7280", margin: 0 }}>
                  Production Order #{order.sourceProductionOrderId}
                </p>
              </div>
              <Badge 
                variant={
                  order.status === "COMPLETED" ? "success" :
                  order.status === "IN_PROGRESS" ? "warning" :
                  order.status === "HALTED" ? "danger" :
                  "default"
                }
              >
                {order.status}
              </Badge>
            </div>
            
            <div style={{ marginBottom: "0.75rem", fontSize: "0.875rem" }}>
              <div style={{ marginBottom: "0.25rem" }}>
                <span style={{ fontWeight: "500" }}>Priority:</span> {order.priority || "MEDIUM"}
              </div>
              {order.targetCompletionTime && (
                <div style={{ marginBottom: "0.25rem" }}>
                  <span style={{ fontWeight: "500" }}>Target:</span> {new Date(order.targetCompletionTime).toLocaleString()}
                </div>
              )}
            </div>
            
            <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
              {order.status === "ASSIGNED" && (
                <Button 
                  variant="success" 
                  size="small" 
                  onClick={() => handleStartAssembly(order.id)}
                  disabled={processingOrderId === order.id}
                >
                  {processingOrderId === order.id ? 'Starting...' : 'Start Task'}
                </Button>
              )}
              {order.status === "IN_PROGRESS" && (
                <>
                  <Button 
                    variant="success" 
                    size="small"
                    onClick={() => handleCompleteAssembly(order.id)}
                    disabled={processingOrderId === order.id}
                  >
                    {processingOrderId === order.id ? 'Completing...' : 'Complete'}
                  </Button>
                  <Button 
                    variant="warning" 
                    size="small"
                    onClick={() => handleHaltAssembly(order.id)}
                    disabled={processingOrderId === order.id}
                  >
                    Halt
                  </Button>
                </>
              )}
              <Button variant="outline" size="small" onClick={() => handleViewDetails(order)}>
                View Details
              </Button>
            </div>
          </Card>
        ))}
      </div>
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
    <>
      <DashboardLayout
        title={getStationTitle()}
        subtitle={`Execute assembly tasks for ${session?.user?.workstation?.name || 'workstation'}`}
        icon="🔩"
        layout="compact"
        statsCards={renderStatsCards()}
        primaryContent={
          <div className="dashboard-box">
            <div className="dashboard-box-header dashboard-box-header-green">
              <h2 className="dashboard-box-header-title">Assembly Tasks</h2>
              {renderFilterControls()}
            </div>
            <div className="dashboard-box-content">
              {renderAssemblyOrders()}
            </div>
          </div>
        }
        messages={{ error, success }}
        onDismissError={() => setError(null)}
        onDismissSuccess={() => setSuccess(null)}
      />
      {renderDetailsModal()}
    </>
  );
}

export default AssemblyWorkstationDashboard;
