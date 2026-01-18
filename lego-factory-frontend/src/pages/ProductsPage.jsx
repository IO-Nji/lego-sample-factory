import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/api';
import '../styles/StandardPage.css';
import '../styles/ProductsPage.css';

export default function ProductsPage() {
  const navigate = useNavigate();
  const { session } = useAuth();
  const [products, setProducts] = useState([]);
  const [productComposition, setProductComposition] = useState(null);
  const [loading, setLoading] = useState(true);
  const [compositionLoading, setCompositionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [expandedModules, setExpandedModules] = useState({});

  useEffect(() => {
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    try {
      setLoading(true);
      setError(null);
      const productsRes = await api.get('/masterdata/product-variants');
      setProducts(productsRes.data);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError(err.response?.data?.message || 'Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const fetchProductComposition = async (productId) => {
    try {
      setCompositionLoading(true);
      const response = await api.get(`/masterdata/product-variants/${productId}/composition`);
      setProductComposition(response.data);
    } catch (err) {
      console.error('Error fetching product composition:', err);
      setError('Failed to load product composition');
    } finally {
      setCompositionLoading(false);
    }
  };

  const handleProductSelect = async (product) => {
    setSelectedProduct(product);
    setProductComposition(null);
    setExpandedModules({});
    await fetchProductComposition(product.id);
  };

  const toggleModuleExpand = (moduleId) => {
    setExpandedModules(prev => ({
      ...prev,
      [moduleId]: !prev[moduleId]
    }));
  };

  if (loading) {
    return (
      <div style={{ padding: '20px', textAlign: 'center' }}>
        <p>Loading products...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '20px', color: 'red' }}>
        <p>Error: {error}</p>
        <button onClick={fetchAllData}>Retry</button>
      </div>
    );
  }

  return (
    <div className="products-container">
      <div className="standard-page-header">
        <div className="standard-header-content">
          <h1>Products Catalog</h1>
          <p className="standard-page-subtitle">LEGO Product Variants: {products.length}</p>
        </div>
        {session?.user?.role === 'ADMIN' && (
          <div className="standard-header-actions">
            <button 
              className="standard-action-btn"
              onClick={() => navigate('/admin-dashboard?tab=products')}
              title="Manage product variants"
            >
              🛠️ Manage Variants
            </button>
            <button 
              className="standard-action-btn"
              onClick={() => navigate('/admin-dashboard')}
              title="Go to admin dashboard"
            >
              📊 Admin Dashboard
            </button>
          </div>
        )}
      </div>

      {selectedProduct ? (
        <div className="product-details">
          <button className="btn-back" onClick={() => setSelectedProduct(null)}>← Back to Products</button>
          
          <div className="product-info">
            <h2>{selectedProduct.name}</h2>
            <p className="product-desc">{selectedProduct.description}</p>
            <div className="product-specs">
              <div className="spec-item">
                <strong>Price:</strong> <span className="price">${selectedProduct.price.toFixed(2)}</span>
              </div>
              <div className="spec-item">
                <strong>Est. Time:</strong> <span>{selectedProduct.estimatedTimeMinutes} minutes</span>
              </div>
            </div>
          </div>

          <div className="components-section">
            <h3>Product Components</h3>
            
            {compositionLoading ? (
              <p className="loading-message">Loading components...</p>
            ) : !productComposition ? (
              <p className="empty-message">No composition data available</p>
            ) : (
              <div className="modules-list">
                {productComposition.modules.length === 0 ? (
                  <p className="empty-message">No components available for this product</p>
                ) : (
                  productComposition.modules.map(module => (
                    <div key={module.id} className="module-card">
                      <div 
                        className="module-header"
                        onClick={() => toggleModuleExpand(module.id)}
                      >
                        <span className="expand-icon">
                          {expandedModules[module.id] ? '▼' : '▶'}
                        </span>
                        <div className="module-info">
                          <h4>{module.name} {module.quantity > 1 && <span className="quantity-badge">×{module.quantity}</span>}</h4>
                          <p className="module-type">{module.type || 'COMPONENT'}</p>
                        </div>
                      </div>

                      {expandedModules[module.id] && (
                        <div className="module-content">
                          <p className="module-description">{module.description}</p>
                          
                          <div className="parts-sublist">
                            <h5>Parts Required:</h5>
                            {!module.parts || module.parts.length === 0 ? (
                              <p className="empty-message">No parts specified</p>
                            ) : (
                              <ul>
                                {module.parts.map(part => (
                                  <li key={part.id} className="part-item">
                                    <span className="part-name">{part.name} {part.quantity > 1 && `(×${part.quantity})`}</span>
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
            )}
          </div>
        </div>
      ) : (
        <div className="products-grid">
          {products.length === 0 ? (
            <p className="empty-message">No products available</p>
          ) : (
            products.map(product => (
              <div 
                key={product.id} 
                className="product-card"
                onClick={() => handleProductSelect(product)}
              >
                <div className="product-card-content">
                  <h3>{product.name}</h3>
                  <p className="card-description">{product.description}</p>
                  <div className="card-specs">
                    <span className="price-badge">${product.price.toFixed(2)}</span>
                    <span className="time-badge">{product.estimatedTimeMinutes}m</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
