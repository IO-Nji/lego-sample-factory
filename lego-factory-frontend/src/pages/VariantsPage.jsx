import { useState, useEffect } from "react";
import PageHeader from "../components/PageHeader.jsx";
import Button from "../components/Button.jsx";
import "../styles/AdminPage.css";

function VariantsPage() {
  const [variants, setVariants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    code: "",
    description: "",
    baseProductId: ""
  });

  useEffect(() => {
    fetchVariants();
  }, []);

  const fetchVariants = async () => {
    setLoading(true);
    // TODO: Implement API call to fetch variants
    setTimeout(() => {
      setVariants([
        { id: 1, name: "Color Variant", code: "CV001", description: "Different color options", baseProductId: "P001" },
        { id: 2, name: "Size Variant", code: "SV001", description: "Different size options", baseProductId: "P002" }
      ]);
      setLoading(false);
    }, 500);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    // TODO: Implement API call to create variant
    console.log("Creating variant:", formData);
    setShowAddForm(false);
    setFormData({ name: "", code: "", description: "", baseProductId: "" });
    fetchVariants();
  };

  const handleDelete = async (id) => {
    if (window.confirm("Are you sure you want to delete this variant?")) {
      // TODO: Implement API call to delete variant
      console.log("Deleting variant:", id);
      fetchVariants();
    }
  };

  return (
    <div className="admin-page">
      <PageHeader 
        title="Variants Management" 
        subtitle="Manage product variants and configurations"
      />

      <div className="page-actions">
        <Button 
          variant="primary" 
          onClick={() => setShowAddForm(!showAddForm)}
        >
          {showAddForm ? "Cancel" : "+ Add Variant"}
        </Button>
      </div>

      {showAddForm && (
        <div className="form-card">
          <h3>Add New Variant</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="name">Variant Name</label>
                <input
                  id="name"
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  required
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label htmlFor="code">Variant Code</label>
                <input
                  id="code"
                  type="text"
                  value={formData.code}
                  onChange={(e) => setFormData({...formData, code: e.target.value})}
                  required
                  className="form-input"
                />
              </div>

              <div className="form-group">
                <label htmlFor="baseProductId">Base Product ID</label>
                <input
                  id="baseProductId"
                  type="text"
                  value={formData.baseProductId}
                  onChange={(e) => setFormData({...formData, baseProductId: e.target.value})}
                  required
                  className="form-input"
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="description">Description</label>
                <textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="form-input"
                  rows="3"
                />
              </div>
            </div>

            <div className="form-actions">
              <Button type="submit" variant="success">Create Variant</Button>
              <Button type="button" variant="secondary" onClick={() => setShowAddForm(false)}>
                Cancel
              </Button>
            </div>
          </form>
        </div>
      )}

      {loading ? (
        <div className="loading">Loading variants...</div>
      ) : (
        <div className="data-table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Variant Name</th>
                <th>Code</th>
                <th>Description</th>
                <th>Base Product</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {variants.length === 0 ? (
                <tr>
                  <td colSpan="6" className="no-data">No variants found</td>
                </tr>
              ) : (
                variants.map((variant) => (
                  <tr key={variant.id}>
                    <td>{variant.id}</td>
                    <td>{variant.name}</td>
                    <td><code>{variant.code}</code></td>
                    <td>{variant.description}</td>
                    <td>{variant.baseProductId}</td>
                    <td className="actions">
                      <Button variant="primary" size="small">Edit</Button>
                      <Button 
                        variant="danger" 
                        size="small"
                        onClick={() => handleDelete(variant.id)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default VariantsPage;
