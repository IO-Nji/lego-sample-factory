import { useState } from "react";
import { DashboardLayout, Notification } from "../../components";
import "../../styles/DashboardLayout.css";

function ManufacturingDashboard() {
  const [notifications, setNotifications] = useState([]);

  const addNotification = (message, type = 'info') => {
    const newNotification = {
      id: Date.now() + Math.random(),
      message,
      type,
      timestamp: new Date().toISOString(),
      station: 'MANFCT'
    };
    setNotifications(prev => [newNotification, ...prev]);
  };

  const clearNotifications = () => {
    setNotifications([]);
  };

  return (
    <DashboardLayout
      title="Manufacturing Workstation Dashboard"
      subtitle="Configure your workstation-specific controls here"
      icon="🔧"
      layout="default"
      secondaryContent={
        <Notification 
          notifications={notifications}
          title="Manufacturing Activity"
          maxVisible={5}
          onClear={clearNotifications}
        />
      }
      ordersSection={
        <div className="dashboard-box-content">
          <div className="dashboard-empty-state">
            <p className="dashboard-empty-state-title">Manufacturing Interface</p>
            <p className="dashboard-empty-state-text">Workstation-specific controls will appear here</p>
          </div>
        </div>
      }
    />
  );
}

export default ManufacturingDashboard;
