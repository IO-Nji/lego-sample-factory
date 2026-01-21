import DashboardPage from "./DashboardPage";
import LoginForm from "../components/LoginForm";
import FeatureCard from "../components/FeatureCard";
import { WorkstationCard } from "../components";
import Footer from "../components/Footer";
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
  const [productModules, setProductModules] = useState([]);
  const [loadingModules, setLoadingModules] = useState(false);
  const [serviceHealth, setServiceHealth] = useState({});

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

  // Fetch product-specific modules and their parts when a product is selected
  useEffect(() => {
    if (!selectedProduct) {
      setProductModules([]);
      return;
    }
    
    const fetchProductModules = async () => {
      setLoadingModules(true);
      try {
        console.log('Fetching modules for product:', selectedProduct.id, selectedProduct.name);
        
        // Fetch product modules
        const modulesResponse = await api.get(`/masterdata/product-variants/${selectedProduct.id}/modules`);
        const productModules = modulesResponse.data;
        
        console.log('Product modules:', productModules);
        
        // For each module, fetch its parts
        const modulesWithParts = await Promise.all(
          productModules.map(async (productModule) => {
            // Get module details
            const moduleResponse = await api.get(`/masterdata/modules/${productModule.moduleId}`);
            const moduleData = moduleResponse.data;
            
            // Get parts for this module
            const partsResponse = await api.get(`/masterdata/modules/${productModule.moduleId}/parts`);
            const moduleParts = partsResponse.data;
            
            // Fetch part details for each module part
            const partsWithDetails = await Promise.all(
              moduleParts.map(async (modulePart) => {
                const partResponse = await api.get(`/masterdata/parts/${modulePart.partId}`);
                return {
                  ...partResponse.data,
                  quantity: modulePart.quantity
                };
              })
            );
            
            return {
              ...moduleData,
              quantity: productModule.quantity,
              parts: partsWithDetails
            };
          })
        );
        
        console.log('Modules with parts:', modulesWithParts);
        setProductModules(modulesWithParts);
      } catch (error) {
        console.error('Error fetching product composition:', error);
        // Fallback to showing message
        setProductModules([]);
      } finally {
        setLoadingModules(false);
      }
    };
    
    fetchProductModules();
  }, [selectedProduct]);

  // Fetch service health status
  useEffect(() => {
    if (isAuthenticated) return; // Skip if authenticated
    
    const fetchServiceHealth = async () => {
      const healthStatus = {};
      
      // Frontend is always healthy if this code is running
      healthStatus['frontend'] = 'healthy';
      
      // Test API Gateway health check endpoint (public, no auth required)
      try {
        const response = await fetch('/api/health', {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        });
        
        if (response.ok) {
          // 200 OK means the API Gateway and services are UP
          healthStatus['api-gateway'] = 'healthy';
          healthStatus['user-service'] = 'healthy';
          healthStatus['masterdata-service'] = 'healthy';
          healthStatus['inventory-service'] = 'healthy';
          healthStatus['order-processing-service'] = 'healthy';
          healthStatus['simal-integration-service'] = 'healthy';
        } else {
          // Other errors mean services might be down
          healthStatus['api-gateway'] = 'down';
          healthStatus['user-service'] = 'down';
          healthStatus['masterdata-service'] = 'down';
          healthStatus['inventory-service'] = 'down';
          healthStatus['order-processing-service'] = 'down';
          healthStatus['simal-integration-service'] = 'down';
        }
      } catch (error) {
        console.error('Health check failed:', error);
        // Network error means services are down
        healthStatus['api-gateway'] = 'down';
        healthStatus['user-service'] = 'down';
        healthStatus['masterdata-service'] = 'down';
        healthStatus['inventory-service'] = 'down';
        healthStatus['order-processing-service'] = 'down';
        healthStatus['simal-integration-service'] = 'down';
      }
      
      setServiceHealth(healthStatus);
    };

    fetchServiceHealth();
    const interval = setInterval(fetchServiceHealth, 15000); // Poll every 15 seconds
    
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  const toggleModuleExpand = (moduleId) => {
    setExpandedModules(prev => ({
      ...prev,
      [moduleId]: !prev[moduleId]
    }));
  };

  // If not authenticated, show login prompt with embedded login form
  if (!isAuthenticated) {
    return (
      <>
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
            
            {/* Product Details Overlay Dropdown */}
            {selectedProduct && (
              <div className="product-details-overlay">
                <div className="product-details-modal">
                  <button 
                    className="btn-close-overlay" 
                    onClick={() => setSelectedProduct(null)}
                  >
                    ✕
                  </button>
                  
                  <div className="product-detail-card-overlay">
                    <h2>{selectedProduct.name}</h2>
                    <p className="description">{selectedProduct.description}</p>
                    <div className="specs">
                      <div className="spec-item">
                        <strong>Price:</strong>
                        <span className="price">${selectedProduct.price.toFixed(2)}</span>
                      </div>
                      <div className="spec-item">
                        <strong>Time:</strong>
                        <span>{selectedProduct.estimatedTimeMinutes} min</span>
                      </div>
                    </div>
                  </div>

                  <div className="components-section-overlay">
                    <h3>Product Components</h3>
                    
                    {loadingModules ? (
                      <p className="loading-text">Loading modules...</p>
                    ) : productModules.length === 0 ? (
                      <div>
                        <p className="no-data">No modules available for this product</p>
                      </div>
                    ) : (
                      <div className="modules-grid-overlay">
                        {productModules.map(module => (
                          <div key={module.id} className="module-item-overlay">
                            <div 
                              onClick={() => toggleModuleExpand(module.id)}
                              className="module-header-overlay"
                            >
                              <span className="expand-icon-overlay">
                                {expandedModules[module.id] ? '▼' : '▶'}
                              </span>
                              <div className="module-info-overlay">
                                <h4>{module.name}</h4>
                                <span className="module-qty">Qty: {module.quantity}</span>
                              </div>
                            </div>

                            {expandedModules[module.id] && (
                              <div className="module-details-overlay">
                                <p className="module-desc">{module.description}</p>
                                <div className="module-time">Time: {module.estimatedTimeMinutes} min</div>
                                
                                {module.parts && module.parts.length > 0 ? (
                                  <div className="parts-section-overlay">
                                    <h5>Parts ({module.parts.length}):</h5>
                                    <div className="parts-list-overlay">
                                      {module.parts.map(part => (
                                        <div key={part.id} className="part-item-overlay">
                                          <span className="part-name-overlay">{part.name}</span>
                                          <span className="part-qty-overlay">×{part.quantity}</span>
                                          <span className="part-time-overlay">{part.estimatedTimeMinutes}m</span>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                ) : (
                                  <p style={{fontSize: '0.65rem', color: '#999', marginTop: '0.3rem'}}>
                                    No parts configured for this module
                                  </p>
                                )}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
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
                <WorkstationCard 
                  icon="🛒" 
                  name="Customer Order" 
                  tooltip="Customer places order for product variants"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="🏭" 
                  name="Plant Warehouse" 
                  tooltip="Final product storage and order fulfillment | Username: warehouse_operator"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="✅" 
                  name="Stock Check" 
                  tooltip="Check stock availability"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="📦" 
                  name="Delivery" 
                  tooltip="Order delivered to customer"
                />
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
                <WorkstationCard 
                  icon="🏢" 
                  name="Modules Supermarket" 
                  tooltip="Intermediate module storage and management | Username: modules_supermarket"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="🔨" 
                  name="Final Assembly" 
                  tooltip="Final assembly of product variants | Username: assembly_control"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="📋" 
                  name="Production Planning" 
                  tooltip="Strategic production scheduling and resource planning | Username: production_planning"
                />
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
                <WorkstationCard 
                  icon="📦" 
                  name="Parts Supply" 
                  tooltip="Raw materials storage and distribution | Username: parts_supply_warehouse"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="🔧" 
                  name="Manufacturing" 
                  tooltip="Injection molding and part production | Username: injection_molding"
                />
                <div className="flow-arrow">→</div>
                <WorkstationCard 
                  icon="⚙️" 
                  name="Assembly" 
                  tooltip="Component assembly into modules | Username: assembly_control"
                />
              </div>
            </div>
          </div>
          
          <div className="home-login-section">
            <LoginForm embedded={true} showHeader={true} showHelpText={true} />
          </div>
        </div>

        {/* Row 2: Application Overview (Full Width) */}
        <div className="home-overview-row">
          <div className="home-overview-section">
            <h3 className="section-title">Application Overview</h3>
            <div className="overview-content">
              <p className="overview-description">
                The <strong>LIFE System</strong> (LEGO Integrated Factory Execution) is an enterprise-grade digital manufacturing 
                execution system (MES) built from the ground up to demonstrate the practical application of academic research 
                in industrial engineering. This comprehensive platform digitizes and automates end-to-end supply chain operations 
                across nine autonomous workstations, coordinating complex production workflows from raw materials to finished products.
              </p>
              
              <div className="tech-highlights-split">
                {/* Left Column - Right Aligned */}
                <div className="tech-column tech-column-left">
                  <div className="tech-feature">
                    <div className="feature-header">
                      <span className="feature-icon">🏗️</span>
                      <strong>Microservices Architecture</strong>
                    </div>
                    <p>6 independently scalable Spring Boot services with isolated H2 databases, orchestrated via Docker Compose</p>
                  </div>
                  
                  <div className="tech-feature">
                    <div className="feature-header">
                      <span className="feature-icon">🔐</span>
                      <strong>Enterprise Security</strong>
                    </div>
                    <p>JWT-based authentication with BCrypt encryption, role-based access control across 9 user roles</p>
                  </div>
                </div>
                
                {/* Center Divider */}
                <div className="tech-divider"></div>
                
                {/* Right Column - Left Aligned */}
                <div className="tech-column tech-column-right">
                  <div className="tech-feature">
                    <div className="feature-header">
                      <span className="feature-icon">⚡</span>
                      <strong>Modern Tech Stack</strong>
                    </div>
                    <p>Spring Boot 3.4.12 (Java 21) backend + React 18 (Vite) frontend + Spring Cloud Gateway + Nginx reverse proxy</p>
                  </div>
                  
                  <div className="tech-feature">
                    <div className="feature-header">
                      <span className="feature-icon">📊</span>
                      <strong>Real-Time Operations</strong>
                    </div>
                    <p>Live inventory tracking, automated order fulfillment, SimAL scheduling integration with interactive Gantt charts</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Row 3: Navigation Guide + Microservices Architecture (Two Columns) */}
        <div className="home-guide-row">
          {/* Column 1: Navigation Guide */}
          <div className="home-navigation-column">
            <h3 className="section-title">📋 Quick Navigation Guide</h3>
            <div className="navigation-content">
              <div className="nav-item">
                <span className="nav-icon">🔑</span>
                <div>
                  <strong>Login:</strong> Hover over workstation cards above to see usernames. Generic password: <code>password</code>
                </div>
              </div>
              
              <div className="nav-item">
                <span className="nav-icon">👥</span>
                <div>
                  <strong>User Roles:</strong> Each role has a dedicated dashboard (Admin, Production Planning, Plant Warehouse, 
                  Modules Supermarket, Assembly Control, Parts Supply, Manufacturing)
                </div>
              </div>
              
              <div className="nav-item">
                <span className="nav-icon">📦</span>
                <div>
                  <strong>Operations:</strong> Role-specific interfaces for customer orders, warehouse orders, production orders, 
                  inventory management, and supply chain coordination
                </div>
              </div>
              
              <div className="nav-item">
                <span className="nav-icon">🔄</span>
                <div>
                  <strong>Real-time Updates:</strong> Dashboard data refreshes automatically every 5-30 seconds depending on the view
                </div>
              </div>
              
              <div className="nav-item">
                <span className="nav-icon">📈</span>
                <div>
                  <strong>Production Scenarios:</strong> System supports 4 fulfillment workflows - direct fulfillment, warehouse orders, 
                  production via modules, and high-volume direct production planning
                </div>
              </div>
            </div>
          </div>

          {/* Column 2: Microservices Architecture Diagram */}
          <div className="home-architecture-column">
            <h3 className="section-title">🏗️ Microservices Architecture</h3>
            <div className="architecture-diagram">
              {/* Frontend Layer - Grouped Box */}
              <div className="service-group frontend-group">
                <div className="group-label">Frontend Layer</div>
                <div className="group-features">UI Workflow • Real-time Updates • Role-based Navigation</div>
                <div className="arch-layer">
                  <div className={`service-box frontend ${serviceHealth['frontend'] || 'unknown'}`}>
                    <span className="service-name">React SPA</span>
                    <span className="service-tech">(Vite + Nginx)</span>
                  </div>
                </div>
              </div>
              
              <div className="arch-arrow-down">↓</div>
              
              {/* API Gateway Layer */}
              <div className="arch-layer">
                <div className={`service-box gateway ${serviceHealth['api-gateway'] || 'unknown'}`}>
                  <span className="service-name">API Gateway</span>
                  <span className="service-tech">:8011</span>
                  <span className={`health-indicator ${serviceHealth['api-gateway'] || 'unknown'}`}></span>
                </div>
              </div>
              
              <div className="arch-arrow-down">↓</div>
              
              {/* Backend Services Layer - Grouped Box */}
              <div className="service-group backend-group">
                <div className="group-label">Backend Services</div>
                <div className="group-features">User Management • Business Logic • Database Access • Order Processing</div>
                <div className="arch-layer services-grid">
                  <div className={`service-box ${serviceHealth['user-service'] || 'unknown'}`}>
                    <span className="service-name">User Service</span>
                    <span className="service-tech">:8012</span>
                    <span className={`health-indicator ${serviceHealth['user-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div className={`service-box ${serviceHealth['masterdata-service'] || 'unknown'}`}>
                    <span className="service-name">Masterdata</span>
                    <span className="service-tech">:8013</span>
                    <span className={`health-indicator ${serviceHealth['masterdata-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div className={`service-box ${serviceHealth['inventory-service'] || 'unknown'}`}>
                    <span className="service-name">Inventory</span>
                    <span className="service-tech">:8014</span>
                    <span className={`health-indicator ${serviceHealth['inventory-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div className={`service-box ${serviceHealth['order-processing-service'] || 'unknown'}`}>
                    <span className="service-name">Order Processing</span>
                    <span className="service-tech">:8015</span>
                    <span className={`health-indicator ${serviceHealth['order-processing-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div className={`service-box ${serviceHealth['simal-integration-service'] || 'unknown'}`}>
                    <span className="service-name">SimAL Integration</span>
                    <span className="service-tech">:8016</span>
                    <span className={`health-indicator ${serviceHealth['simal-integration-service'] || 'unknown'}`}></span>
                  </div>
                </div>
                
                <div className="arch-note">
                  <span className="db-icon">💾</span> H2 In-Memory Databases (Isolated per service)
                </div>
              </div>
            </div>
            
            <div className="health-legend">
              <span className="legend-item">
                <span className="health-dot healthy"></span> Healthy
              </span>
              <span className="legend-item">
                <span className="health-dot degraded"></span> Degraded
              </span>
              <span className="legend-item">
                <span className="health-dot down"></span> Down
              </span>
              <span className="legend-item">
                <span className="health-dot unknown"></span> Unknown
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <Footer />
    </>
  );
}

  // If authenticated, show the dashboard
  return <DashboardPage />;
}

export default HomePage;
