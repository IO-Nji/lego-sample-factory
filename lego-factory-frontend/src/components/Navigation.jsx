import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import "./Navigation.css";

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

  return (
    <nav className="main-navigation">
      <ul className="nav-list">
        {/* 1. Overview */}
        <li className={isActive("/") ? "active" : ""}>
          <Link to="/">Overview</Link>
        </li>

        {/* 2. Dashboard */}
        <li className={isActive("/dashboard") ? "active" : ""}>
          <Link to="/dashboard">Dashboard</Link>
        </li>

        {/* 3. Account - Submenu */}
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

        {/* 4. CONTROL - Admin Only Submenu */}
        {isAdmin && (
          <li className={isParentActive(["/control"]) ? "has-submenu active" : "has-submenu"}>
            <button 
              type="button" 
              className="menu-toggle"
              onClick={() => toggleMenu("control")}
            >
              CONTROL
              <span className={`arrow ${expandedMenus.control ? "expanded" : ""}`}>▼</span>
            </button>
            <ul className="submenu" style={{ display: expandedMenus.control ? "block" : undefined }}>
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
        )}
      </ul>
    </nav>
  );
}

export default Navigation;
