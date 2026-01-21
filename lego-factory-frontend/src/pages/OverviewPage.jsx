import { useAuth } from "../context/AuthContext.jsx";
import PageHeader from "../components/PageHeader.jsx";
import { StatisticsGrid } from "../components";
import Footer from "../components/Footer.jsx";
import "../styles/OverviewPage.css";

function OverviewPage() {
  const { session } = useAuth();

  const statsData = [
    { value: 'Online', label: 'System Status', variant: 'success', icon: '✓' },
    { value: '-', label: 'Active Orders', variant: 'info', icon: '📦' },
    { value: '-', label: 'Pending Tasks', variant: 'warning', icon: '⚠' },
    { value: '0', label: 'Notifications', variant: 'default', icon: '🔔' },
    { value: '-', label: 'Total Users', variant: 'info', icon: '👥' },
    { value: '-', label: 'Workstations', variant: 'default', icon: '🏭' },
    { value: '-', label: 'Inventory Items', variant: 'info', icon: '📊' },
    { value: '-', label: 'Completed Today', variant: 'success', icon: '✅' },
  ];

  return (
    <div className="overview-page">
      <PageHeader 
        title="Overview" 
        subtitle={`Welcome, ${session?.user?.username || "User"}`}
      />

      <div className="overview-grid">
        <StatisticsGrid stats={statsData} />
      </div>

      <div className="placeholder-section">
        <div className="placeholder-card">
          <h3>Overview Dashboard</h3>
          <p>
            This page will display personalized statistics, notifications, system status,
            and order information curated specifically for your role and workstation.
          </p>
          <ul>
            <li>Recent activity and events</li>
            <li>System notifications and alerts</li>
            <li>Order status summaries</li>
            <li>Workstation-specific metrics</li>
            <li>Quick actions and shortcuts</li>
          </ul>
          <div className="placeholder-note">
            <strong>Note:</strong> This page is under development and will be fully implemented soon.
          </div>
        </div>
      </div>
    </div>
  );
}

export default OverviewPage;
