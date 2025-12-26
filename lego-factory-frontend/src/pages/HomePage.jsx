import DashboardPage from "./DashboardPage";
import LoginForm from "../components/LoginForm";
import { useAuth } from "../context/AuthContext.jsx";
import "../styles/StandardPage.css";
import "../styles/HomePage.css";
import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

function HomePage() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [message, setMessage] = useState("");

  useEffect(() => {
    const reason = location.state?.reason || new URLSearchParams(location.search).get('reason');
    
    // Redirect to login page if there's an authentication-related reason
    if (reason === "expired" || reason === "unauthenticated" || reason === "session_expired" || reason === "backend_unavailable") {
      navigate('/login?reason=' + reason, { replace: true });
      return;
    }
    
    if (reason === "unauthorized") {
      setMessage("You do not have permission to access that page.");
      setTimeout(() => setMessage(""), 5000);
    } else if (reason === "backend_down") {
      setMessage("Unable to connect to the backend. Please check if the services are running.");
      setTimeout(() => setMessage(""), 8000);
    }
  }, [location.state, location.search, navigate]);

  // If not authenticated, show login prompt with embedded login form
  if (!isAuthenticated) {
    return (
      <section className="home-page">
        <div className="home-hero">
          <h2>Welcome to the LEGO Factory</h2>
          <p className="subtitle">
            A comprehensive digital manufacturing platform designed to
            streamline and coordinate complex production workflows across
            multiple interconnected factory stations.
          </p>
        </div>

        {message && (
          <div className="alert-message" style={{
            backgroundColor: '#fef3cd',
            border: '1px solid #ffc107',
            padding: '12px 20px',
            margin: '20px auto',
            maxWidth: '600px',
            borderRadius: '4px',
            textAlign: 'center',
            color: '#856404'
          }}>
            {message}
          </div>
        )}
        
        {/* Embedded Login Form */}
        <div className="home-login-section">
          <LoginForm embedded={true} showHeader={false} showHelpText={true} />
        </div>

        <div className="home-content">
          <h3 style={{ textAlign: 'center', color: 'var(--color-primary)', marginBottom: 'var(--spacing-xl)' }}>
            Platform Features
          </h3>
          <div className="feature-cards">
            <div className="card">
              <h1>🏭</h1>
              <h3>Plant Warehouse</h3>
              <p>
                Manage customer orders and inventory at the plant warehouse
                distribution center.
              </p>
            </div>
            <div className="card">
              <h1>🏢</h1>
              <h3>Modules Supermarket</h3>
              <p>
                Organize module assembly and fulfill warehouse orders with
                pre-assembled modules.
              </p>
            </div>
            <div className="card">
              <h1>📋</h1>
              <h3>Production Planning</h3>
              <p>
                Plan and schedule production workflows across manufacturing and
                assembly stations.
              </p>
            </div>
            <div className="card">
              <h1>🏭 </h1>
              <h3>Production Control</h3>
              <p>
                Monitor manufacturing workstations and track production progress
                in real-time.
              </p>
            </div>
            <div className="card">
              <h1>⚙️</h1>
              <h3>Assembly Control</h3>
              <p>
                Supervise assembly workstations including gear, motor, and final
                assembly stations.
              </p>
            </div>
            <div className="card">
              <h1>📦</h1>
              <h3>Parts Supply Warehouse</h3>
              <p>
                Supply parts to manufacturing and assembly stations on demand.
              </p>
            </div>
            <div className="card">
              <h1>🔧</h1>
              <h3>Manufacturing Stations</h3>
              <p>
                Execute manufacturing tasks at injection molding,
                pre-production, and finishing stations.
              </p>
            </div>
            <div className="card">
              <h1>🔩</h1>
              <h3> Assembly Stations</h3>
              <p>
                Execute assembly tasks for gear, motor, and final product
                assembly.
              </p>
            </div>
          </div>
        </div>
      </section>
    );
  }

  // If authenticated, show the dashboard
  return <DashboardPage />;
}

export default HomePage;
