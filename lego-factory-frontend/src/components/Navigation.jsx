import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import "../styles/Navigation.css";

/**
 * Navigation Component
 * Main navigation menu with role-based menu items and submenus
 */
function Navigation() {
  const location = useLocation();
  const { isAdmin, session } = useAuth();
  const [expandedMenus, setExpandedMenus] = useState({});

  const toggleMenu = (menuKey) => {
    setExpandedMenus(prev => ({
      ...prev,
      [menuKey]: !prev[menuKey]
    }));
  };

  const isActive = (path) => location.pathname === path;
  const isParentActive = (paths) => paths.some(path => location.pathname.startsWith(path));

  /**
   * Role-based title mapping for dashboard menu
   */
  const getRoleTitle = () => {
    const role = session?.user?.role;
    const roleTitles = {
      ADMIN: "ADMIN",
      PLANT_WAREHOUSE: "WAREHOUSE",
      MODULES_SUPERMARKET: "SUPERMARKET",
      PRODUCTION_PLANNING: "PLANNING",
      PRODUCTION_CONTROL: "PRODUCTION",
      ASSEMBLY_CONTROL: "ASSEMBLY CTRL",
      MANUFACTURING: "MANUFACTURING",
      ASSEMBLY_WORKSTATION: "ASSEMBLY WS",
      PARTS_SUPPLY: "PARTS SUPPLY"
    };
    return roleTitles[role] || "DASHBOARD";
  };

  /**
   * Role-based route mapping for operational pages
   */
  const getRoleRoute = () => {
    const role = session?.user?.role;
    const roleRoutes = {
      ADMIN: "/control/masterdata",
      PLANT_WAREHOUSE: "/warehouse",
      MODULES_SUPERMARKET: "/modules-supermarket",
      PRODUCTION_PLANNING: "/production-planning",
      PRODUCTION_CONTROL: "/production-control",
      ASSEMBLY_CONTROL: "/assembly-control",
      MANUFACTURING: "/manufacturing",
      ASSEMBLY_WORKSTATION: "/assembly-workstation",
      PARTS_SUPPLY: "/parts-supply"
    };
    return roleRoutes[role] || "/dashboard";
  };

  return (
    <nav className="main-navigation">
      <ul className="nav-list">
        {/* Dashboard - Routes to Dashboard (visual analytics) */}
        <li className={isActive("/dashboard") ? "active" : ""}>
          <Link to="/dashboard">Dashboard</Link>
        </li>

        {/* NEW: Overview Pages - Design System Components */}
        <li className={isParentActive(["/overview"]) ? "has-submenu active" : "has-submenu"}>
          <button 
            type="button" 
            className="menu-toggle"
            onClick={() => toggleMenu("overview")}
          >
            Overview
            <span className={`arrow ${expandedMenus.overview ? "expanded" : ""}`}>▼</span>
          </button>
          <ul className="submenu" style={{ display: expandedMenus.overview ? "block" : undefined }}>
            {isAdmin && (
              <li className={isActive("/overview/admin") ? "active" : ""}>
                <Link to="/overview/admin">Admin Overview</Link>
              </li>
            )}
            <li className={isActive("/overview/manager") ? "active" : ""}>
              <Link to="/overview/manager">Manager Overview</Link>
            </li>
            {(session?.user?.role === "PLANT_WAREHOUSE" || isAdmin) && (
              <li className={isActive("/overview/warehouse") ? "active" : ""}>
                <Link to="/overview/warehouse">Warehouse Overview</Link>
              </li>
            )}
            {(session?.user?.role === "MANUFACTURING_WORKSTATION" || isAdmin) && (
              <li className={isActive("/overview/manufacturing") ? "active" : ""}>
                <Link to="/overview/manufacturing">Manufacturing Overview</Link>
              </li>
            )}
          </ul>
        </li>

        {/* Role-specific operational page with Admin submenu */}
        {isAdmin ? (
          <li className={isParentActive(["/control"]) ? "has-submenu active" : "has-submenu"}>
            <button 
              type="button" 
              className="menu-toggle"
              onClick={() => toggleMenu("admin")}
            >
              {getRoleTitle()}
              <span className={`arrow ${expandedMenus.admin ? "expanded" : ""}`}>▼</span>
            </button>
            <ul className="submenu" style={{ display: expandedMenus.admin ? "block" : undefined }}>
              <li className={isActive("/control/products") ? "active" : ""}>
                <Link to="/control/products">Products</Link>
              </li>
              <li className={isActive("/control/masterdata") ? "active" : ""}>
                <Link to="/control/masterdata">Masterdata</Link>
              </li>
              <li className={isActive("/control/inventory") ? "active" : ""}>
                <Link to="/control/inventory">Inventory</Link>
              </li>
              <li className={isActive("/control/warehouses") ? "active" : ""}>
                <Link to="/control/warehouses">Warehouses</Link>
              </li>
              <li className={isActive("/control/users") ? "active" : ""}>
                <Link to="/control/users">Users</Link>
              </li>
              <li className={isActive("/control/variants") ? "active" : ""}>
                <Link to="/control/variants">Variants</Link>
              </li>
            </ul>
          </li>
        ) : session?.user?.role === "PRODUCTION_PLANNING" ? (
          /* Production Planning with Manual Scheduler submenu */
          <li className={isParentActive(["/production-planning"]) ? "has-submenu active" : "has-submenu"}>
            <button 
              type="button" 
              className="menu-toggle"
              onClick={() => toggleMenu("planning")}
            >
              {getRoleTitle()}
              <span className={`arrow ${expandedMenus.planning ? "expanded" : ""}`}>▼</span>
            </button>
            <ul className="submenu" style={{ display: expandedMenus.planning ? "block" : undefined }}>
              <li className={isActive("/production-planning") ? "active" : ""}>
                <Link to="/production-planning">Dashboard</Link>
              </li>
              <li className={isActive("/production-planning/manual-scheduler") ? "active" : ""}>
                <Link to="/production-planning/manual-scheduler">Manual Scheduler</Link>
              </li>
            </ul>
          </li>
        ) : (
          <li className={isActive(getRoleRoute()) ? "active" : ""}>
            <Link to={getRoleRoute()}>{getRoleTitle()}</Link>
          </li>
        )}

        {/* Account Submenu */}
        <li className={isParentActive(["/account"]) ? "has-submenu active" : "has-submenu"}>
          <button 
            type="button" 
            className="menu-toggle"
            onClick={() => toggleMenu("account")}
          >
            Account
            <span className={`arrow ${expandedMenus.account ? "expanded" : ""}`}>▼</span>
          </button>
          <ul className="submenu" style={{ display: expandedMenus.account ? "block" : undefined }}>
            <li className={isActive("/account/user") ? "active" : ""}>
              <Link to="/account/user">User Account</Link>
            </li>
            <li className={isActive("/account/workstation") ? "active" : ""}>
              <Link to="/account/workstation">Workstation</Link>
            </li>
          </ul>
        </li>
      </ul>
    </nav>
  );
}

export default Navigation;
