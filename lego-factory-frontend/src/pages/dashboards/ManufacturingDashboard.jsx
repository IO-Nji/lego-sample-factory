import { DashboardLayout } from "../../components";
import "../../styles/DashboardLayout.css";

function ManufacturingDashboard() {
  return (
    <DashboardLayout
      title="Manufacturing Workstation Dashboard"
      subtitle="Configure your workstation-specific controls here"
      icon="🔧"
      layout="default"
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
