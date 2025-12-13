import DashboardPage from "./DashboardPage";
import { useAuth } from "../context/AuthContext.jsx";
import "../styles/HomePage.css";

function HomePage() {
  const { isAuthenticated } = useAuth();

  // If not authenticated, show login prompt
  if (!isAuthenticated) {
    return (
      <section className="home-page">
        <div className="home-hero">
          <h2>Welcome to the LEGO Factory</h2>
          <p className="subtitle">
            A comprehensive digital manufacturing platform designed to
            streamline and coordinate complex production workflows across
            multiple interconnected factory stations. This application enables
            real-time monitoring, inventory management, order processing, and
            production planning for modern manufacturing environments. From
            parts supply and injection molding to assembly control and warehouse
            management, the LEGO Factory system provides an integrated solution
            for optimizing manufacturing operations and ensuring efficient
            coordination between all production stages.
          </p>
        </div>
        
        <div className="home-actions">
          <a href="/login" className="btn btn-primary">
            Sign In
          </a>
        </div>

        <div className="home-content">
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
