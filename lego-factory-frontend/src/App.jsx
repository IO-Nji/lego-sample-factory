import { Navigate, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import UserAccountPage from "./pages/UserAccountPage.jsx";
import AdminDashboard from "./pages/dashboards/AdminDashboard.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import UserManagementPage from "./pages/UserManagementPage.jsx";
import WarehouseManagementPage from "./pages/WarehouseManagementPage.jsx";
import InventoryManagementPage from "./pages/InventoryManagementPage.jsx";
import MasterdataAdminPage from "./pages/MasterdataAdminPage.jsx";
import ProductsPage from "./pages/ProductsPage.jsx";
import VariantsPage from "./pages/VariantsPage.jsx";
import DashboardLayout from "./layouts/DashboardLayout.jsx";
import WebhooksAdminPage from "./pages/WebhooksAdminPage.jsx";
import ManualSchedulerPage from "./pages/ManualSchedulerPage.jsx";
// Overview Pages using Design System Components
import AdminOverviewPage from "./pages/AdminOverviewPage.jsx";
import ManagerOverviewPage from "./pages/ManagerOverviewPage.jsx";
import WarehouseOverviewPage from "./pages/WarehouseOverviewPage.jsx";
import ManufacturingOverviewPage from "./pages/ManufacturingOverviewPage.jsx";
// Demo Pages
import OrderCardDemo from "./pages/demos/OrderCardDemo.jsx";
import { useAuth } from "./context/AuthContext.jsx";
import { AuthGuard, AdminGuard } from "./components/AuthGuard.jsx";

function App() {
  const { isAuthenticated, isAdmin, isPlantWarehouse, session } = useAuth();

  // Define all role checks for consistent guard logic
  const isModulesSupermarket = session?.user?.role === "MODULES_SUPERMARKET";
  const isProductionPlanning = session?.user?.role === "PRODUCTION_PLANNING";
  const isProductionControl = session?.user?.role === "PRODUCTION_CONTROL";
  const isAssemblyControl = session?.user?.role === "ASSEMBLY_CONTROL";
  const isPartsSupplyWarehouse = session?.user?.role === "PARTS_SUPPLY";
  const isManufacturingWorkstation = session?.user?.role === "MANUFACTURING_WORKSTATION";
  const isAssemblyWorkstation = session?.user?.role === "ASSEMBLY_WORKSTATION";

  return (
    <Routes>
      <Route element={<DashboardLayout />}>
        {/* Root and Overview Route - shows dashboard for authenticated users */}
        <Route index element={isAuthenticated ? <DashboardPage /> : <HomePage />} />
        
        {/* Dashboard - All authenticated users */}
        <Route
          path="dashboard"
          element={
            <AuthGuard>
              <DashboardPage />
            </AuthGuard>
          }
        />

        {/* Account Management - All authenticated users */}
        <Route
          path="account/user"
          element={
            <AuthGuard>
              <UserAccountPage />
            </AuthGuard>
          }
        />

        {/* OVERVIEW Pages - New Design System Components */}
        <Route
          path="overview/admin"
          element={
            <AdminGuard>
              <AdminOverviewPage />
            </AdminGuard>
          }
        />
        <Route
          path="overview/manager"
          element={
            <AuthGuard>
              <ManagerOverviewPage />
            </AuthGuard>
          }
        />
        <Route
          path="overview/warehouse"
          element={
            <AuthGuard requiredRole="PLANT_WAREHOUSE">
              <WarehouseOverviewPage />
            </AuthGuard>
          }
        />
        <Route
          path="overview/manufacturing"
          element={
            <AuthGuard requiredRole="MANUFACTURING_WORKSTATION">
              <ManufacturingOverviewPage />
            </AuthGuard>
          }
        />

        {/* CONTROL Menu - Admin Only */}
        <Route
          path="control/products"
          element={
            <AdminGuard>
              <ProductsPage />
            </AdminGuard>
          }
        />
        <Route
          path="control/masterdata"
          element={
            <AdminGuard>
              <MasterdataAdminPage />
            </AdminGuard>
          }
        />
        <Route
          path="control/inventory"
          element={
            <AdminGuard>
              <InventoryManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="control/warehouses"
          element={
            <AdminGuard>
              <WarehouseManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="control/users"
          element={
            <AdminGuard>
              <UserManagementPage />
            </AdminGuard>
          }
        />
        <Route
          path="control/variants"
          element={
            <AdminGuard>
              <VariantsPage />
            </AdminGuard>
          }
        />

        {/* Production Planning - Manual Scheduler */}
        <Route
          path="production-planning/manual-scheduler"
          element={
            <AuthGuard requiredRole="PRODUCTION_PLANNING">
              <ManualSchedulerPage />
            </AuthGuard>
          }
        />

        {/* Demo Pages - Public access for testing */}
        <Route
          path="demos/order-cards"
          element={<OrderCardDemo />}
        />
      </Route>
      <Route path="/login" element={<LoginPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
