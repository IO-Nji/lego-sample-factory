import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import api from "../api/api";
import { DashboardLayout, StatCard, Button, Card, Badge } from "../components";
import "../styles/DashboardLayout.css";

/**
 * Generic Workstation Page - Used for both Manufacturing and Assembly
 * Manufacturing: Injection Molding, Parts Pre-Production, Part Finishing
 * Assembly: Gear Assembly, Motor Assembly, Final Assembly
 * Displays tasks and allows operators to complete them.
 */
function ManufacturingWorkstationPage() {
  const { workstationType } = useParams();
  const { session } = useAuth();
  const [controlOrders, setControlOrders] = useState([]);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [operatorNotes, setOperatorNotes] = useState("");
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  const workstationId = session?.user?.workstation?.id;

  // Map manufacturing workstation types to API endpoints
  const manufacturingEndpoints = {
    "injection-molding": "/api/manufacturing/injection-molding",
    "parts-pre-production": "/api/manufacturing/parts-pre-production",
    "part-finishing": "/api/manufacturing/part-finishing",
  };

  // Map assembly workstation types to API endpoints
  const assemblyEndpoints = {
    "gear-assembly": "/api/assembly/gear-assembly",
    "motor-assembly": "/api/assembly/motor-assembly",
    "final-assembly": "/api/assembly/final-assembly",
  };

  // Combine all endpoints
  const apiEndpointMap = { ...manufacturingEndpoints, ...assemblyEndpoints };

  // Manufacturing station titles
  const manufacturingTitles = {
    "injection-molding": "🏭 Injection Molding Station",
    "parts-pre-production": "⚙️ Parts Pre-Production Station",
    "part-finishing": "✨ Part Finishing Station",
  };

  // Assembly station titles
  const assemblyTitles = {
    "gear-assembly": "⚙️ Gear Assembly Station",
    "motor-assembly": "🔌 Motor Assembly Station",
    "final-assembly": "📦 Final Assembly Station",
  };

  // Combine all titles
  const stationTitles = { ...manufacturingTitles, ...assemblyTitles };

  const apiEndpoint = apiEndpointMap[workstationType] || "/api/manufacturing/injection-molding";
  const stationTitle = stationTitles[workstationType] || "Workstation";
  const isAssembly = Object.keys(assemblyTitles).includes(workstationType);
  const taskType = isAssembly ? "assembly" : "manufacturing";

  useEffect(() => {
    if (workstationId) {
      fetchControlOrders();
      const interval = setInterval(fetchControlOrders, 30000); // 30 seconds
      return () => clearInterval(interval);
    }
  }, [workstationId, workstationType]);

  const fetchControlOrders = async () => {
    if (!workstationId) return;
    setLoading(true);
    setError(null);

    try {
      const response = await api.get(`${apiEndpoint}/workstation/${workstationId}`);
      const orders = Array.isArray(response.data) ? response.data : [];
      setControlOrders(orders);
    } catch (err) {
      setError(`Failed to load ${taskType} tasks: ` + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const startProduction = async (orderId) => {
    try {
      setLoading(true);
      await api.post(`${apiEndpoint}/${orderId}/start`, { operatorId: session?.user?.id || "UNKNOWN" });
      await fetchControlOrders();
      setSuccess(`${taskType.charAt(0).toUpperCase() + taskType.slice(1)} task started successfully!`);
    } catch (err) {
      setError(`Failed to start ${taskType}: ` + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const completeProduction = async (orderId) => {
    const message = isAssembly 
      ? "Are you sure the assembly task is complete and quality checks have passed?"
      : "Are you sure the manufacturing task is complete and quality checks have passed?";
    
    if (!window.confirm(message)) return;

    try {
      setLoading(true);
      await api.put(`${apiEndpoint}/${orderId}/complete`, {});
      setSelectedOrder(null);
      setShowDetailsModal(false);
      await fetchControlOrders();
      const creditMsg = isAssembly && workstationType === "final-assembly"
        ? "Plant Warehouse has been credited with a finished product."
        : "Modules Supermarket has been credited with a module unit.";
      setSuccess(`${taskType.charAt(0).toUpperCase() + taskType.slice(1)} task completed! ${creditMsg}`);
    } catch (err) {
      setError(`Failed to complete ${taskType} task: ` + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const haltProduction = async (orderId) => {
    const reason = prompt(`Enter reason for halting ${taskType}:`);
    if (!reason) return;

    try {
      setLoading(true);
      await api.post(`${apiEndpoint}/${orderId}/halt`, { reason });
      setSelectedOrder(null);
      setShowDetailsModal(false);
      await fetchControlOrders();
      setSuccess(`${taskType.charAt(0).toUpperCase() + taskType.slice(1)} task halted and logged`);
    } catch (err) {
      setError(`Failed to halt ${taskType}: ` + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const getFilteredOrders = () => {
    if (filterStatus === "ALL") return controlOrders;
    return controlOrders.filter((o) => o.status === filterStatus);
  };

  const handleViewDetails = (order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  // Stats cards
  const renderStatsCards = () => {
    const total = controlOrders.length;
    const assigned = controlOrders.filter(o => o.status === "ASSIGNED").length;
    const inProgress = controlOrders.filter(o => o.status === "IN_PROGRESS").length;
    const completed = controlOrders.filter(o => o.status === "COMPLETED").length;

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
        onClick={fetchControlOrders} 
        disabled={loading}
      >
        {loading ? "Refreshing..." : "Refresh"}
      </Button>
    </div>
  );

  // Orders display
  const renderOrders = () => {
    const filteredOrders = getFilteredOrders();

    if (loading && controlOrders.length === 0) {
      return <div className="dashboard-empty-state">Loading tasks...</div>;
    }

    if (filteredOrders.length === 0) {
      return (
        <div className="dashboard-empty-state">
          <p className="dashboard-empty-state-title">No tasks</p>
          <p className="dashboard-empty-state-text">
            {filterStatus !== "ALL" 
              ? `No tasks with status: ${filterStatus}` 
              : `No ${taskType} tasks assigned yet`}
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
                  {order.controlOrderNumber}
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
              <div style={{ marginBottom: "0.25rem" }}>
                <span style={{ fontWeight: "500" }}>Target:</span> {new Date(order.targetCompletionTime).toLocaleString()}
              </div>
            </div>
            
            <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
              {order.status === "ASSIGNED" && (
                <Button variant="success" size="small" onClick={() => startProduction(order.id)}>
                  Start Task
                </Button>
              )}
              {order.status === "IN_PROGRESS" && (
                <>
                  <Button variant="success" size="small" onClick={() => completeProduction(order.id)}>
                    Complete
                  </Button>
                  <Button variant="warning" size="small" onClick={() => haltProduction(order.id)}>
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
            <h2>{taskType.charAt(0).toUpperCase() + taskType.slice(1)} Task: {selectedOrder.controlOrderNumber}</h2>
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
            {selectedOrder.productionInstructions && (
              <div style={{ marginTop: "1rem" }}>
                <strong>Production Instructions:</strong>
                <p style={{ marginTop: "0.5rem", padding: "0.75rem", backgroundColor: "#f3f4f6", borderRadius: "0.375rem" }}>
                  {selectedOrder.productionInstructions}
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
            {selectedOrder.safetyProcedures && (
              <div style={{ marginTop: "1rem" }}>
                <strong>Safety Procedures:</strong>
                <p style={{ marginTop: "0.5rem", padding: "0.75rem", backgroundColor: "#fef3c7", borderRadius: "0.375rem" }}>
                  {selectedOrder.safetyProcedures}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  const filteredOrders = getFilteredOrders();

  return (
    <>
      <DashboardLayout
        title={stationTitle}
        subtitle={`Execute ${taskType} tasks for ${session?.user?.workstation?.name || 'workstation'}`}
        icon={isAssembly ? "⚙️" : "🏭"}
        layout="compact"
        statsCards={renderStatsCards()}
        primaryContent={
          <div className="dashboard-box">
            <div className="dashboard-box-header dashboard-box-header-green">
              <h2 className="dashboard-box-header-title">{taskType.charAt(0).toUpperCase() + taskType.slice(1)} Tasks</h2>
              {renderFilterControls()}
            </div>
            <div className="dashboard-box-content">
              {renderOrders()}
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

export default ManufacturingWorkstationPage;
