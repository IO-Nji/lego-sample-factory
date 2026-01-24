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
    const workstationName = session?.user?.workstation?.name;
    
    // Use workstation name if available, otherwise use role-based titles
    if (workstationName) {
      return workstationName.toUpperCase();
    }
    
    const roleTitles = {
      ADMIN: "ADMIN CONTROL",
      PLANT_WAREHOUSE: "PLANT WAREHOUSE",
      MODULES_SUPERMARKET: "MODULES SUPERMARKET",
      PRODUCTION_PLANNING: "PRODUCTION PLANNING",
      PRODUCTION_CONTROL: "PRODUCTION CONTROL",
      ASSEMBLY_CONTROL: "ASSEMBLY CONTROL",
      MANUFACTURING: "MANUFACTURING",
      ASSEMBLY_WORKSTATION: "ASSEMBLY WORKSTATION",
      PARTS_SUPPLY: "PARTS SUPPLY WAREHOUSE"
    };
    return roleTitles[role] || "DASHBOARD";
  };

  return (
    <nav className="main-navigation">
      <ul className="nav-list">
        {/* Role-based Dashboard - Routes to role-specific dashboard */}
        <li className={isActive("/dashboard") ? "active" : ""}>
          <Link to="/dashboard">{getRoleTitle()}</Link>
        </li>

        {/* Admin Control Menu */}
        {isAdmin && (
          <li className={isParentActive(["/control"]) ? "has-submenu active" : "has-submenu"}>
            <button 
              type="button" 
              className="menu-toggle"
              onClick={() => toggleMenu("admin")}
            >
              Control
              <span className={`arrow ${expandedMenus.admin ? "expanded" : ""}`}>▼</span>
            </button>
            <ul className="submenu" style={{ display: expandedMenus.admin ? "block" : undefined }}>
              <li className={isActive("/control/users") ? "active" : ""}>
                <Link to="/control/users">Users</Link>
              </li>
              <li className={isActive("/control/masterdata") ? "active" : ""}>
                <Link to="/control/masterdata">Masterdata</Link>
              </li>
              <li className={isActive("/control/inventory") ? "active" : ""}>
                <Link to="/control/inventory">Inventory</Link>
              </li>
            </ul>
          </li>
        )}

        {/* Production Planning Manual Scheduler */}
        {session?.user?.role === "PRODUCTION_PLANNING" && (
          <li className={isActive("/production-planning/manual-scheduler") ? "active" : ""}>
            <Link to="/production-planning/manual-scheduler">Manual Scheduler</Link>
          </li>
        )}

        {/* Account Menu */}
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
              <Link to="/account/user">Profile</Link>
            </li>
            <li className={isActive("/account/workstation") ? "active" : ""}>
              <Link to="/account/workstation">Workstation Info</Link>
            </li>
          </ul>
        </li>
      </ul>
    </nav>
  );
}

export default Navigation;
