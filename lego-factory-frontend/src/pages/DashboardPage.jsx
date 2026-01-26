import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import AdminDashboard from "./dashboards/AdminDashboard";
import PlantWarehouseDashboard from "./dashboards/PlantWarehouseDashboard";
import ModulesSupermarketDashboard from "./dashboards/ModulesSupermarketDashboard";
import PartsSupplyWarehouseDashboard from "./dashboards/PartsSupplyWarehouseDashboard";
import InjectionMoldingDashboard from "./dashboards/InjectionMoldingDashboard";
import PartsPreProductionDashboard from "./dashboards/PartsPreProductionDashboard";
import PartFinishingDashboard from "./dashboards/PartFinishingDashboard";
import GearAssemblyDashboard from "./dashboards/GearAssemblyDashboard";
import MotorAssemblyDashboard from "./dashboards/MotorAssemblyDashboard";
import FinalAssemblyDashboard from "./dashboards/FinalAssemblyDashboard";
import ProductionPlanningDashboard from "./dashboards/ProductionPlanningDashboard";
import ProductionControlDashboard from "./dashboards/ProductionControlDashboard";
import AssemblyControlDashboard from "./dashboards/AssemblyControlDashboard";

/**
 * DashboardPage - Workstation-aware dashboard router
 * Routes users to their workstation-specific dashboard based on workstationId
 * 
 * Workstation Mapping:
 * - WS-1: Injection Molding (Manufacturing)
 * - WS-2: Parts Pre-Production (Manufacturing)
 * - WS-3: Part Finishing (Manufacturing)
 * - WS-4: Gear Assembly (Assembly)
 * - WS-5: Motor Assembly (Assembly)
 * - WS-6: Final Assembly (Assembly)
 * - WS-7: Plant Warehouse (Customer Fulfillment)
 * - WS-8: Modules Supermarket (Internal Warehouse)
 * - WS-9: Parts Supply Warehouse (Raw Materials)
 */
function DashboardPage() {
  const { session, isAdmin } = useAuth();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(false);
  }, []);

  const userRole = session?.user?.role;
  // Ensure workstationId is a number for consistent switch comparison
  const rawWorkstationId = session?.user?.workstationId;
  const workstationId = rawWorkstationId ? Number(rawWorkstationId) : null;

  if (isLoading) {
    return (
      <section>
        <h2>Loading Dashboard...</h2>
        <p>Initializing your personalized dashboard...</p>
      </section>
    );
  }

  // Admin gets admin dashboard
  if (isAdmin) {
    return <AdminDashboard />;
  }

  // Route by workstation ID first (most specific)
  switch (workstationId) {
    case 1:
      return <InjectionMoldingDashboard />;
    case 2:
      return <PartsPreProductionDashboard />;
    case 3:
      return <PartFinishingDashboard />;
    case 4:
      return <GearAssemblyDashboard />;
    case 5:
      return <MotorAssemblyDashboard />;
    case 6:
      return <FinalAssemblyDashboard />;
    case 7:
      return <PlantWarehouseDashboard />;
    case 8:
      return <ModulesSupermarketDashboard />;
    case 9:
      return <PartsSupplyWarehouseDashboard />;
  }

  // Fallback to role-based routing for users without workstation assignment
  switch (userRole) {
    case "PLANT_WAREHOUSE":
      return <PlantWarehouseDashboard />;
    case "MODULES_SUPERMARKET":
      return <ModulesSupermarketDashboard />;
    case "PARTS_SUPPLY":
      return <PartsSupplyWarehouseDashboard />;
    case "PRODUCTION_PLANNING":
      return <ProductionPlanningDashboard />;
    case "PRODUCTION_CONTROL":
      return <ProductionControlDashboard />;
    case "ASSEMBLY_CONTROL":
      return <AssemblyControlDashboard />;
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
