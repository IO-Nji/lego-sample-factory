import { useAuth } from "../context/AuthContext.jsx";
import PageHeader from "../components/PageHeader.jsx";
import StatsCard from "../components/StatsCard.jsx";
import "../styles/OverviewPage.css";

function OverviewPage() {
  const { session } = useAuth();

  return (
    <div className="overview-page">
      <PageHeader 
        title="Overview" 
        subtitle={`Welcome, ${session?.user?.username || "User"}`}
      />

      <div className="overview-grid">
        <StatsCard
          title="System Status"
          value="Online"
          icon="✓"
          variant="success"
        />
        
        <StatsCard
          title="Active Orders"
          value="-"
          icon="📦"
          variant="info"
        />
        
        <StatsCard
          title="Pending Tasks"
          value="-"
          icon="⚠"
          variant="warning"
        />
        
        <StatsCard
          title="Notifications"
          value="0"
          icon="🔔"
          variant="default"
        />
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
