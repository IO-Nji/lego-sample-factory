import { useEffect, useState } from 'react';
import api from '../api/api';
import ErrorNotification from '../components/ErrorNotification';

export default function MasterdataAdminPage() {
  const [modules, setModules] = useState([]);
  const [parts, setParts] = useState([]);
  const [workstations, setWorkstations] = useState([]);

  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('modules'); // modules, parts, workstations

  // Align forms with backend DTOs
  const [moduleForm, setModuleForm] = useState({ name: '', description: '', type: '' });
  const [partForm, setPartForm] = useState({ name: '', description: '', category: '', unitCost: '' });
  const [workstationForm, setWorkstationForm] = useState({ name: '', workstationType: '', description: '', active: true });

  const [selectedModuleId, setSelectedModuleId] = useState(null);
  const [selectedPartId, setSelectedPartId] = useState(null);
  const [selectedWorkstationId, setSelectedWorkstationId] = useState(null);

  const loadAll = async () => {
    try {
      setError(null);
      const [mRes, pRes, wRes] = await Promise.all([
        api.get('/masterdata/modules'),
        api.get('/masterdata/parts'),
        api.get('/masterdata/workstations'),
      ]);
      setModules(mRes.data || []);
      setParts(pRes.data || []);
      setWorkstations(wRes.data || []);
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to load masterdata');
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const resetForms = () => {
    setModuleForm({ name: '', description: '', type: '' });
    setPartForm({ name: '', description: '', category: '', unitCost: '' });
    setWorkstationForm({ name: '', workstationType: '', description: '', active: true });
    setSelectedModuleId(null);
    setSelectedPartId(null);
    setSelectedWorkstationId(null);
  };

  // Modules CRUD
  const createModule = async () => {
    try {
      setError(null);
      await api.post('/masterdata/modules', moduleForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to create module');
    }
  };

  const updateModule = async () => {
    if (!selectedModuleId) return;
    try {
      setError(null);
      await api.put(`/masterdata/modules/${selectedModuleId}`, moduleForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to update module');
    }
  };

  const deleteModule = async (id) => {
    try {
      setError(null);
      await api.delete(`/masterdata/modules/${id}`);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to delete module');
    }
  };

  // Parts CRUD
  const createPart = async () => {
    try {
      setError(null);
      await api.post('/masterdata/parts', partForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to create part');
    }
  };

  const updatePart = async () => {
    if (!selectedPartId) return;
    try {
      setError(null);
      await api.put(`/masterdata/parts/${selectedPartId}`, partForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to update part');
    }
  };

  const deletePart = async (id) => {
    try {
      setError(null);
      await api.delete(`/masterdata/parts/${id}`);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to delete part');
    }
  };

  // Workstations CRUD
  const createWorkstation = async () => {
    try {
      setError(null);
      await api.post('/masterdata/workstations', workstationForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to create workstation');
    }
  };

  const updateWorkstation = async () => {
    if (!selectedWorkstationId) return;
    try {
      setError(null);
      await api.put(`/masterdata/workstations/${selectedWorkstationId}`, workstationForm);
      resetForms();
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to update workstation');
    }
  };

  const deleteWorkstation = async (id) => {
    try {
      setError(null);
      await api.delete(`/masterdata/workstations/${id}`);
      await loadAll();
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Failed to delete workstation');
    }
  };

  return (
    <div className="app-content">
      <h1 style={{ marginTop: 0 }}>Masterdata</h1>
      {error && <ErrorNotification message={error} onClose={() => setError(null)} />}

      {/* Tab Navigation */}
      <div className="dashboard-tabs">
        <button
          className={`tab-button ${activeTab === 'modules' ? 'active' : ''}`}
          onClick={() => setActiveTab('modules')}
        >
          🧩 Modules
        </button>
        <button
          className={`tab-button ${activeTab === 'parts' ? 'active' : ''}`}
          onClick={() => setActiveTab('parts')}
        >
          🔩 Parts
        </button>
        <button
          className={`tab-button ${activeTab === 'workstations' ? 'active' : ''}`}
          onClick={() => setActiveTab('workstations')}
        >
          🔧 Workstations
        </button>
      </div>

      {/* Modules Tab */}
      {activeTab === 'modules' && (
      <section className="form-section" style={{ overflow: 'hidden' }}>
        <h2>Modules</h2>
        <div className="form-card">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="module-name">Name</label>
              <input
                id="module-name"
                placeholder="e.g., Engine Module"
                value={moduleForm.name}
                onChange={(e) => setModuleForm({ ...moduleForm, name: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="module-type">Type</label>
              <input
                id="module-type"
                placeholder="e.g., MODULE"
                value={moduleForm.type}
                onChange={(e) => setModuleForm({ ...moduleForm, type: e.target.value })}
              />
            </div>
          </div>
          <div className="form-group">
            <label htmlFor="module-description">Description</label>
            <input
              id="module-description"
              placeholder="Short description"
              value={moduleForm.description}
              onChange={(e) => setModuleForm({ ...moduleForm, description: e.target.value })}
            />
          </div>
          <div className="button-group">
            <button className="primary-link" onClick={createModule}>Create</button>
            <button className="secondary-link" disabled={!selectedModuleId} onClick={updateModule}>Update</button>
          </div>
        </div>

        <div className="users-table-container" style={{ maxHeight: '320px' }}>
          <table className="products-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Description</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {modules.map((m) => (
                <tr key={m.id}>
                  <td>{m.name}</td>
                  <td>{m.type}</td>
                  <td>{m.description}</td>
                  <td>
                    <div className="actions">
                      <button
                        className="edit-btn"
                        onClick={() => { setSelectedModuleId(m.id); setModuleForm({ name: m.name || '', description: m.description || '', type: m.type || '' }); }}
                      >✎ Edit</button>
                      <button className="delete-btn" onClick={() => deleteModule(m.id)}>🗑 Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      )}

      {/* Parts Tab */}
      {activeTab === 'parts' && (
      <section className="form-section" style={{ overflow: 'hidden' }}>
        <h2>Parts</h2>
        <div className="form-card">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="part-name">Name</label>
              <input
                id="part-name"
                placeholder="e.g., 2x4 Brick"
                value={partForm.name}
                onChange={(e) => setPartForm({ ...partForm, name: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="part-category">Category</label>
              <input
                id="part-category"
                placeholder="e.g., BRICK"
                value={partForm.category}
                onChange={(e) => setPartForm({ ...partForm, category: e.target.value })}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="part-unit-cost">Unit Cost</label>
              <input
                id="part-unit-cost"
                type="number"
                step="0.01"
                placeholder="e.g., 0.25"
                value={partForm.unitCost}
                onChange={(e) => setPartForm({ ...partForm, unitCost: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="part-description">Description</label>
              <input
                id="part-description"
                placeholder="Short description"
                value={partForm.description}
                onChange={(e) => setPartForm({ ...partForm, description: e.target.value })}
              />
            </div>
          </div>
          <div className="button-group">
            <button className="primary-link" onClick={createPart}>Create</button>
            <button className="secondary-link" disabled={!selectedPartId} onClick={updatePart}>Update</button>
          </div>
        </div>

        <div className="users-table-container" style={{ maxHeight: '320px' }}>
          <table className="products-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Category</th>
                <th>Unit Cost</th>
                <th>Description</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {parts.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.category}</td>
                  <td>{p.unitCost}</td>
                  <td>{p.description}</td>
                  <td>
                    <div className="actions">
                      <button
                        className="edit-btn"
                        onClick={() => { setSelectedPartId(p.id); setPartForm({ name: p.name || '', description: p.description || '', category: p.category || '', unitCost: p.unitCost ?? '' }); }}
                      >✎ Edit</button>
                      <button className="delete-btn" onClick={() => deletePart(p.id)}>🗑 Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      )}

      {/* Workstations Tab */}
      {activeTab === 'workstations' && (
      <section className="form-section" style={{ overflow: 'hidden' }}>
        <h2>Workstations</h2>
        <div className="form-card">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="workstation-name">Name</label>
              <input
                id="workstation-name"
                placeholder="e.g., Modules Supermarket"
                value={workstationForm.name}
                onChange={(e) => setWorkstationForm({ ...workstationForm, name: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="workstation-type">Type</label>
              <input
                id="workstation-type"
                placeholder="e.g., SUPERMARKET"
                value={workstationForm.workstationType}
                onChange={(e) => setWorkstationForm({ ...workstationForm, workstationType: e.target.value })}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="workstation-description">Description</label>
              <input
                id="workstation-description"
                placeholder="Short description"
                value={workstationForm.description}
                onChange={(e) => setWorkstationForm({ ...workstationForm, description: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label htmlFor="workstation-active">Active</label>
              <select
                id="workstation-active"
                value={workstationForm.active ? 'true' : 'false'}
                onChange={(e) => setWorkstationForm({ ...workstationForm, active: e.target.value === 'true' })}
              >
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            </div>
          </div>
          <div className="button-group">
            <button className="primary-link" onClick={createWorkstation}>Create</button>
            <button className="secondary-link" disabled={!selectedWorkstationId} onClick={updateWorkstation}>Update</button>
          </div>
        </div>

        <div className="users-table-container" style={{ maxHeight: '320px' }}>
          <table className="products-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Description</th>
                <th>Active</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {workstations.map((w) => (
                <tr key={w.id}>
                  <td>{w.name}</td>
                  <td>{w.workstationType}</td>
                  <td>{w.description}</td>
                  <td>{w.active ? 'Yes' : 'No'}</td>
                  <td>
                    <div className="actions">
                      <button
                        className="edit-btn"
                        onClick={() => { setSelectedWorkstationId(w.id); setWorkstationForm({ name: w.name || '', workstationType: w.workstationType || '', description: w.description || '', active: !!w.active }); }}
                      >✎ Edit</button>
                      <button className="delete-btn" onClick={() => deleteWorkstation(w.id)}>🗑 Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      )}
    </div>
  );
}
