import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import AdminDashboard from "./dashboards/AdminDashboard";
import PlantWarehouseDashboard from "./dashboards/PlantWarehouseDashboard";
import ModulesSupermarketDashboard from "./dashboards/ModulesSupermarketDashboard";
import ProductionPlanningDashboard from "./dashboards/ProductionPlanningDashboard";
import ProductionControlDashboard from "./dashboards/ProductionControlDashboard";
import AssemblyControlDashboard from "./dashboards/AssemblyControlDashboard";
import ManufacturingDashboard from "./dashboards/ManufacturingDashboard";
import AssemblyWorkstationDashboard from "./dashboards/AssemblyWorkstationDashboard";
import PartsSupplyWarehouseDashboard from "./dashboards/PartsSupplyWarehouseDashboard";

/**
 * DashboardPage - Role-aware dashboard router
 * Routes users to their appropriate dashboard based on role
 * 
 * Modular Architecture:
 * - Each dashboard is in its own file under pages/dashboards/
 * - Shared utilities in utils/dashboardHelpers.js
 * - Easier to maintain, test, and debug
 */
function DashboardPage() {
  const { session, isAdmin } = useAuth();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(false);
  }, []);

  const userRole = session?.user?.role;

  if (isLoading) {
    return (
      <section>
        <h2>Loading Dashboard...</h2>
        <p>Initializing your personalized dashboard...</p>
      </section>
    );
  }

  // Route to appropriate dashboard based on role
  if (isAdmin) {
    return <AdminDashboard />;
  }

  if (userRole === "PLANT_WAREHOUSE") {
    return <PlantWarehouseDashboard />;
  }

  if (userRole === "MODULES_SUPERMARKET") {
    return <ModulesSupermarketDashboard />;
  }

  if (userRole === "PRODUCTION_PLANNING") {
    return <ProductionPlanningDashboard />;
  }

  if (userRole === "PRODUCTION_CONTROL") {
    return <ProductionControlDashboard />;
  }

  if (userRole === "ASSEMBLY_CONTROL") {
    return <AssemblyControlDashboard />;
  }

  if (userRole === "MANUFACTURING_WORKSTATION") {
    return <ManufacturingDashboard />;
  }

  if (userRole === "ASSEMBLY_WORKSTATION") {
    return <AssemblyWorkstationDashboard />;
  }

  if (userRole === "PARTS_SUPPLY") {
    return <PartsSupplyWarehouseDashboard />;
  }

  // Default fallback for unknown roles
  return (
    <section>
      <h2>Factory</h2>
      <p>Welcome to the LEGO Factory Control System</p>
      <p>Your role ({userRole || "Unknown"}) is currently being set up. Contact your administrator if you believe this is an error.</p>
    </section>
  );
}

export default DashboardPage;
