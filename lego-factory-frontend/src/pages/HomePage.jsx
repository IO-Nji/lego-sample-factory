import { createPortal } from "react-dom";
import DashboardPage from "./DashboardPage";
import LoginForm from "../components/LoginForm";
import FeatureCard from "../components/FeatureCard";
import { WorkstationCard } from "../components";
import Footer from "../components/Footer";
import { useAuth } from "../context/AuthContext.jsx";
import api from "../api/api";
import "../styles/StandardPage.css";
import "../styles/homepage/index.css"; // Modular CSS - replaces monolithic HomePage.css
import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

function HomePage() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [message, setMessage] = useState("");
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [expandedModules, setExpandedModules] = useState({});
  const [expandedBomModules, setExpandedBomModules] = useState({}); // Track which modules in BOM are expanded
  const [productModules, setProductModules] = useState([]);
  const [loadingModules, setLoadingModules] = useState(false);
  const [serviceHealth, setServiceHealth] = useState({});

  // Toggle module expansion within BOM
  const toggleBomModule = (moduleId) => {
    setExpandedBomModules(prev => ({
      ...prev,
      [moduleId]: !prev[moduleId]
    }));
  };

  // Product icon mapping for realistic visuals
  const getProductIcon = (name) => {
    const nameLower = name.toLowerCase();
    // LEGO Technic Trucks
    if (nameLower.includes('truck') && nameLower.includes('yellow')) 
      return { icon: '🚛', color: '#ffc107' };
    if (nameLower.includes('truck') && nameLower.includes('red')) 
      return { icon: '🚚', color: '#e53935' };
    if (nameLower.includes('truck')) 
      return { icon: '🛻', color: '#ff8f00' };
    // Creator House
    if (nameLower.includes('house') || nameLower.includes('home') || nameLower.includes('creator')) 
      return { icon: '🏠', color: '#4caf50' };
    // Castle Set
    if (nameLower.includes('castle') || nameLower.includes('medieval')) 
      return { icon: '🏰', color: '#7b1fa2' };
    // Other vehicles
    if (nameLower.includes('race') || nameLower.includes('sports')) 
      return { icon: '🏎️', color: '#e53935' };
    if (nameLower.includes('car')) 
      return { icon: '🚘', color: '#1976d2' };
    if (nameLower.includes('plane') || nameLower.includes('aircraft')) 
      return { icon: '✈️', color: '#0288d1' };
    if (nameLower.includes('helicopter')) 
      return { icon: '🚁', color: '#00bcd4' };
    if (nameLower.includes('boat') || nameLower.includes('ship')) 
      return { icon: '🚢', color: '#00796b' };
    if (nameLower.includes('motorcycle') || nameLower.includes('bike')) 
      return { icon: '🏍️', color: '#d32f2f' };
    return { icon: '🧱', color: '#2c5aa0' };
  };

  // Handle BOM toggle for a product
  const handleBomToggle = async (product) => {
    const isCurrentlyExpanded = expandedModules[product.id];
    
    if (isCurrentlyExpanded) {
      // Collapse
      setExpandedModules(prev => ({ ...prev, [product.id]: false }));
      setProductModules([]);
      setExpandedBomModules({}); // Reset expanded modules within BOM
    } else {
      // Expand and fetch modules with their parts
      setExpandedModules({ [product.id]: true }); // Only one expanded at a time
      setExpandedBomModules({}); // Reset expanded modules within BOM
      setLoadingModules(true);
      try {
        const modulesResponse = await api.get(`/masterdata/products/${product.id}/modules`);
        const modulesData = modulesResponse.data || [];
        
        // Fetch parts for each module - API returns componentId, componentName
        const modulesWithParts = await Promise.all(
          modulesData.map(async (module) => {
            try {
              // Use componentId to fetch parts for this module
              const partsResponse = await api.get(`/masterdata/modules/${module.componentId}/parts`);
              const partsData = partsResponse.data || [];
              // Map API response fields to expected names
              return {
                id: module.componentId,
                name: module.componentName,
                quantity: module.quantity,
                parts: partsData.map(part => ({
                  id: part.componentId,
                  name: part.componentName,
                  quantity: part.quantity
                }))
              };
            } catch (err) {
              console.error(`Failed to fetch parts for module ${module.componentId}:`, err);
              return {
                id: module.componentId,
                name: module.componentName,
                quantity: module.quantity,
                parts: []
              };
            }
          })
        );
        
        setProductModules(modulesWithParts);
      } catch (error) {
        console.error('Failed to fetch product modules:', error);
        setProductModules([]);
      } finally {
        setLoadingModules(false);
      }
    }
  };

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

  // Fetch products for display
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const productsRes = await api.get('/masterdata/products');
        setProducts(productsRes.data || []);
      } catch (err) {
        console.error('Error fetching products:', err);
      } finally {
        setLoadingProducts(false);
      }
    };
    
    if (!isAuthenticated) {
      fetchProducts();
    } else {
      setLoadingProducts(false); // Don't leave loading state when authenticated
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
        // Fetch product modules
        const modulesResponse = await api.get(`/masterdata/products/${selectedProduct.id}/modules`);
        const productModules = modulesResponse.data;
        
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
      <div>
      <section className="home-page">
        {/* Welcome Header */}
        <div className="home-hero">
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
        
        {/* ================================================================
            ROW 1: L.I.F.E Title | Application Overview | Login Form
            ================================================================ */}
        <div className="home-overview-row">
          {/* Column 1: L.I.F.E Title Card */}
          <div className="home-tagline-column">
            <div className="glass-card life-title-card">
              <div className="life-title-row">
                <h1 className="life-title">
                  <span className="title-letter" style={{'--i': 0}}>L</span>
                  <span className="title-dot">.</span>
                  <span className="title-letter" style={{'--i': 1}}>I</span>
                  <span className="title-dot">.</span>
                  <span className="title-letter" style={{'--i': 2}}>F</span>
                  <span className="title-dot">.</span>
                  <span className="title-letter" style={{'--i': 3}}>E</span>
                </h1>
                <div className="life-version-badge">
                  <span className="version-label">MES</span>
                  <span className="version-number">v2.0</span>
                </div>
              </div>
              <p className="life-subtitle">LEGO INTEGRATED FACTORY EXECUTION</p>
              <h2 className="life-industry-title">Industry 4.0 Digital Manufacturing</h2>
              <p className="life-slogan">
                Enterprise-grade Manufacturing Execution System demonstrating 
                smart factory automation, real-time production orchestration, 
                and intelligent supply chain management.
              </p>
            </div>
          </div>

          {/* Column 2: Application Overview Card - Redesigned */}
          <div className="home-overview-column">
            <div className="overview-card-v2">
              <div className="overview-header">
                <div className="overview-icon">🏭</div>
                <div className="overview-title-block">
                  <h3>Application Overview</h3>
                  <span className="overview-badge">Enterprise MES Platform</span>
                </div>
              </div>
              <div className="overview-body">
                <p>The <strong>LIFE System</strong> (LEGO Integrated Factory Execution) is an enterprise-grade digital manufacturing execution system built to demonstrate academic research in industrial engineering. This platform digitizes end-to-end supply chain operations across <strong>nine autonomous workstations</strong>, coordinating complex production workflows from raw materials to finished products. Supporting <strong>four distinct business scenarios</strong>, it handles direct fulfillment, warehouse replenishment, full production cycles, and high-volume batch processing with real-time inventory tracking.</p>
                <p>Login with username <strong>lego_admin</strong> to access the Admin Dashboard for a complete overview of system state and operations.</p>
              </div>
              <div className="overview-metrics">
                <div className="metric-box">
                  <span className="metric-num">6</span>
                  <span className="metric-lbl">Microservices</span>
                </div>
                <div className="metric-box">
                  <span className="metric-num">9</span>
                  <span className="metric-lbl">Workstations</span>
                </div>
                <div className="metric-box">
                  <span className="metric-num">4</span>
                  <span className="metric-lbl">Scenarios</span>
                </div>
                <div className="metric-box">
                  <span className="metric-num">9</span>
                  <span className="metric-lbl">User Roles</span>
                </div>
              </div>
              <div className="overview-tech">
                <span className="tech-pill">⚛️ React</span>
                <span className="tech-pill">🍃 Spring</span>
                <span className="tech-pill">🐳 Docker</span>
                <span className="tech-pill">🔐 JWT</span>
                <span className="tech-pill">📡 REST</span>
              </div>
            </div>
          </div>

          {/* Column 3: Login Form */}
          <div className="home-login-column">
            <LoginForm embedded={true} showHeader={true} showHelpText={true} />
          </div>
        </div>

        {/* ================================================================
            ROW 2: Products | Order Fulfillment Flow | Microservices Architecture
            ================================================================ */}
        <div className="home-features-row">
          {/* Column 1: Products - Portrait Cards */}
          <div className="home-products-column">
            <div className="glass-card products-column-card">
              <h3 className="section-title">🧱 Products</h3>
              {loadingProducts ? (
                <p className="loading-text">Loading products...</p>
              ) : products.length === 0 ? (
                <p className="no-data">No products available</p>
              ) : (
                <div className="products-grid-portrait">
                  {products.map(product => {
                    const productIcon = getProductIcon(product.name);
                    
                    return (
                      <div key={product.id} className="product-card-portrait">
                        <div className="product-card-portrait-inner">
                          <div className="product-portrait-icon">
                            <span className="portrait-emoji">{productIcon.icon}</span>
                            <span className="portrait-color-dot" style={{background: productIcon.color}}></span>
                          </div>
                          <div className="product-portrait-info">
                            <h4 className="portrait-name">{product.name}</h4>
                            <span className="portrait-sku">SKU: PRD-{String(product.id).padStart(3, '0')}</span>
                          </div>
                          <button 
                            className="view-components-btn"
                            onClick={() => handleBomToggle(product)}
                            title="View Bill of Materials"
                          >
                            📋 Components
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
              
              {/* BOM Modal - Rendered via portal to document.body for proper layering */}
              {Object.keys(expandedModules).some(key => expandedModules[key]) && createPortal(
                <div className="bom-modal-overlay" onClick={() => {
                  setExpandedModules({});
                  setProductModules([]);
                  setExpandedBomModules({});
                }}>
                  <div className="bom-modal" onClick={(e) => e.stopPropagation()}>
                    <div className="bom-modal-header">
                      <div className="bom-modal-title">
                        <span className="bom-modal-icon">🛠️</span>
                        <div className="bom-modal-title-text">
                          <h3>Bill of Materials</h3>
                          <span className="bom-modal-product">
                            {products.find(p => expandedModules[p.id])?.name || 'Product'}
                          </span>
                        </div>
                      </div>
                      <button 
                        className="bom-modal-close"
                        onClick={() => {
                          setExpandedModules({});
                          setProductModules([]);
                          setExpandedBomModules({});
                        }}
                      >
                        ✕
                      </button>
                    </div>
                    
                    <div className="bom-modal-body">
                      {loadingModules ? (
                        <div className="bom-modal-loading">
                          <span className="loading-spinner">⏳</span>
                          <span>Loading components...</span>
                        </div>
                      ) : productModules.length > 0 ? (
                        <div className="bom-modal-modules">
                          {productModules.map(module => {
                            const isModuleExpanded = expandedBomModules[module.id];
                            const hasParts = module.parts && module.parts.length > 0;
                            
                            return (
                              <div key={module.id} className="bom-modal-module-group">
                                <div 
                                  className={`bom-modal-module ${hasParts ? 'expandable' : ''} ${isModuleExpanded ? 'expanded' : ''}`}
                                  onClick={() => hasParts && toggleBomModule(module.id)}
                                >
                                  <span className="module-icon-large">⚙️</span>
                                  <div className="module-info">
                                    <span className="module-name-large">{module.name}</span>
                                    <span className="module-meta">
                                      Qty: {module.quantity || 1} • {module.parts?.length || 0} parts
                                    </span>
                                  </div>
                                  {hasParts && (
                                    <span className="module-expand-icon">
                                      {isModuleExpanded ? '▼' : '▶'}
                                    </span>
                                  )}
                                </div>
                                
                                {hasParts && isModuleExpanded && (
                                  <div className="bom-modal-parts">
                                    <div className="parts-header">
                                      <span className="parts-col">Part</span>
                                      <span className="parts-col-qty">Qty</span>
                                    </div>
                                    {module.parts.map(part => (
                                      <div key={part.id} className="bom-modal-part">
                                        <div className="part-info">
                                          <span className="part-icon-small">🔩</span>
                                          <span className="part-name-large">{part.name}</span>
                                        </div>
                                        <span className="part-qty-large">×{part.quantity || 1}</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      ) : (
                        <div className="bom-modal-empty">
                          <span className="empty-icon">📦</span>
                          <span>No components found for this product</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>,
                document.body
              )}
            </div>
          </div>

          {/* Column 2: Flow Diagram */}
          <div className="home-flow-column">
            <div className="glass-card">
              <h3 className="section-title">🔄 Order Fulfillment Process</h3>
            
            {/* Enhanced Flow Diagram - Hierarchical Order Layers */}
            <div className="flow-diagram-v3">
              
              {/* LAYER 1: Customer Order Level */}
              <div className="flow-layer customer-layer">
                <div className="layer-header">
                  <span className="layer-badge customer">📋 Customer Order</span>
                  <span className="layer-subtitle">Order Entry & Fulfillment</span>
                </div>
                <div className="layer-content-stacked">
                  {/* Main horizontal flow: Customer → Plant Warehouse → Ship */}
                  <div className="layer-row main-flow">
                    <div className="flow-node-enhanced customer-node">
                      <span className="node-icon">👤</span>
                      <span className="node-label">Customer</span>
                    </div>
                    <div className="flow-connector-enhanced">
                      <span className="connector-line"></span>
                      <span className="connector-label">Places Order</span>
                      <span className="connector-arrow">→</span>
                    </div>
                    <div className="warehouse-decision-wrapper">
                      <div className="flow-node-enhanced warehouse-node" data-tooltip="Receives customer orders and checks product stock availability&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 warehouse_operator">
                        <span className="node-icon">🏭</span>
                        <span className="node-label">Plant Warehouse</span>
                      </div>
                      {/* Escalation path below Plant Warehouse */}
                      <div className="branch-path escalate vertical-branch">
                        <span className="path-label">✗ Insufficient</span>
                        <span className="path-arrow">↓</span>
                        <span className="scenario-indicator s2-4">S2-4</span>
                      </div>
                    </div>
                    <div className="flow-connector-enhanced success-path">
                      <span className="connector-line success"></span>
                      <span className="connector-label">✓ In Stock</span>
                      <span className="connector-arrow">→</span>
                    </div>
                    <div className="flow-node-enhanced delivery-node">
                      <span className="node-icon">🚚</span>
                      <span className="node-label">Ship</span>
                    </div>
                    <span className="scenario-indicator s1">S1</span>
                  </div>
                </div>
              </div>

              {/* Arrow: Customer → Warehouse Order */}
              <div className="layer-connector">
                <div className="connector-vertical">
                  <span className="connector-line-v"></span>
                  <span className="connector-text">Creates</span>
                </div>
              </div>

              {/* LAYER 2: Warehouse Order Level */}
              <div className="flow-layer warehouse-layer">
                <div className="layer-header">
                  <span className="layer-badge warehouse">📦 Warehouse Order</span>
                  <span className="layer-subtitle">Module Assembly</span>
                </div>
                <div className="layer-content-stacked">
                  {/* Main horizontal flow: Modules Supermarket → Final Assembly → Plant Warehouse */}
                  <div className="layer-row main-flow">
                    <div className="modules-decision-wrapper">
                      <div className="flow-node-enhanced modules-node" data-tooltip="Stores pre-assembled modules and fulfills internal warehouse orders&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 modules_supermarket">
                        <span className="node-icon">🏢</span>
                        <span className="node-label">Modules Supermarket</span>
                        <span className="node-sublabel">Module Check</span>
                      </div>
                      {/* Escalation path below Modules Supermarket */}
                      <div className="branch-path production-needed vertical-branch">
                        <span className="path-label">✗ Need Production</span>
                        <span className="path-arrow">↓</span>
                        <span className="scenario-indicator s3-4">S3/S4</span>
                      </div>
                    </div>
                    <div className="flow-connector-enhanced success-path">
                      <span className="connector-line success"></span>
                      <span className="connector-label">✓ Modules Available</span>
                      <span className="connector-arrow">→</span>
                    </div>
                    <div className="assembly-output-wrapper">
                      <div className="return-indicator-top">
                        <span className="return-arrow">↑</span>
                        <span className="return-text">to Plant Warehouse</span>
                        <span className="scenario-indicator s2">S2</span>
                      </div>
                      <div className="flow-node-enhanced assembly-node" data-tooltip="Combines modules into finished products ready for shipment&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 final_assembly">
                        <span className="node-icon">🔨</span>
                        <span className="node-label">Final Assembly</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Arrow: Warehouse → Production Order */}
              <div className="layer-connector">
                <div className="connector-vertical">
                  <span className="connector-line-v"></span>
                  <span className="connector-text">Triggers</span>
                </div>
              </div>

              {/* LAYER 3: Production Order Level */}
              <div className="flow-layer production-layer">
                <div className="layer-header">
                  <span className="layer-badge production">⚙️ Production Order</span>
                  <span className="layer-subtitle">Full Manufacturing Pipeline</span>
                </div>
                <div className="layer-content production-pipeline-v2">
                  {/* Planning Stage */}
                  <div className="pipeline-stage-v2 planning" data-tooltip="Schedules production orders and optimizes manufacturing sequences&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 production_planning">
                    <span className="stage-icon">📋</span>
                    <span className="stage-label">Planning</span>
                  </div>
                  <span className="pipeline-arrow-v2">→</span>

                  {/* Control Stage */}
                  <div className="pipeline-stage-v2 control" data-tooltip="Coordinates and monitors production and assembly operations&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 production_control&#10;👤 assembly_control">
                    <span className="stage-icon">🎛️</span>
                    <span className="stage-label">Control</span>
                  </div>
                  <span className="pipeline-arrow-v2">→</span>
                  
                  {/* Parts Supply */}
                  <div className="pipeline-stage-v2 supply" data-tooltip="Distributes raw materials and components to workstations&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 parts_supply">
                    <span className="stage-icon">📦</span>
                    <span className="stage-label">Parts Supply</span>
                  </div>
                  <span className="pipeline-arrow-v2">→</span>

                  {/* Manufacturing Stage */}
                  <div className="pipeline-stage-v2 manufacturing" data-tooltip="Produces parts through injection molding, pre-production, and finishing&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 injection_molding&#10;👤 parts_preproduction&#10;👤 part_finishing">
                    <span className="stage-icon">🔧</span>
                    <span className="stage-label">Manufacturing</span>
                    <span className="stage-output">→ Parts</span>
                  </div>
                  <span className="pipeline-arrow-v2">→</span>

                  {/* Assembly Stage */}
                  <div className="pipeline-stage-v2 assembly" data-tooltip="Assembles gear and motor modules from manufactured parts&#10;━━━━━━━━━━━━━━━━━━━━━━&#10;👤 gear_assembly&#10;👤 motor_assembly">
                    <span className="stage-icon">⚙️</span>
                    <span className="stage-label">Assembly</span>
                    <span className="stage-output">→ Modules</span>
                  </div>
                </div>
                
                {/* Output flows */}
                <div className="production-outputs">
                  <div className="output-path">
                    <span className="output-arrow">↑</span>
                    <span className="output-label">Modules → Supermarket</span>
                    <span className="scenario-indicator s3">S3</span>
                  </div>
                  <div className="output-path direct">
                    <span className="output-arrow">↑↑</span>
                    <span className="output-label">Modules → Final Assembly</span>
                    <span className="scenario-indicator s4">S4</span>
                  </div>
                </div>
              </div>

              {/* Scenario Legend */}
              <div className="scenario-legend-v2">
                <div className="legend-title">📊 Business Scenarios</div>
                <div className="legend-grid">
                  <div className="legend-item">
                    <span className="scenario-tag s1">S1</span>
                    <span className="legend-text">Direct Fulfillment</span>
                  </div>
                  <div className="legend-item">
                    <span className="scenario-tag s2">S2</span>
                    <span className="legend-text">Warehouse + Assembly</span>
                  </div>
                  <div className="legend-item">
                    <span className="scenario-tag s3">S3</span>
                    <span className="legend-text">Full Production</span>
                  </div>
                  <div className="legend-item">
                    <span className="scenario-tag s4">S4</span>
                    <span className="legend-text">High Volume Direct</span>
                  </div>
                </div>
              </div>
            </div>
            </div>
          </div>
          
          {/* Column 2: Microservices Architecture - Layered Layout */}
          <div className="home-architecture-column">
            <div className="glass-card">
              <h3 className="section-title">🏗️ Microservice Architecture</h3>
              <div className="architecture-layered">
                
                {/* Layer 1: Client/Frontend */}
                <div className="arch-layer client-layer">
                  <div className="layer-services with-label">
                    <div className="layer-label-inline">Client</div>
                    <div className="service-box-horizontal frontend" data-tooltip="React 18 + Vite SPA | Port: 5173">
                      <span className="service-icon">⚛️</span>
                      <span className="service-name-vertical">React<br/>Frontend</span>
                      <span className={`health-indicator ${serviceHealth['frontend'] || 'unknown'}`}></span>
                    </div>
                  </div>
                </div>

                {/* Animated Arrow: Client → Gateway */}
                <div className="arch-flow-arrow">
                  <div className="arrow-line animated-pulse"></div>
                  <div className="arrow-head">▼</div>
                  <span className="arrow-label">HTTP/REST</span>
                </div>

                {/* Layer 2: API Gateway */}
                <div className="arch-layer gateway-layer">
                  <div className="layer-services with-label">
                    <div className="layer-label-inline">Gateway</div>
                    <div className="service-box-horizontal gateway" data-tooltip="Spring Cloud Gateway | Auth & Routing | Port: 8011">
                      <span className="service-icon">🚪</span>
                      <span className="service-name-vertical">API<br/>Gateway</span>
                      <span className={`health-indicator ${serviceHealth['api-gateway'] || 'unknown'}`}></span>
                    </div>
                  </div>
                </div>

                {/* Animated Arrow: Gateway → Services (Fan-out) */}
                <div className="arch-flow-arrow fanout">
                  <div className="arrow-branches">
                    <div className="branch-line left"></div>
                    <div className="branch-line center"></div>
                    <div className="branch-line right"></div>
                  </div>
                  <div className="arrow-heads">
                    <span className="arrow-head">▼</span>
                    <span className="arrow-head">▼</span>
                    <span className="arrow-head">▼</span>
                  </div>
                  <span className="arrow-label">Route & Auth</span>
                </div>

                {/* Layer 3: Backend Microservices - Multi-row Layout */}
                <div className="arch-layer services-layer">
                  
                  {/* Services Grid with Communication Lines */}
                  <div className="services-grid-creative">
                    
                    {/* Row 1: Auth Service (standalone) */}
                    <div className="services-row-auth with-label">
                      <div className="layer-label-inline">Microservices</div>
                      <div className="service-box-horizontal core" data-tooltip="Authentication & Authorization | Port: 8012">
                        <span className="service-icon">👤</span>
                        <span className="service-name-vertical">User<br/>Service</span>
                        <span className={`health-indicator ${serviceHealth['user-service'] || 'unknown'}`}></span>
                      </div>
                    </div>
                    
                    {/* Animated connection: Auth → Order */}
                    <div className="service-connector vertical-connector">
                      <div className="connector-line-animated"></div>
                      <span className="connector-label">JWT</span>
                    </div>
                    
                    {/* Row 2: Central Orchestrator + Integration */}
                    <div className="services-row-core">
                      <div className="service-box-horizontal core orchestrator" data-tooltip="Order Processing Engine | Port: 8015">
                        <span className="service-icon">📋</span>
                        <span className="service-name-vertical">Order<br/>Service</span>
                        <span className={`health-indicator ${serviceHealth['order-processing-service'] || 'unknown'}`}></span>
                      </div>
                      
                      {/* Horizontal connector to SimAL */}
                      <div className="service-connector horizontal-connector">
                        <div className="connector-line-animated horizontal"></div>
                        <span className="connector-label">Schedule</span>
                      </div>
                      
                      <div className="service-box-horizontal integration" data-tooltip="SimAL Scheduling & Optimization | Port: 8016">
                        <span className="service-icon">📊</span>
                        <span className="service-name-vertical">SimAL<br/>Service</span>
                        <span className={`health-indicator ${serviceHealth['simal-integration-service'] || 'unknown'}`}></span>
                      </div>
                    </div>
                    
                    {/* Animated fan-out: Order → Data Services */}
                    <div className="service-connector fanout-connector">
                      <div className="fanout-lines">
                        <div className="fanout-branch left"></div>
                        <div className="fanout-branch right"></div>
                      </div>
                      <div className="fanout-labels">
                        <span className="connector-label">BOM</span>
                        <span className="connector-label">Stock</span>
                      </div>
                    </div>
                    
                    {/* Row 3: Data Services */}
                    <div className="services-row-data">
                      <div className="service-box-horizontal data" data-tooltip="Product Catalog & BOM | Port: 8013">
                        <span className="service-icon">📦</span>
                        <span className="service-name-vertical">Master<br/>Data</span>
                        <span className={`health-indicator ${serviceHealth['masterdata-service'] || 'unknown'}`}></span>
                      </div>
                      
                      <div className="service-box-horizontal data" data-tooltip="Stock Management | Port: 8014">
                        <span className="service-icon">📈</span>
                        <span className="service-name-vertical">Inventory<br/>Service</span>
                        <span className={`health-indicator ${serviceHealth['inventory-service'] || 'unknown'}`}></span>
                      </div>
                    </div>
                    
                  </div>
                </div>

                {/* Architecture Legend */}
                <div className="architecture-legend-horizontal">
                  <div className="legend-item">
                    <span className="legend-color frontend"></span>
                    <span>Frontend</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-color gateway"></span>
                    <span>Gateway</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-color core"></span>
                    <span>Core</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-color data"></span>
                    <span>Data</span>
                  </div>
                  <div className="legend-item">
                    <span className="legend-color integration"></span>
                    <span>Integration</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* ================================================================
            ROW 3: Quick Navigation Guide | Feature Highlights
            ================================================================ */}
        <div className="home-guide-row">
          {/* Column 1: Navigation Guide */}
          <div className="home-navigation-column">
            <div className="glass-card">
              <h3 className="section-title">📋 Quick Start Guide</h3>
              <div className="nav-grid">
                <div className="navigation-item">
                  <span className="nav-icon">🔑</span>
                  <div className="nav-content">
                    <strong>Login</strong>
                    <p>Hover over workstation cards to see usernames. Password: <code>password</code></p>
                  </div>
                </div>
                
                <div className="navigation-item">
                  <span className="nav-icon">👥</span>
                  <div className="nav-content">
                    <strong>User Roles</strong>
                    <p>9 specialized roles with dedicated dashboards for each function</p>
                  </div>
                </div>
                
                <div className="navigation-item">
                  <span className="nav-icon">📦</span>
                  <div className="nav-content">
                    <strong>Operations</strong>
                    <p>Customer orders, production planning, inventory, and supply chain</p>
                  </div>
                </div>
                
                <div className="navigation-item">
                  <span className="nav-icon">🔄</span>
                  <div className="nav-content">
                    <strong>Real-time</strong>
                    <p>Dashboard auto-refresh every 5-30 seconds depending on view</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Column 2: Feature Highlights */}
          <div className="home-highlights-column">
            <div className="glass-card">
              <h3 className="section-title">✨ Feature Highlights</h3>
              <div className="highlights-grid">
                <div className="feature-highlight-card">
                  <div className="feature-highlight-icon">🏗️</div>
                  <div className="feature-highlight-content">
                    <h4 className="feature-highlight-title">Microservices</h4>
                    <p className="feature-highlight-description">6 Spring Boot services with isolated databases</p>
                  </div>
                </div>
                
                <div className="feature-highlight-card">
                  <div className="feature-highlight-icon">🔐</div>
                  <div className="feature-highlight-content">
                    <h4 className="feature-highlight-title">Enterprise Security</h4>
                    <p className="feature-highlight-description">JWT authentication with 9-role RBAC</p>
                  </div>
                </div>
                
                <div className="feature-highlight-card">
                  <div className="feature-highlight-icon">⚡</div>
                  <div className="feature-highlight-content">
                    <h4 className="feature-highlight-title">Modern Stack</h4>
                    <p className="feature-highlight-description">Java 21, Spring Boot 3.4, React 18, Vite</p>
                  </div>
                </div>
                
                <div className="feature-highlight-card">
                  <div className="feature-highlight-icon">📊</div>
                  <div className="feature-highlight-content">
                    <h4 className="feature-highlight-title">SimAL Integration</h4>
                    <p className="feature-highlight-description">Interactive Gantt charts for scheduling</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <Footer />
    </div>
  );
  }

  // If authenticated, show the dashboard
  return <DashboardPage />;
}

export default HomePage;
