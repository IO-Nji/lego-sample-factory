import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { getRolePrimaryWorkstation } from "../config/workstationConfig";
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
 * 
 * Routing Strategy (priority order):
 * 1. Admin role → AdminDashboard
 * 2. User's explicit workstationId → Workstation-specific dashboard
 * 3. Role's primary workstation → Workstation-specific dashboard
 * 4. Role-specific dashboard (for control/planning roles with no workstation)
 * 5. Fallback for unknown configuration
 * 
 * Workstation Mapping (WS-1 to WS-9):
 * - WS-1: Injection Molding (Manufacturing)
 * - WS-2: Parts Pre-Production (Manufacturing)
 * - WS-3: Part Finishing (Manufacturing)
 * - WS-4: Gear Assembly (Assembly)
 * - WS-5: Motor Assembly (Assembly)
 * - WS-6: Final Assembly (Assembly)
 * - WS-7: Plant Warehouse (Customer Fulfillment)
 * - WS-8: Modules Supermarket (Internal Warehouse)
 * - WS-9: Parts Supply Warehouse (Raw Materials)
 * 
 * @see workstationConfig.js for ROLE_WORKSTATION_ACCESS mapping
 * @see backend UserRole.java for consistent access rules
 */

// Workstation ID → Dashboard Component mapping
const WORKSTATION_DASHBOARDS = {
  1: InjectionMoldingDashboard,
  2: PartsPreProductionDashboard,
  3: PartFinishingDashboard,
  4: GearAssemblyDashboard,
  5: MotorAssemblyDashboard,
  6: FinalAssemblyDashboard,
  7: PlantWarehouseDashboard,
  8: ModulesSupermarketDashboard,
  9: PartsSupplyWarehouseDashboard,
};

// Role → Dashboard Component mapping (for roles without a specific workstation)
const ROLE_DASHBOARDS = {
  ADMIN: AdminDashboard,
  PRODUCTION_PLANNING: ProductionPlanningDashboard,
  PRODUCTION_CONTROL: ProductionControlDashboard,
  ASSEMBLY_CONTROL: AssemblyControlDashboard,
};

function DashboardPage() {
  const { session, isAdmin } = useAuth();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(false);
  }, []);

  const userRole = session?.user?.role;
  // Ensure workstationId is a number for consistent lookup
  const rawWorkstationId = session?.user?.workstationId;
  const userWorkstationId = rawWorkstationId ? Number(rawWorkstationId) : null;

  if (isLoading) {
    return (
      <section>
        <h2>Loading Dashboard...</h2>
        <p>Initializing your personalized dashboard...</p>
      </section>
    );
  }

  // 1. Admin gets admin dashboard
  if (isAdmin) {
    return <AdminDashboard />;
  }

  // 2. Route by user's explicit workstation ID (highest priority)
  if (userWorkstationId && WORKSTATION_DASHBOARDS[userWorkstationId]) {
    const DashboardComponent = WORKSTATION_DASHBOARDS[userWorkstationId];
    return <DashboardComponent />;
  }

  // 3. Route by role's primary workstation (from config)
  const rolePrimaryWorkstation = getRolePrimaryWorkstation(userRole);
  if (rolePrimaryWorkstation > 0 && WORKSTATION_DASHBOARDS[rolePrimaryWorkstation]) {
    const DashboardComponent = WORKSTATION_DASHBOARDS[rolePrimaryWorkstation];
    return <DashboardComponent />;
  }

  // 4. Route by role-specific dashboard (for control/planning roles)
  if (userRole && ROLE_DASHBOARDS[userRole]) {
    const DashboardComponent = ROLE_DASHBOARDS[userRole];
    return <DashboardComponent />;
  }

  // 5. Default fallback for unknown roles/configurations
  return (
    <section>
      <h2>Factory</h2>
      <p>Welcome to the LEGO Factory Control System</p>
      <p>Your role ({userRole || "Unknown"}) is currently being set up. Contact your administrator if you believe this is an error.</p>
    </section>
  );
}

export default DashboardPage;
