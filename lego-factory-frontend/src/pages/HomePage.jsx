import DashboardPage from "./DashboardPage";
import LoginForm from "../components/LoginForm";
import FeatureCard from "../components/FeatureCard";
import { useAuth } from "../context/AuthContext.jsx";
import api from "../api/api";
import "../styles/StandardPage.css";
import "../styles/HomePage.css";
import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

function HomePage() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [message, setMessage] = useState("");
  const [products, setProducts] = useState([]);
  const [modules, setModules] = useState([]);
  const [parts, setParts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [expandedModules, setExpandedModules] = useState({});

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

  // Fetch products, modules, and parts for display
  useEffect(() => {
    const fetchAllData = async () => {
      try {
        const [productsRes, modulesRes, partsRes] = await Promise.all([
          api.get('/masterdata/product-variants'),
          api.get('/masterdata/modules'),
          api.get('/masterdata/parts')
        ]);
        setProducts(productsRes.data);
        setModules(modulesRes.data);
        setParts(partsRes.data);
        console.log('Loaded products:', productsRes.data.length);
        console.log('Loaded modules:', modulesRes.data.length);
        console.log('Loaded parts:', partsRes.data.length);
      } catch (err) {
        console.error('Error fetching data:', err);
      } finally {
        setLoadingProducts(false);
      }
    };
    
    if (!isAuthenticated) {
      fetchAllData();
    }
  }, [isAuthenticated]);

  const toggleModuleExpand = (moduleId) => {
    setExpandedModules(prev => ({
      ...prev,
      [moduleId]: !prev[moduleId]
    }));
  };

  const getModulePartsForProduct = (product) => {
    return modules;
  };

  const getPartsForModule = (module) => {
    return parts;
  };

  // If not authenticated, show login prompt with embedded login form
  if (!isAuthenticated) {
    return (
      <section className="home-page">
        {/* Welcome Header */}
        <div className="home-hero">
          <h2>LIFE System - LEGO Integrated Factory Execution</h2>
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
        
        {/* Row 1: Products + Flow Diagram + Login Form */}
        <div className="home-top-row">
          {/* Products Column */}
          <div className="home-products-column">
            <h3 className="section-title">Our Products</h3>
            {loadingProducts ? (
              <p className="loading-text">Loading products...</p>
            ) : selectedProduct ? (
              <div className="product-details">
                <button 
                  className="btn-back" 
                  onClick={() => setSelectedProduct(null)}
                >
                  ← Back to Products
                </button>
                
                <div className="product-detail-card">
                  <h2>{selectedProduct.name}</h2>
                  <p className="description">{selectedProduct.description}</p>
                  <div className="specs">
                    <div className="spec-item">
                      <strong>Price:</strong>
                      <span className="price">${selectedProduct.price.toFixed(2)}</span>
                    </div>
                    <div className="spec-item">
                      <strong>Est. Time:</strong>
                      <span>{selectedProduct.estimatedTimeMinutes} min</span>
                    </div>
                  </div>
                </div>

                <div className="components-section">
                  <h3>Product Components</h3>
                  
                  {getModulePartsForProduct(selectedProduct).length === 0 ? (
                    <p className="no-data">No components available</p>
                  ) : (
                    getModulePartsForProduct(selectedProduct).map(module => (
                      <div key={module.id} className="module-item">
                        <div 
                          onClick={() => toggleModuleExpand(module.id)}
                          className="module-header"
                        >
                          <span className="expand-icon">
                            {expandedModules[module.id] ? '▼' : '▶'}
                          </span>
                          <div className="module-info">
                            <h4>{module.name}</h4>
                            <p>{module.type || 'COMPONENT'}</p>
                          </div>
                        </div>

                        {expandedModules[module.id] && (
                          <div className="module-details">
                            <p>{module.description}</p>
                            
                            <div className="parts-section">
                              <h5>Parts Required:</h5>
                              {getPartsForModule(module).length === 0 ? (
                                <p className="no-data">No parts specified</p>
                              ) : (
                                <ul>
                                  {getPartsForModule(module).map(part => (
                                    <li key={part.id}>
                                      <span className="part-name">{part.name}</span>
                                      <span className="part-category">{part.category}</span>
                                      <span className="part-cost">${part.unitCost?.toFixed(2) || '0.00'}</span>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </div>
              </div>
            ) : products.length === 0 ? (
              <p className="no-data">No products available</p>
            ) : (
              <div className="products-grid-2x2">
                {products.map(product => (
                  <div 
                    key={product.id} 
                    className="product-card"
                    onClick={() => setSelectedProduct(product)}
                  >
                    <h4>{product.name}</h4>
                    <p>{product.description}</p>
                    <div className="product-specs">
                      <span className="price">${product.price.toFixed(2)}</span>
                      <span className="time">{product.estimatedTimeMinutes}m</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Flow Diagram Column */}
          <div className="home-flow-diagram-section">
            <h3 className="section-title">Order Fulfillment Process Flow</h3>
            
            {/* Flow Diagram */}
            <div className="flow-diagram">
              {/* Row 1: Customer Order Entry */}
              <div className="flow-row">
                <div className="station-card" data-tooltip="Customer places order for product variants">
                  <div className="station-icon">🛒</div>
                  <div className="station-name">Customer Order</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Final product storage and order fulfillment (WS-7)">
                  <div className="station-icon">🏭</div>
                  <div className="station-name">Plant Warehouse</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Check stock availability">
                  <div className="station-icon">✅</div>
                  <div className="station-name">Stock Check</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Order delivered to customer">
                  <div className="station-icon">📦</div>
                  <div className="station-name">Delivery</div>
                </div>
              </div>
              
              {/* Row 2: Downward arrow */}
              <div className="flow-row">
                <div className="flow-spacer"></div>
                <div className="flow-arrow-down">↓</div>
                <div className="flow-note">If stock unavailable</div>
                <div className="flow-spacer"></div>
              </div>
              
              {/* Row 3: Production Flow */}
              <div className="flow-row">
                <div className="station-card" data-tooltip="Intermediate module storage and management (WS-8)">
                  <div className="station-icon">🏢</div>
                  <div className="station-name">Modules Supermarket</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Final assembly of product variants (WS-6)">
                  <div className="station-icon">🔨</div>
                  <div className="station-name">Final Assembly</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Strategic production scheduling and resource planning">
                  <div className="station-icon">📋</div>
                  <div className="station-name">Production Planning</div>
                </div>
              </div>
              
              {/* Row 4: Downward arrow */}
              <div className="flow-row">
                <div className="flow-spacer"></div>
                <div className="flow-arrow-down">↓</div>
                <div className="flow-note">If modules unavailable</div>
                <div className="flow-spacer"></div>
              </div>
              
              {/* Row 5: Manufacturing Flow */}
              <div className="flow-row">
                <div className="station-card" data-tooltip="Raw materials storage and distribution (WS-9)">
                  <div className="station-icon">📦</div>
                  <div className="station-name">Parts Supply</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Injection molding and part production (WS-1,2,3)">
                  <div className="station-icon">🔧</div>
                  <div className="station-name">Manufacturing</div>
                </div>
                <div className="flow-arrow">→</div>
                <div className="station-card" data-tooltip="Component assembly into modules (WS-4,5)">
                  <div className="station-icon">⚙️</div>
                  <div className="station-name">Assembly</div>
                </div>
              </div>
            </div>
          </div>
          
          <div className="home-login-section">
            <LoginForm embedded={true} showHeader={true} showHelpText={true} />
          </div>
        </div>

        {/* Row 2: Application Overview + Navigation Guide (Full Width) */}
        <div className="home-bottom-row">
          <div className="home-intro-column-fullwidth">
            <h3 className="section-title">Application Overview</h3>
            <div className="intro-content">
              <p className="intro-description">
                The <strong>LIFE System</strong> (LEGO Integrated Factory Execution) is a comprehensive digital manufacturing 
                platform that coordinates complex production workflows across multiple interconnected factory workstations. 
                The system manages the entire manufacturing lifecycle from raw materials to finished products.
              </p>
              
              <div className="navigation-guide">
                <h4>📋 Quick Navigation Guide</h4>
                <ul>
                  <li><strong>Login:</strong> Use credentials on the right to access role-specific dashboards</li>
                  <li><strong>User Roles:</strong> Admin, Production Planning, Plant Warehouse, Assembly Control, and more</li>
                  <li><strong>Operations:</strong> Each role provides tailored views for order management, inventory control, and production tracking</li>
                  <li><strong>Real-time Updates:</strong> Dashboard data refreshes automatically every 30 seconds</li>
                </ul>
                <p className="tip">💡 <strong>Tip:</strong> Hover over the manufacturing stations above to see detailed descriptions</p>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <footer className="home-footer">
          <p>© {new Date().getFullYear()} <strong>nji.io</strong> - All Rights Reserved</p>
        </footer>
      </section>
    );
  }

  // If authenticated, show the dashboard
  return <DashboardPage />;
}

export default HomePage;
