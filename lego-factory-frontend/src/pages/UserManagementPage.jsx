import { useState, useEffect } from "react";
import api from "../api/api";
import { USERS_ENDPOINT, WORKSTATIONS_ENDPOINT } from "../api/apiConfig";
import { useAuth } from "../context/AuthContext.jsx";
import { Button, Input, Alert, ControlPage, StatCard } from "../components";
import "../styles/StandardPage.css";

const ROLE_OPTIONS = [
  "ADMIN",
  "PLANT_WAREHOUSE",
  "MODULES_SUPERMARKET",
  "PRODUCTION_PLANNING",
  "PRODUCTION_CONTROL",
  "ASSEMBLY_CONTROL",
  "PARTS_SUPPLY",
  "MANUFACTURING",
  "VIEWER",
];

function UserManagementPage() {
  const { session, isAdmin, logout } = useAuth();
  const [form, setForm] = useState({
    username: "",
    password: "",
    role: "VIEWER",
    workstationId: "",
  });
  const [users, setUsers] = useState([]);
  const [workstations, setWorkstations] = useState([]);
  const [editingUserId, setEditingUserId] = useState(null);
  const [editForm, setEditForm] = useState({
    role: "",
    workstationId: "",
  });
  const [feedback, setFeedback] = useState({ type: "", message: "" });
  const [submitting, setSubmitting] = useState(false);
  const [createdUser, setCreatedUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const authToken = session?.token ?? null;

  useEffect(() => {
    if (isAdmin && authToken) {
      loadUsersAndWorkstations();
    } else {
      setLoading(false);
    }
  }, [isAdmin, authToken]);

  const loadUsersAndWorkstations = async () => {
    try {
      const [usersRes, workstationsRes] = await Promise.all([
        api.get(USERS_ENDPOINT),
        api.get(WORKSTATIONS_ENDPOINT),
      ]);
      setUsers(usersRes.data || []);
      setWorkstations(workstationsRes.data || []);
    } catch (error) {
      if (error.response?.status === 401) {
        logout();
      }
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleEditChange = (event) => {
    const { name, value } = event.target;
    setEditForm((current) => ({ ...current, [name]: value }));
  };

  const resetFeedback = () => setFeedback({ type: "", message: "" });

  const handleSubmit = async (event) => {
    event.preventDefault();
    resetFeedback();

    if (!authToken || !isAdmin) {
      setFeedback({ type: "error", message: "Please sign in as an administrator first." });
      return;
    }

    if (!form.username.trim() || !form.password.trim()) {
      setFeedback({ type: "error", message: "Username and password are required." });
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        username: form.username.trim(),
        password: form.password,
        role: form.role,
        workstationId: form.workstationId ? Number(form.workstationId) : null,
      };
      
      const response = await api.post(USERS_ENDPOINT, payload);
      const userData = response.data;
      setCreatedUser(userData);
      setUsers([...users, userData]);
      setFeedback({ 
        type: "success", 
        message: `User "${userData.username}" created successfully` 
      });
      setForm({ username: "", password: "", role: form.role, workstationId: "" });
    } catch (error) {
      if (error.response?.status === 401) {
        logout();
      }
      const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        "Unable to create user.";
      setFeedback({ type: "error", message });
    } finally {
      setSubmitting(false);
    }
  };

  const startEditUser = (user) => {
    setEditingUserId(user.id);
    setEditForm({
      role: user.role,
      workstationId: user.workstationId || "",
    });
  };

  const cancelEdit = () => {
    setEditingUserId(null);
    setEditForm({ role: "", workstationId: "" });
  };

  const saveUserUpdate = async (userId) => {
    resetFeedback();
    setSubmitting(true);
    try {
      const user = users.find((u) => u.id === userId);
      const payload = {
        username: user.username,
        role: editForm.role,
        workstationId: editForm.workstationId ? Number(editForm.workstationId) : null,
      };

      const response = await api.put(`${USERS_ENDPOINT}/${userId}`, payload);
      setUsers(users.map((u) => (u.id === userId ? response.data : u)));
      setEditingUserId(null);
      setFeedback({
        type: "success",
        message: `User "${response.data.username}" updated successfully`,
      });
    } catch (error) {
      if (error.response?.status === 401) {
        logout();
      }
      const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        "Unable to update user.";
      setFeedback({ type: "error", message });
    } finally {
      setSubmitting(false);
    }
  };

  if (!session) {
    return (
      <section className="form-section">
        <h2>Administrator sign-in required</h2>
        <p className="form-helper">Log in with an administrator account to manage users.</p>
      </section>
    );
  }

  if (!isAdmin) {
    return (
      <section className="form-section">
        <h2>Insufficient permissions</h2>
        <p className="form-helper">
          You are signed in as <strong>{session.user?.username}</strong>, but only administrators can create new users.
        </p>
      </section>
    );
  }

  // Statistics for the cards
  const totalUsers = users.length;
  const activeUsers = users.filter(u => u.role !== 'VIEWER').length;
  const adminUsers = users.filter(u => u.role === 'ADMIN').length;
  const operatorUsers = users.filter(u => ['PLANT_WAREHOUSE', 'MODULES_SUPERMARKET', 'PRODUCTION_CONTROL', 'ASSEMBLY_CONTROL', 'PARTS_SUPPLY', 'MANUFACTURING'].includes(u.role)).length;

  const statistics = [
    { value: totalUsers, label: 'Total Users', variant: 'primary', icon: '👥' },
    { value: activeUsers, label: 'Active Users', variant: 'success', icon: '✓' },
    { value: adminUsers, label: 'Admins', variant: 'warning', icon: '👑' },
    { value: operatorUsers, label: 'Operators', variant: 'info', icon: '⚙️' }
  ];

  // Table content
  const tableContent = (
    <>
      <h2>Manage Existing Users</h2>
      {loading && <p>Loading users...</p>}
      {!loading && users.length === 0 && <p className="form-helper">No users found.</p>}
      {!loading && users.length > 0 && (
        <div className="users-table-container">
          <table className="products-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Role</th>
                <th>Workstation</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => {
                const isEditing = editingUserId === user.id;
                const workstationName = workstations.find(
                  (ws) => ws.id === user.workstationId
                )?.name || "-- None --";

                return (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.username}</td>
                    <td>
                      {isEditing ? (
                        <select
                          value={editForm.role}
                          onChange={(e) =>
                            setEditForm({ ...editForm, role: e.target.value })
                          }
                        >
                          {ROLE_OPTIONS.map((role) => (
                            <option key={role} value={role}>
                              {role}
                            </option>
                          ))}
                        </select>
                      ) : (
                        user.role
                      )}
                    </td>
                    <td>
                      {isEditing ? (
                        <select
                          value={editForm.workstationId}
                          onChange={(e) =>
                            setEditForm({
                              ...editForm,
                              workstationId: e.target.value,
                            })
                          }
                        >
                          <option value="">-- None --</option>
                          {workstations.map((ws) => (
                            <option key={ws.id} value={ws.id}>
                              {ws.name}
                            </option>
                          ))}
                        </select>
                      ) : (
                        workstationName
                      )}
                    </td>
                    <td>
                      {isEditing ? (
                        <div className="actions">
                          <button
                            className="edit-btn"
                            onClick={() => saveUserUpdate(user.id)}
                            disabled={submitting}
                          >
                            {submitting ? "Saving..." : "✓ Save"}
                          </button>
                          <button
                            className="delete-btn"
                            onClick={cancelEdit}
                            disabled={submitting}
                          >
                            ✕ Cancel
                          </button>
                        </div>
                      ) : (
                        <div className="actions">
                          <button
                            className="edit-btn"
                            onClick={() => startEditUser(user)}
                          >
                            ✎ Edit
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  );

  // Form content
  const formContent = (
    <>
      <h2 style={{ marginTop: 0, marginBottom: '0.75rem', fontSize: '1rem' }}>Create New User</h2>
      <p className="form-helper" style={{ fontSize: '0.8rem', marginBottom: '0.5rem' }}>
        Add new operators for factory roles.
      </p>
      <form 
        className="form-card" 
        onSubmit={handleSubmit} 
        noValidate 
        style={{ 
          padding: '0.5rem', 
          display: 'flex', 
          flexDirection: 'column'
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.4rem', marginBottom: '0.4rem' }}>
          <Input
            id="username"
            name="username"
            label="Username"
            type="text"
            value={form.username}
            onChange={handleChange}
            disabled={submitting}
            required
            fullWidth
            placeholder="Username"
          />
          <Input
            id="password"
            name="password"
            label="Password"
            type="password"
            value={form.password}
            onChange={handleChange}
            disabled={submitting}
            required
            fullWidth
            placeholder="Password"
            autoComplete="new-password"
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.4rem', marginBottom: '0.4rem' }}>
          <div>
            <label htmlFor="role" style={{ display: 'block', marginBottom: '0.25rem', fontWeight: '500', fontSize: '0.8rem' }}>
              Role <span style={{ color: 'var(--color-danger)' }}>*</span>
            </label>
            <select
              id="role"
              name="role"
              value={form.role}
              onChange={handleChange}
              disabled={submitting}
              required
              style={{ 
                width: '100%', 
                padding: '0.4rem', 
                borderRadius: 'var(--border-radius)',
                border: '1px solid var(--color-border)',
                fontSize: '0.8rem'
              }}
            >
              {ROLE_OPTIONS.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="workstationId" style={{ display: 'block', marginBottom: '0.25rem', fontWeight: '500', fontSize: '0.8rem' }}>
              Workstation
            </label>
            <select
              id="workstationId"
              name="workstationId"
              value={form.workstationId}
              onChange={handleChange}
              disabled={submitting}
              style={{ 
                width: '100%', 
                padding: '0.4rem', 
                borderRadius: 'var(--border-radius)',
                border: '1px solid var(--color-border)',
                fontSize: '0.8rem'
              }}
            >
              <option value="">-- None --</option>
              {workstations.map((ws) => (
                <option key={ws.id} value={ws.id}>
                  {ws.name}
                </option>
              ))}
            </select>
            <small style={{ display: 'block', marginTop: '0.2rem', color: 'var(--color-text-secondary)', fontSize: '0.7rem' }}>
              Optional
            </small>
          </div>
        </div>

        <Button 
          type="submit" 
          variant="primary" 
          disabled={submitting}
          fullWidth
          size="small"
          style={{ marginTop: 'auto' }}
        >
          {submitting ? "Creating..." : "Create User"}
        </Button>
      </form>
      
      {feedback.message && (
        <Alert 
          variant={feedback.type === "error" ? "danger" : "success"}
          onClose={resetFeedback}
          style={{ marginTop: '0.5rem', fontSize: '0.8rem', padding: '0.5rem' }}
        >
          {feedback.message}
        </Alert>
      )}
      
      {createdUser && (
        <div className="form-success-details" style={{ marginTop: '0.5rem', fontSize: '0.8rem', padding: '0.5rem' }}>
          <h3 style={{ marginBottom: '0.4rem', fontSize: '0.85rem' }}>Created User Details:</h3>
          <ul style={{ margin: 0, paddingLeft: '1rem', fontSize: '0.75rem' }}>
            <li><strong>ID:</strong> {createdUser.id}</li>
            <li><strong>Username:</strong> {createdUser.username}</li>
            <li><strong>Role:</strong> {createdUser.role}</li>
            {createdUser.workstationId && (
              <li><strong>Workstation ID:</strong> {createdUser.workstationId}</li>
            )}
          </ul>
        </div>
      )}
    </>
  );

  return (
    <ControlPage
      title="User Management"
      subtitle="Manage system users and assign workstation roles"
      icon="👥"
      statistics={statistics}
      tableContent={tableContent}
      formContent={formContent}
      error={null}
      onErrorClose={() => {}}
    />
  );
}

export default UserManagementPage;
