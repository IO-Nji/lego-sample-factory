import { useEffect, useState } from 'react';
import api from '../api/api';
import ErrorNotification from '../components/ErrorNotification';
import PageHeader from '../components/PageHeader';
import '../styles/StandardPage.css';

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
    <div className="standard-page-container">
      <PageHeader
        title="Masterdata Administration"
        subtitle={`Manage modules (${modules.length}), parts (${parts.length}), and workstations (${workstations.length})`}
        icon="🛠️"
      />
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
      <section className="form-section" style={{ marginTop: '1.5rem' }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: '60% 40%', 
          gap: '1rem', 
          alignItems: 'start',
          maxWidth: '100%'
        }}>
          
          {/* Left Column: Modules Table */}
          <div style={{ minWidth: 0 }}>
            <h2>Modules</h2>
            <div className="users-table-container">
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
          </div>

          {/* Right Column: Create/Edit Form */}
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column',
            margin: '1.5rem',
            padding: '1.5rem',
            background: 'var(--card-bg)',
            borderRadius: 'var(--card-border-radius)',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <h2 style={{ marginTop: 0, marginBottom: '0.75rem', fontSize: '1rem' }}>
              {selectedModuleId ? 'Edit Module' : 'Create Module'}
            </h2>
            <div className="form-card" style={{ padding: '0.5rem' }}>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="module-name">Name</label>
                <input
                  id="module-name"
                  placeholder="e.g., Engine Module"
                  value={moduleForm.name}
                  onChange={(e) => setModuleForm({ ...moduleForm, name: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="module-type">Type</label>
                <input
                  id="module-type"
                  placeholder="e.g., MODULE"
                  value={moduleForm.type}
                  onChange={(e) => setModuleForm({ ...moduleForm, type: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="module-description">Description</label>
                <input
                  id="module-description"
                  placeholder="Short description"
                  value={moduleForm.description}
                  onChange={(e) => setModuleForm({ ...moduleForm, description: e.target.value })}
                />
              </div>
              <div className="button-group" style={{ marginTop: '1rem' }}>
                <button className="primary-link" onClick={createModule}>Create</button>
                <button className="secondary-link" disabled={!selectedModuleId} onClick={updateModule}>Update</button>
                {selectedModuleId && (
                  <button className="secondary-link" onClick={resetForms}>Clear</button>
                )}
              </div>
            </div>
          </div>

        </div>
      </section>
      )}

      {/* Parts Tab */}
      {activeTab === 'parts' && (
      <section className="form-section" style={{ marginTop: '1.5rem' }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: '60% 40%', 
          gap: '1rem', 
          alignItems: 'start',
          maxWidth: '100%'
        }}>
          
          {/* Left Column: Parts Table */}
          <div style={{ minWidth: 0 }}>
            <h2>Parts</h2>
            <div className="users-table-container">
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
          </div>

          {/* Right Column: Create/Edit Form */}
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column',
            margin: '1.5rem',
            padding: '1.5rem',
            background: 'var(--card-bg)',
            borderRadius: 'var(--card-border-radius)',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <h2 style={{ marginTop: 0, marginBottom: '0.75rem', fontSize: '1rem' }}>
              {selectedPartId ? 'Edit Part' : 'Create Part'}
            </h2>
            <div className="form-card" style={{ padding: '0.5rem' }}>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="part-name">Name</label>
                <input
                  id="part-name"
                  placeholder="e.g., 2x4 Brick"
                  value={partForm.name}
                  onChange={(e) => setPartForm({ ...partForm, name: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="part-category">Category</label>
                <input
                  id="part-category"
                  placeholder="e.g., BRICK"
                  value={partForm.category}
                  onChange={(e) => setPartForm({ ...partForm, category: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
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
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="part-description">Description</label>
                <input
                  id="part-description"
                  placeholder="Short description"
                  value={partForm.description}
                  onChange={(e) => setPartForm({ ...partForm, description: e.target.value })}
                />
              </div>
              <div className="button-group" style={{ marginTop: '1rem' }}>
                <button className="primary-link" onClick={createPart}>Create</button>
                <button className="secondary-link" disabled={!selectedPartId} onClick={updatePart}>Update</button>
                {selectedPartId && (
                  <button className="secondary-link" onClick={resetForms}>Clear</button>
                )}
              </div>
            </div>
          </div>

        </div>
      </section>
      )}

      {/* Workstations Tab */}
      {activeTab === 'workstations' && (
      <section className="form-section" style={{ marginTop: '1.5rem' }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: '60% 40%', 
          gap: '1rem', 
          alignItems: 'start',
          maxWidth: '100%'
        }}>
          
          {/* Left Column: Workstations Table */}
          <div style={{ minWidth: 0 }}>
            <h2>Workstations</h2>
            <div className="users-table-container">
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
          </div>

          {/* Right Column: Create/Edit Form */}
          <div style={{ 
            display: 'flex', 
            flexDirection: 'column',
            margin: '1.5rem',
            padding: '1.5rem',
            background: 'var(--card-bg)',
            borderRadius: 'var(--card-border-radius)',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <h2 style={{ marginTop: 0, marginBottom: '0.75rem', fontSize: '1rem' }}>
              {selectedWorkstationId ? 'Edit Workstation' : 'Create Workstation'}
            </h2>
            <div className="form-card" style={{ padding: '0.5rem' }}>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="workstation-name">Name</label>
                <input
                  id="workstation-name"
                  placeholder="e.g., Modules Supermarket"
                  value={workstationForm.name}
                  onChange={(e) => setWorkstationForm({ ...workstationForm, name: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="workstation-type">Type</label>
                <input
                  id="workstation-type"
                  placeholder="e.g., SUPERMARKET"
                  value={workstationForm.workstationType}
                  onChange={(e) => setWorkstationForm({ ...workstationForm, workstationType: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
                <label htmlFor="workstation-description">Description</label>
                <input
                  id="workstation-description"
                  placeholder="Short description"
                  value={workstationForm.description}
                  onChange={(e) => setWorkstationForm({ ...workstationForm, description: e.target.value })}
                />
              </div>
              <div className="form-group" style={{ marginBottom: '0.75rem' }}>
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
              <div className="button-group" style={{ marginTop: '1rem' }}>
                <button className="primary-link" onClick={createWorkstation}>Create</button>
                <button className="secondary-link" disabled={!selectedWorkstationId} onClick={updateWorkstation}>Update</button>
                {selectedWorkstationId && (
                  <button className="secondary-link" onClick={resetForms}>Clear</button>
                )}
              </div>
            </div>
          </div>

        </div>
      </section>
      )}
    </div>
  );
}
