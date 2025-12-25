import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import axios from "axios";
import PageHeader from "../../components/PageHeader";
import Button from "../../components/Button";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

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
    <div className="standard-page-container">
      <PageHeader
        title={getStationTitle()}
        subtitle={`Workstation ${session?.user?.workstationId || 'N/A'} - Process assembly tasks for production orders`}
        icon="🔩"
      />
      <section className="dashboard-page">

        {error && (
          <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
            <p className="font-semibold text-red-900">Error</p>
            <p className="text-red-800 text-sm mt-1">{error}</p>
            <button onClick={() => setError(null)} className="text-red-600 hover:text-red-800 font-semibold text-sm mt-2">
              Dismiss
            </button>
          </div>
        )}

        {successMessage && (
          <div className="form-success-details mb-6">
            <p className="font-semibold text-green-900">Success</p>
            <p className="text-green-800 text-sm mt-1">{successMessage}</p>
            <button onClick={() => setSuccessMessage(null)} className="text-green-700 hover:text-green-900 font-semibold text-sm mt-2">
              Dismiss
            </button>
          </div>
        )}

        {!session?.user?.workstationId && (
          <div className="form-error mb-6 p-4 bg-red-50 border-l-4 border-red-600 rounded">
            <p className="font-semibold text-red-900">⚠️ No workstation assigned</p>
            <p className="text-red-800 text-sm mt-1">Contact administrator to assign a workstation to your account.</p>
          </div>
        )}

        <div className="bg-white rounded-lg shadow-md overflow-hidden border border-gray-200 mb-6">
          <div className="bg-purple-50 px-6 py-3 border-b border-purple-200">
            <h2 className="text-lg font-semibold text-purple-900">📋 Assembly Tasks</h2>
          </div>
          <div className="p-6">
            {loading ? (
              <div className="text-center text-gray-500 py-8">Loading assembly tasks...</div>
            ) : assemblyOrders.length > 0 ? (
              <div className="orders-grid">
                {assemblyOrders.map((order) => (
                  <div key={order.id} className={`customer-order-card status-${getStatusBadgeClass(order.status)}`}>
                    <div className="order-card-header">
                      <span className="order-number">#{order.controlOrderNumber}</span>
                      <span className={`order-status-badge ${getStatusBadgeClass(order.status)}`}>
                        {order.status}
                      </span>
                    </div>
                    
                    <div className="order-card-body">
                      <div className="order-items-list">
                        <div className="order-item">
                          <div className="item-name">Quantity</div>
                          <div className="item-quantity">{order.quantity || 1}</div>
                        </div>
                        {order.priority && (
                          <div className="order-item">
                            <div className="item-name">Priority</div>
                            <div className="item-quantity">{order.priority}</div>
                          </div>
                        )}
                      </div>
                      
                      <div className="order-date">
                        {new Date(order.createdAt).toLocaleDateString('en-US', { 
                          month: 'short', 
                          day: 'numeric',
                          year: 'numeric'
                        })}
                      </div>
                    </div>

                    {(order.status === 'ASSIGNED' || order.status === 'IN_PROGRESS') && (
                      <div className="order-card-footer">
                        <div className="action-buttons">
                          {order.status === 'ASSIGNED' && (
                            <Button 
                              variant="primary" 
                              size="small" 
                              onClick={() => handleStartAssembly(order.id)}
                              disabled={processingOrderId === order.id}
                            >
                              Start Assembly
                            </Button>
                          )}
                          
                          {order.status === 'IN_PROGRESS' && (
                            <Button 
                              variant="success" 
                              size="small" 
                              onClick={() => handleCompleteAssembly(order.id)}
                              disabled={processingOrderId === order.id}
                              loading={processingOrderId === order.id}
                            >
                              {processingOrderId === order.id ? 'Completing...' : 'Complete Assembly'}
                            </Button>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500 py-8">
                <p className="text-sm">No assembly tasks assigned to this workstation</p>
                <p className="text-xs mt-2 text-gray-400">Tasks will appear here when production orders are created</p>
              </div>
            )}
          </div>
        </div>

        <style>{`
          .form-success-details {
            background: #efe;
            color: #3c3;
            border-left: 4px solid #3c3;
            padding: 1rem;
            border-radius: 0.5rem;
            margin-bottom: 1.5rem;
          }
        `}</style>
      </section>
    </div>
  );
}

export default AssemblyWorkstationDashboard;
