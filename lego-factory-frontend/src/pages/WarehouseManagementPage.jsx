import { useState, useEffect } from "react";
import api from "../api/api";
import { WORKSTATIONS_ENDPOINT } from "../api/apiConfig";
import { useAuth } from "../context/AuthContext.jsx";
import "../styles/StandardPage.css";

function WarehouseManagementPage() {
  const { session, isAdmin, logout } = useAuth();
  const [warehouses, setWarehouses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState({ type: "", message: "" });
  const [selectedWarehouse, setSelectedWarehouse] = useState(null);

  const authToken = session?.token ?? null;

  // Load warehouses on mount
  useEffect(() => {
    if (isAdmin && authToken) {
      loadWarehouses();
    } else {
      setLoading(false);
    }
  }, [isAdmin, authToken]);

  const loadWarehouses = async () => {
    try {
      const response = await api.get(WORKSTATIONS_ENDPOINT, {
        headers: { Authorization: `Bearer ${authToken}` },
      });
      
      // Filter to only warehouse type workstations
      const warehouseList = response.data.filter(
        (ws) => ws.workstationType === "WAREHOUSE"
      );
      setWarehouses(warehouseList);
    } catch (error) {
      console.error("Failed to load warehouses:", error);
      if (error.response?.status === 401) {
        logout();
      }
      setFeedback({
        type: "error",
        message: "Failed to load warehouses. Please try again.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleWarehouseClick = (warehouse) => {
    setSelectedWarehouse(warehouse);
  };

  const closeModal = () => {
    setSelectedWarehouse(null);
  };

  if (!session) {
    return (
      <section className="form-section">
        <h2>Administrator sign-in required</h2>
        <p className="form-helper">Log in with an administrator account to manage warehouses.</p>
      </section>
    );
  }

  if (!isAdmin) {
    return (
      <section className="form-section">
        <h2>Insufficient permissions</h2>
        <p className="form-helper">
          You are signed in as <strong>{session.user?.username}</strong>, but only administrators can view
          warehouse management.
        </p>
      </section>
    );
  }

  return (
    <div className="standard-page-container">
      <div className="standard-page-header">
        <div className="standard-header-content">
          <h1>🏭 Warehouse Management</h1>
          <p className="standard-page-subtitle">
            View and manage all warehouses and distribution centers in the LEGO factory system. 
            Total Warehouses: {warehouses.length}
          </p>
        </div>
      </div>
      
      {feedback.message && (
        <div className={feedback.type === "error" ? "standard-error" : "standard-success"}>
          <p>{feedback.message}</p>
        </div>
      )}

      {loading ? (
        <div className="standard-loading">
          <p>Loading warehouses...</p>
        </div>
      ) : warehouses.length === 0 ? (
        <p className="standard-empty-message">No warehouses found in the system.</p>
      ) : (
        <>
          <div className="standard-grid">
            {warehouses.map((warehouse) => (
              <div 
                key={warehouse.id} 
                className="standard-card"
                onClick={() => handleWarehouseClick(warehouse)}
              >
                <div className="standard-card-header">
                  <h3>{warehouse.name}</h3>
                  <span className="standard-card-id">ID: {warehouse.id}</span>
                </div>
                
                <div className="standard-card-content">
                  <div className="standard-detail-row">
                    <span className="standard-detail-label">Type:</span>
                    <span className="standard-detail-value">{warehouse.workstationType}</span>
                  </div>
                  
                  <div className="standard-detail-row">
                    <span className="standard-detail-label">Description:</span>
                    <span className="standard-detail-value">{warehouse.description || "N/A"}</span>
                  </div>
                  
                  <div className="standard-detail-row">
                    <span className="standard-detail-label">Status:</span>
                    <span className={`standard-status-badge ${warehouse.active ? "active" : "inactive"}`}>
                      {warehouse.active ? "✓ Active" : "✗ Inactive"}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {selectedWarehouse && (
            <div className="standard-modal-overlay" onClick={closeModal}>
              <div className="standard-modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="standard-modal-header">
                  <h2>{selectedWarehouse.name}</h2>
                  <button className="standard-modal-close" onClick={closeModal}>✕</button>
                </div>
                
                <div className="standard-modal-body">
                  <div className="standard-modal-section">
                    <h3>Warehouse Information</h3>
                    
                    <dl className="standard-detail-list">
                      <dt>ID:</dt>
                      <dd>{selectedWarehouse.id}</dd>
                      
                      <dt>Type:</dt>
                      <dd>{selectedWarehouse.workstationType}</dd>
                      
                      <dt>Name:</dt>
                      <dd>{selectedWarehouse.name}</dd>
                      
                      <dt>Description:</dt>
                      <dd>{selectedWarehouse.description || "N/A"}</dd>
                      
                      <dt>Status:</dt>
                      <dd>
                        <span className={`standard-status-badge ${selectedWarehouse.active ? "active" : "inactive"}`}>
                          {selectedWarehouse.active ? "✓ Active" : "✗ Inactive"}
                        </span>
                      </dd>
                    </dl>
                  </div>
                </div>
                
                <div className="standard-modal-footer">
                  <button className="standard-action-btn" onClick={closeModal}>Close</button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default WarehouseManagementPage;
