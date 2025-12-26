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
    // In a real scenario, these would come from a junction table
    // For now, we'll show all modules as available for this product
    return modules;
  };

  const getPartsForModule = (module) => {
    // In a real scenario, these would come from a junction table
    // For now, we'll show all parts as available for this module
    return parts;
  };

  // If not authenticated, show login prompt with embedded login form
  if (!isAuthenticated) {
    return (
      <section className="home-page">
        {/* Welcome Header */}
        <div className="home-hero">
          <h2>Welcome to the LEGO Factory</h2>
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
        
        {/* Row 1: Intro Paragraph + Login Form side by side */}
        <div className="home-intro-section">
          <div className="home-intro-text">
            <p className="subtitle">
              A comprehensive digital manufacturing platform designed to
              streamline and coordinate complex production workflows across
              multiple interconnected factory stations.
            </p>
          </div>
          <div className="home-login-container">
            <LoginForm embedded={true} showHeader={false} showHelpText={true} />
          </div>
        </div>

        {/* Row 2: Product Variants */}
        <div className="home-content">
          <h3 style={{ textAlign: 'center', color: 'var(--color-primary)', marginBottom: 'var(--spacing-lg)' }}>
            Our Products
          </h3>
          {loadingProducts ? (
            <p style={{ textAlign: 'center' }}>Loading products...</p>
          ) : selectedProduct ? (
            <div className="product-details" style={{ maxWidth: '1200px', margin: '0 auto' }}>
              <button 
                className="btn-back" 
                onClick={() => setSelectedProduct(null)}
                style={{
                  backgroundColor: '#6c757d',
                  color: 'white',
                  border: 'none',
                  padding: '8px 16px',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  marginBottom: '15px',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                ← Back to Products
              </button>
              
              <div style={{
                backgroundColor: 'white',
                borderLeft: '4px solid #2c5aa0',
                padding: '15px',
                borderRadius: '4px',
                marginBottom: '20px'
              }}>
                <h2 style={{ color: '#2c5aa0', margin: '0 0 10px 0', fontSize: '22px' }}>
                  {selectedProduct.name}
                </h2>
                <p style={{ color: '#666', margin: '0 0 12px 0', fontSize: '14px' }}>
                  {selectedProduct.description}
                </p>
                <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                  <div style={{ fontSize: '13px', color: '#333' }}>
                    <strong style={{ color: '#2c5aa0', marginRight: '5px' }}>Price:</strong>
                    <span style={{ color: '#28a745', fontSize: '16px', fontWeight: 'bold' }}>
                      ${selectedProduct.price.toFixed(2)}
                    </span>
                  </div>
                  <div style={{ fontSize: '13px', color: '#333' }}>
                    <strong style={{ color: '#2c5aa0', marginRight: '5px' }}>Est. Time:</strong>
                    <span>{selectedProduct.estimatedTimeMinutes} minutes</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 style={{ color: '#2c5aa0', fontSize: '18px', margin: '0 0 15px 0', borderBottom: '2px solid #2c5aa0', paddingBottom: '8px' }}>
                  Product Components
                </h3>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {getModulePartsForProduct(selectedProduct).length === 0 ? (
                    <p style={{ textAlign: 'center', color: '#999', padding: '20px', fontStyle: 'italic' }}>
                      No components available for this product
                    </p>
                  ) : (
                    getModulePartsForProduct(selectedProduct).map(module => (
                      <div 
                        key={module.id} 
                        style={{
                          backgroundColor: 'white',
                          border: '1px solid #ddd',
                          borderRadius: '6px',
                          overflow: 'hidden'
                        }}
                      >
                        <div 
                          onClick={() => toggleModuleExpand(module.id)}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            padding: '12px',
                            cursor: 'pointer',
                            backgroundColor: expandedModules[module.id] ? '#f8f9fa' : 'white'
                          }}
                        >
                          <span style={{ marginRight: '10px', fontSize: '14px' }}>
                            {expandedModules[module.id] ? '▼' : '▶'}
                          </span>
                          <div>
                            <h4 style={{ margin: '0 0 4px 0', fontSize: '16px', color: '#2c5aa0' }}>
                              {module.name}
                            </h4>
                            <p style={{ margin: 0, fontSize: '12px', color: '#666' }}>
                              {module.type || 'COMPONENT'}
                            </p>
                          </div>
                        </div>

                        {expandedModules[module.id] && (
                          <div style={{ padding: '12px', backgroundColor: '#fafafa', borderTop: '1px solid #ddd' }}>
                            <p style={{ color: '#666', fontSize: '14px', marginBottom: '12px' }}>
                              {module.description}
                            </p>
                            
                            <div>
                              <h5 style={{ fontSize: '14px', color: '#2c5aa0', marginBottom: '8px' }}>
                                Parts Required:
                              </h5>
                              {getPartsForModule(module).length === 0 ? (
                                <p style={{ color: '#999', fontSize: '13px', fontStyle: 'italic' }}>
                                  No parts specified
                                </p>
                              ) : (
                                <ul style={{ margin: 0, paddingLeft: '20px' }}>
                                  {getPartsForModule(module).map(part => (
                                    <li 
                                      key={part.id} 
                                      style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        padding: '6px 0',
                                        fontSize: '13px',
                                        borderBottom: '1px solid #eee'
                                      }}
                                    >
                                      <span style={{ fontWeight: '500', color: '#333' }}>{part.name}</span>
                                      <span style={{ color: '#666', marginLeft: '10px' }}>{part.category}</span>
                                      <span style={{ color: '#28a745', fontWeight: '600', marginLeft: '10px' }}>
                                        ${part.unitCost?.toFixed(2) || '0.00'}
                                      </span>
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
            </div>
          ) : products.length === 0 ? (
            <p style={{ textAlign: 'center', color: '#999' }}>No products available</p>
          ) : (
            <div className="home-products-grid">
              {products.map(product => (
                <div 
                  key={product.id} 
                  className="home-product-card"
                  onClick={() => setSelectedProduct(product)}
                >
                  <h3>{product.name}</h3>
                  <p className="home-product-description">{product.description}</p>
                  <div className="home-product-specs">
                    <span className="price-badge">${product.price.toFixed(2)}</span>
                    <span className="time-badge">{product.estimatedTimeMinutes}m</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Row 3: Platform Features with New FeatureCard Component */}
        <div className="home-content">
          <h3 style={{ textAlign: 'center', color: 'var(--color-primary)', marginBottom: 'var(--spacing-lg)' }}>
            Platform Features
          </h3>
          <div className="feature-cards-container">
            <FeatureCard
              icon="🏭"
              title="Plant Warehouse"
              description="Manage customer orders and inventory at the plant warehouse distribution center."
            />
            <FeatureCard
              icon="🏢"
              title="Modules Supermarket"
              description="Organize module assembly and fulfill warehouse orders with pre-assembled modules."
            />
            <FeatureCard
              icon="📋"
              title="Production Planning"
              description="Plan and schedule production workflows across manufacturing and assembly stations."
            />
            <FeatureCard
              icon="🏭"
              title="Production Control"
              description="Monitor manufacturing workstations and track production progress in real-time."
            />
            <FeatureCard
              icon="⚙️"
              title="Assembly Control"
              description="Supervise assembly workstations including gear, motor, and final assembly stations."
            />
            <FeatureCard
              icon="📦"
              title="Parts Supply Warehouse"
              description="Supply parts to manufacturing and assembly stations on demand."
            />
            <FeatureCard
              icon="🔧"
              title="Manufacturing Stations"
              description="Execute manufacturing tasks at injection molding, pre-production, and finishing stations."
            />
            <FeatureCard
              icon="🔩"
              title="Assembly Stations"
              description="Execute assembly tasks for gear, motor, and final product assembly."
            />
          </div>
        </div>
      </section>
    );
  }

  // If authenticated, show the dashboard
  return <DashboardPage />;
}

export default HomePage;
