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
            <h3 className="section-title">Order Fulfillment Process</h3>
            
            {/* Redesigned Flow Diagram - Accurate Business Process */}
            <div className="flow-diagram-v2">
              
              {/* TOP ROW: Customer Entry & Exit */}
              <div className="flow-row-top">
                <WorkstationCard 
                  icon="🛒" 
                  name="Customer" 
                  tooltip="Customer places order for products | Entry point for all orders"
                  layout="horizontal"
                />
                <div className="flow-connector green right-arrow">
                  <div className="connector-line"></div>
                  <span className="connector-label">order</span>
                </div>
                <WorkstationCard 
                  icon="🏭" 
                  name={<>Plant<br/>Warehouse</>} 
                  tooltip="WS-7: Final product storage | Checks stock → fulfills or triggers production"
                  status="highlight"
                  layout="horizontal"
                />
                <div className="flow-connector green right-arrow">
                  <div className="connector-line"></div>
                  <span className="connector-label">ship</span>
                </div>
                <WorkstationCard 
                  icon="📦" 
                  name="Delivery" 
                  tooltip="Order shipped to customer | Scenario 1: Direct Fulfillment complete"
                  layout="horizontal"
                />
              </div>

              {/* VERTICAL CONNECTORS */}
              <div className="flow-vertical-section">
                <div className="flow-down-branch">
                  <div className="branch-label">if stock unavailable</div>
                  <div className="branch-arrow orange">↓</div>
                </div>
                <div className="flow-up-return">
                  <div className="return-arrow blue">↑</div>
                  <div className="return-label">products credited</div>
                </div>
              </div>

              {/* MIDDLE ROW: Warehouse & Assembly */}
              <div className="flow-row-middle">
                <WorkstationCard 
                  icon="🏢" 
                  name={<>Modules<br/>Supermarket</>} 
                  tooltip="WS-8: Module storage | Supplies modules for final assembly"
                  layout="horizontal"
                />
                <div className="flow-connector purple left-arrow">
                  <span className="connector-label">modules</span>
                  <div className="connector-line"></div>
                </div>
                <WorkstationCard 
                  icon="🔨" 
                  name={<>Final<br/>Assembly</>} 
                  tooltip="WS-6: Assembles finished products | Credits Plant Warehouse on completion"
                  layout="horizontal"
                />
              </div>

              {/* VERTICAL CONNECTORS - MIDDLE */}
              <div className="flow-vertical-section">
                <div className="flow-down-branch">
                  <div className="branch-label">if modules unavailable</div>
                  <div className="branch-arrow orange">↓</div>
                </div>
                <div className="flow-up-return">
                  <div className="return-arrow purple">↑</div>
                  <div className="return-label">assembled modules</div>
                </div>
              </div>

              {/* BOTTOM ROW: Planning & Production */}
              <div className="flow-row-bottom">
                <div className="flow-station-stack">
                  <WorkstationCard 
                    icon="📋" 
                    name={<>Production<br/>Planning</>} 
                    tooltip="Schedules production orders | Dispatches to workstations"
                    layout="horizontal"
                  />
                  <div className="stack-arrow purple">↓</div>
                  <WorkstationCard 
                    icon="🎛️" 
                    name="Control" 
                    tooltip="Production & Assembly Control | Coordinates manufacturing sequence"
                    layout="horizontal"
                  />
                </div>
                
                <div className="flow-connector-vertical orange">
                  <div className="connector-line-v"></div>
                  <span className="connector-label-v">dispatch<br/>orders</span>
                </div>

                <div className="flow-station-stack">
                  <WorkstationCard 
                    icon="📦" 
                    name={<>Parts<br/>Supply</>} 
                    tooltip="WS-9: Raw materials warehouse | Stages materials at workstations"
                    layout="horizontal"
                  />
                  <div className="stack-arrow green">↓</div>
                  <WorkstationCard 
                    icon="⚙️" 
                    name="Manufacturing" 
                    tooltip="WS-1,2,3: Injection Molding → Pre-Production → Finishing"
                    layout="horizontal"
                  />
                </div>

                <div className="flow-connector-vertical blue">
                  <div className="connector-line-v"></div>
                  <span className="connector-label-v">parts</span>
                </div>

                <div className="flow-station-stack single">
                  <WorkstationCard 
                    icon="🔧" 
                    name="Assembly" 
                    tooltip="WS-4,5: Gear Assembly → Motor Assembly | Creates modules"
                    layout="horizontal"
                  />
                </div>
              </div>

              {/* Legend */}
              <div className="flow-legend">
                <span className="legend-item"><span className="legend-dot green"></span>Order/Material</span>
                <span className="legend-item"><span className="legend-dot blue"></span>Product Return</span>
                <span className="legend-item"><span className="legend-dot orange"></span>Trigger/Dispatch</span>
                <span className="legend-item"><span className="legend-dot purple"></span>Module Flow</span>
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
            <h3 className="section-title">🏗️ Microservice Communication</h3>
            <div className="architecture-diagram">
              {/* Frontend Layer */}
              <div className="service-group frontend-group">
                <div className="group-label">Frontend Layer</div>
                <div className="arch-layer">
                  <div 
                    className={`service-box frontend ${serviceHealth['frontend'] || 'unknown'}`}
                    title="React SPA - User interface with real-time updates, role-based dashboards, order management, and inventory tracking. Built with Vite and served via Nginx."
                  >
                    <span className="service-name">React App</span>
                    <span className="service-tech">UI Layer</span>
                  </div>
                </div>
              </div>
              
              <div className="arch-arrow-down">▼</div>
              
              {/* API Gateway Layer */}
              <div className="arch-layer">
                <div 
                  className={`service-box gateway ${serviceHealth['api-gateway'] || 'unknown'}`}
                  title="API Gateway - Central routing hub with JWT authentication, request filtering, and load balancing. Routes all frontend requests to backend microservices."
                >
                  <span className="service-name">API Gateway</span>
                  <span className="service-tech">Auth & Routing</span>
                  <span className={`health-indicator ${serviceHealth['api-gateway'] || 'unknown'}`}></span>
                </div>
              </div>
              
              <div className="arch-arrow-down">▼</div>
              
              {/* Backend Services Layer - Show Gateway Communication */}
              <div className="service-group backend-group">
                <div className="group-label">Backend Services</div>
                
                {/* Primary Services (Gateway Routes) */}
                <div className="arch-layer services-primary">
                  <div 
                    className={`service-box service-user ${serviceHealth['user-service'] || 'unknown'}`}
                    title="User Service - Authentication & authorization with JWT tokens, user management, role-based access control (9 roles), and workstation assignments."
                  >
                    <span className="service-name">User</span>
                    <span className={`health-indicator ${serviceHealth['user-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div 
                    className={`service-box service-order ${serviceHealth['order-processing-service'] || 'unknown'}`}
                    title="Order Processing - Customer orders, warehouse orders, production orders, fulfillment workflows, order state management, and production scheduling integration."
                  >
                    <span className="service-name">Order</span>
                    <span className={`health-indicator ${serviceHealth['order-processing-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div 
                    className={`service-box service-simal ${serviceHealth['simal-integration-service'] || 'unknown'}`}
                    title="SimAL Integration - Production scheduling with Gantt charts, manufacturing timeline optimization, control order generation, and real-time schedule adjustments."
                  >
                    <span className="service-name">SimAL</span>
                    <span className={`health-indicator ${serviceHealth['simal-integration-service'] || 'unknown'}`}></span>
                  </div>
                </div>
                
                <div className="service-communication-arrows">
                  <div className="comm-arrow">Inter-service Communication</div>
                </div>
                
                {/* Secondary Services (Called by other services) */}
                <div className="arch-layer services-secondary">
                  <div 
                    className={`service-box service-masterdata ${serviceHealth['masterdata-service'] || 'unknown'}`}
                    title="Masterdata Service - Product variants, modules, parts catalog, bill-of-materials (BOM), workstation definitions, and manufacturing hierarchies."
                  >
                    <span className="service-name">Masterdata</span>
                    <span className={`health-indicator ${serviceHealth['masterdata-service'] || 'unknown'}`}></span>
                  </div>
                  
                  <div 
                    className={`service-box service-inventory ${serviceHealth['inventory-service'] || 'unknown'}`}
                    title="Inventory Service - Real-time stock tracking across 9 workstations, stock ledger audit trail, low stock alerts, and inventory transactions (credit/debit/transfer)."
                  >
                    <span className="service-name">Inventory</span>
                    <span className={`health-indicator ${serviceHealth['inventory-service'] || 'unknown'}`}></span>
                  </div>
                </div>
                
                <div className="arch-note">
                  <span className="db-icon">💾</span> Independent H2 Databases
                  <span className="note-separator">•</span>
                  <span className="comm-icon">📡</span> Docker DNS Communication
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
