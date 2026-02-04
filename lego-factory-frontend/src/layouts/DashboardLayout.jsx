import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import Navigation from "../components/Navigation.jsx";

function DashboardLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, logout, session } = useAuth();

  // Handle logout and redirect to home page
  const handleLogout = () => {
    logout();
    // Don't navigate - logout() already redirects to home
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="header-top">
          <h1>LEGO Factory Control</h1>
          {isAuthenticated && (
            <div className="user-info">
              <span className="username">{session?.user?.username}</span>
              {session?.user?.role && (
                <span className="role-badge">
                  {session.user.role.replaceAll("_", " ")}
                </span>
              )}
              <button 
                type="button" 
                className="logout-button" 
                onClick={handleLogout}
                title="Log out"
              >
                ⎋
              </button>
            </div>
          )}
        </div>
        {isAuthenticated && <Navigation />}
      </header>
      <main className="app-content">
        <Outlet key={location.pathname} />
      </main>
    </div>
  );
}

export default DashboardLayout;