import { Navigate, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import AdminDashboard from "./pages/AdminDashboard.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import UserManagementPage from "./pages/UserManagementPage.jsx";
import WarehouseManagementPage from "./pages/WarehouseManagementPage.jsx";
import InventoryManagementPage from "./pages/InventoryManagementPage.jsx";
import MasterdataAdminPage from "./pages/MasterdataAdminPage.jsx";
import ProductsPage from "./pages/ProductsPage.jsx";
import DashboardLayout from "./layouts/DashboardLayout.jsx";
import ManufacturingWorkstationPage from "./pages/ManufacturingWorkstationPage.jsx";
import WebhooksAdminPage from "./pages/WebhooksAdminPage.jsx";
import ProductionPlanningPage from "./pages/ProductionPlanningPage.jsx";
import ModulesSupermarketPage from "./pages/ModulesSupermarketPage.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { AuthGuard, AdminGuard } from "./components/AuthGuard.jsx";

function App() {
  const { isAuthenticated, isAdmin, isPlantWarehouse, session } = useAuth();

  // Define all role checks for consistent guard logic
  const isModulesSupermarket = session?.user?.role === "MODULES_SUPERMARKET";
  const isProductionPlanning = session?.user?.role === "PRODUCTION_PLANNING";
  const isProductionControl = session?.user?.role === "PRODUCTION_CONTROL";
  const isAssemblyControl = session?.user?.role === "ASSEMBLY_CONTROL";
  const isPartsSupplyWarehouse = session?.user?.role === "PARTS_SUPPLY_WAREHOUSE";
  const isManufacturingWorkstation = session?.user?.role === "MANUFACTURING_WORKSTATION";
  const isAssemblyWorkstation = session?.user?.role === "ASSEMBLY_WORKSTATION";

  return (
    <Routes>
      <Route element={<DashboardLayout />}>
        <Route index element={<HomePage />} />
        <Route
          path="dashboard"
          element={
            <AuthGuard>
              <DashboardPage />
            </AuthGuard>
          }
        />
        <Route
          path="admin-dashboard"
          element={
            <AdminGuard>
              <AdminDashboard />
            </AdminGuard>
          }
        />
        <Route
          path="users"
          element={
            <AdminGuard>
              <UserManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="admin/masterdata"
          element={
            <AdminGuard>
              <MasterdataAdminPage />
            </AdminGuard>
          }
        />
        <Route
          path="warehouses"
          element={
            <AdminGuard>
              <WarehouseManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="inventory"
          element={
            <AdminGuard>
              <InventoryManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="admin/webhooks"
          element={
            <AdminGuard>
              <WebhooksAdminPage />
            </AdminGuard>
          }
        />
        <Route
          path="products"
          element={
            <AuthGuard>
              <ProductsPage />
            </AuthGuard>
          }
        />
        <Route
          path="warehouse"
          element={
            <AuthGuard requiredRole="PLANT_WAREHOUSE">
              <DashboardPage />
            </AuthGuard>
          }
        />
        <Route
          path="modules-supermarket"
          element={
            <AuthGuard requiredRole="MODULES_SUPERMARKET">
              <ModulesSupermarketPage />
            </AuthGuard>
          }
        />
        <Route
          path="production-planning"
          element={
            <AuthGuard requiredRole="PRODUCTION_PLANNING">
              <ProductionPlanningPage />
            </AuthGuard>
          }
        />
        <Route
          path="production-control"
          element={
            <AuthGuard requiredRole="PRODUCTION_CONTROL">
              <DashboardPage />
            </AuthGuard>
          }
        />
        <Route
          path="assembly-control"
          element={
            <AuthGuard requiredRole="ASSEMBLY_CONTROL">
              <DashboardPage />
            </AuthGuard>
          }
        />
        <Route
          path="parts-supply-warehouse"
          element={
            <AuthGuard requiredRole="PARTS_SUPPLY_WAREHOUSE">
              <DashboardPage />
            </AuthGuard>
          }
        />
        <Route
          path="manufacturing/:workstationType"
          element={
            <AuthGuard requiredRole="MANUFACTURING_WORKSTATION">
              <ManufacturingWorkstationPage />
            </AuthGuard>
          }
        />
        <Route
          path="assembly/:workstationType"
          element={
            <AuthGuard requiredRole="ASSEMBLY_WORKSTATION">
              <ManufacturingWorkstationPage />
            </AuthGuard>
          }
        />
      </Route>
      <Route path="/login" element={<LoginPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
