import { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";
import PageHeader from "../components/PageHeader.jsx";
import Button from "../components/Button.jsx";
import "../styles/AccountPage.css";

function UserAccountPage() {
  const { session } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    username: session?.user?.username || "",
    email: session?.user?.email || "",
    fullName: session?.user?.fullName || "",
    role: session?.user?.role || ""
  });
  const [message, setMessage] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ type: "info", text: "Updating account details..." });
    
    setTimeout(() => {
      setMessage({ type: "success", text: "Account details updated successfully!" });
      setIsEditing(false);
    }, 1000);
  };

  const handleCancel = () => {
    setFormData({
      username: session?.user?.username || "",
      email: session?.user?.email || "",
      fullName: session?.user?.fullName || "",
      role: session?.user?.role || ""
    });
    setIsEditing(false);
    setMessage(null);
  };

  return (
    <div className="account-page">
      <PageHeader 
        title="User Account" 
        subtitle="Manage your account details and preferences"
      />

      {message && (
        <div className={`message message-${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="account-card">
        <form onSubmit={handleSubmit}>
          <div className="form-section">
            <h3>Account Information</h3>
            
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                name="username"
                type="text"
                value={formData.username}
                onChange={handleChange}
                disabled={!isEditing}
                className="form-input"
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                disabled={!isEditing}
                className="form-input"
                placeholder="user@example.com"
              />
            </div>

            <div className="form-group">
              <label htmlFor="fullName">Full Name</label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                value={formData.fullName}
                onChange={handleChange}
                disabled={!isEditing}
                className="form-input"
                placeholder="Enter your full name"
              />
            </div>

            <div className="form-group">
              <label htmlFor="role">Role</label>
              <input
                id="role"
                name="role"
                type="text"
                value={formData.role.replace(/_/g, " ")}
                disabled
                className="form-input readonly"
              />
              <small className="form-help">Your role is assigned by an administrator</small>
            </div>
          </div>

          <div className="form-actions">
            {!isEditing ? (
              <Button
                type="button"
                variant="primary"
                onClick={() => setIsEditing(true)}
              >
                Edit Account
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
    </div>
  );
}

export default UserAccountPage;
