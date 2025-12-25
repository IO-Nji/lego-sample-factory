import PageHeader from "../../components/PageHeader";
import "../../styles/StandardPage.css";
import "../../styles/DashboardStandard.css";

function ManufacturingDashboard() {
  return (
    <div className="standard-page-container">
    <section className="dashboard-page">
      <PageHeader
        title="Manufacturing Workstation Dashboard"
        subtitle="Configure your workstation-specific controls here"
        icon="🔧"
      />
      <p>Manufacturing workstation interface</p>
    </section>
    </div>
  );
}

export default ManufacturingDashboard;
