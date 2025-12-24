import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import PageHeader from "../components/PageHeader.jsx";
import Button from "../components/Button.jsx";
import "../styles/AccountPage.css";

function WorkstationPage() {
  const { session } = useAuth();
  const [workstationData, setWorkstationData] = useState({
    id: session?.user?.workstationId || "",
    name: session?.user?.workstationName || "",
    type: "",
    status: "Active",
    capacity: "",
    notes: ""
  });
  const [isEditing, setIsEditing] = useState(false);
  const [message, setMessage] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setWorkstationData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ type: "info", text: "Updating workstation details..." });
    
    // TODO: Implement API call to update workstation
    setTimeout(() => {
      setMessage({ type: "success", text: "Workstation details updated successfully!" });
      setIsEditing(false);
    }, 1000);
  };

  const handleCancel = () => {
    setWorkstationData({
      id: session?.user?.workstationId || "",
      name: session?.user?.workstationName || "",
      type: "",
      status: "Active",
      capacity: "",
      notes: ""
    });
    setIsEditing(false);
    setMessage(null);
  };

  return (
    <div className="account-page">
      <PageHeader 
        title="Workstation Management" 
        subtitle="View and manage your workstation details"
      />

      {message && (
        <div className={`message message-${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="account-card">
        <form onSubmit={handleSubmit}>
          <div className="form-section">
            <h3>Workstation Information</h3>
            
            <div className="form-group">
              <label htmlFor="id">Workstation ID</label>
              <input
                id="id"
                name="id"
                type="text"
                value={workstationData.id}
                disabled
                className="form-input readonly"
              />
            </div>

            <div className="form-group">
              <label htmlFor="name">Workstation Name</label>
              <input
                id="name"
                name="name"
                type="text"
                value={workstationData.name}
                disabled
                className="form-input readonly"
              />
            </div>

            <div className="form-group">
              <label htmlFor="status">Status</label>
              <select
                id="status"
                name="status"
                value={workstationData.status}
                onChange={handleChange}
                disabled={!isEditing}
                className="form-input"
              >
                <option value="Active">Active</option>
                <option value="Maintenance">Maintenance</option>
                <option value="Offline">Offline</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="notes">Station Notes / Report</label>
              <textarea
                id="notes"
                name="notes"
                value={workstationData.notes}
                onChange={handleChange}
                disabled={!isEditing}
                className="form-input"
                rows="6"
                placeholder="Enter workstation notes, issues, or reports..."
              />
            </div>
          </div>

          <div className="form-actions">
            {!isEditing ? (
              <Button
                type="button"
                variant="primary"
                onClick={() => setIsEditing(true)}
              >
                Edit Workstation
              </Button>
            ) : (
              <>
                <Button type="submit" variant="success">
                  Save Changes
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={handleCancel}
                >
                  Cancel
                </Button>
              </>
            )}
          </div>
        </form>
      </div>

      <div className="info-section">
        <h3>Workstation Guidelines</h3>
        <ul>
          <li>Update status when performing maintenance or if offline</li>
          <li>Use notes to report issues or log important events</li>
          <li>Contact administrator for workstation configuration changes</li>
        </ul>
      </div>
    </div>
  );
}

export default WorkstationPage;
