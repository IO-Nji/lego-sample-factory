import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import { DashboardLayout, Button } from "../../components";
import "../../styles/DashboardLayout.css";

function AssemblyWorkstationDashboard() {
  const { session } = useAuth();
  const [assemblyOrders, setAssemblyOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [processingOrderId, setProcessingOrderId] = useState(null);

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
      const interval = setInterval(fetchAssemblyOrders, 10000);
      return () => clearInterval(interval);
    }
  }, [session?.user?.workstationId]);

  const fetchAssemblyOrders = async () => {
    if (!session?.user?.workstationId) return;
    
    try {
      setLoading(true);
      const response = await axios.get(`${apiEndpoint}/workstation/${session.user.workstationId}`);
      setAssemblyOrders(Array.isArray(response.data) ? response.data : []);
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
    setSuccessMessage(null);

    try {
      await axios.put(`${apiEndpoint}/${orderId}/start`);
      setSuccessMessage("Assembly task started successfully!");
      fetchAssemblyOrders();
    } catch (err) {
      setError("Failed to start assembly: " + (err.response?.data?.message || err.message));
    } finally {
      setProcessingOrderId(null);
    }
  };

  const handleCompleteAssembly = async (orderId) => {
    setProcessingOrderId(orderId);
    setError(null);
    setSuccessMessage(null);

    try {
      const isFinalAssembly = assemblyType === "final-assembly";
      const response = await axios.put(`${apiEndpoint}/${orderId}/complete`);
      
      const creditMsg = isFinalAssembly
        ? "Plant Warehouse has been credited with a finished product."
        : "Modules Supermarket has been credited with a module unit.";
      
      setSuccessMessage(`Assembly task completed successfully! ${creditMsg}`);
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

  return (
    <DashboardLayout
      title={getStationTitle()}
      subtitle={`Workstation ${session?.user?.workstationId || 'N/A'} - Process assembly tasks`}
      icon="🔩"
      layout="default"
      ordersSection={renderAssemblyOrders()}
      messages={{ error, success: successMessage }}
      onDismissError={() => setError(null)}
      onDismissSuccess={() => setSuccessMessage(null)}
    />
  );
}

export default AssemblyWorkstationDashboard;
