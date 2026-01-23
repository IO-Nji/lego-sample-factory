import { useState, useEffect } from "react";
import api from "../api/api";
import { USERS_ENDPOINT, WORKSTATIONS_ENDPOINT } from "../api/apiConfig";
import { useAuth } from "../context/AuthContext.jsx";
import PageHeader from "../components/PageHeader";
import StatCard from "../components/StatCard";
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

  // Load users and workstations on mount
  useEffect(() => {
    if (isAdmin && authToken) {
      loadUsersAndWorkstations();
    } else {
      setLoading(false);
    }
  }, [isAdmin, authToken]);

  const loadUsersAndWorkstations = async () => {
    console.log('Loading users and workstations...', { isAdmin, authToken: authToken?.substring(0, 20) + '...' });
    try {
      const [usersRes, workstationsRes] = await Promise.all([
        api.get(USERS_ENDPOINT),
        api.get(WORKSTATIONS_ENDPOINT),
      ]);
      console.log('Users loaded:', usersRes.data?.length, 'Workstations loaded:', workstationsRes.data?.length);
      setUsers(usersRes.data || []);
      setWorkstations(workstationsRes.data || []);
    } catch (error) {
      console.error("Failed to load users/workstations:", error.response?.status, error.response?.data, error.message);
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
      
      console.log('Creating user with payload:', { ...payload, password: '***' });
      const response = await api.post(USERS_ENDPOINT, payload);

      const userData = response.data;
      setCreatedUser(userData);
      setUsers([...users, userData]);
      setFeedback({ 
        type: "success", 
        message: `User "${userData.username}" created successfully (ID: ${userData.id}, Role: ${userData.role})` 
      });
      setForm({ username: "", password: "", role: form.role, workstationId: "" });
    } catch (error) {
      console.error('User creation error:', error.response?.status, error.response?.data, error.message);
      if (error.response?.status === 401) {
        logout();
      }

      const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        (error.response?.status === 403
          ? "You do not have permission to manage users."
          : "Unable to create user. Confirm your admin session is valid.");
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

      const response = await axios.put(`${USERS_ENDPOINT}/${userId}`, payload);

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
        {feedback.message && (
          <p className={feedback.type === "error" ? "form-error" : "form-success"}>{feedback.message}</p>
        )}
      </section>
    );
  }

  if (!isAdmin) {
    return (
      <section className="form-section">
        <h2>Insufficient permissions</h2>
        <p className="form-helper">
          You are signed in as <strong>{session.user?.username}</strong>, but only administrators can create
          new users.
        </p>
      </section>
    );
  }

  return (
    <div className="standard-page-container">
      <PageHeader
        title="User Management"
        subtitle={`Manage system users and assign workstation roles. Total users: ${users.length}`}
        icon="👥"
      />
      
      {/* Row 1: Users' Status - 4 cards in horizontal row */}
      <section className="form-section">
        <h2>Users' Status</h2>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(4, 1fr)', 
          gap: '1rem',
          justifyItems: 'center',
          marginBottom: '1rem'
        }}>
          <StatCard 
            title="Total"
            value={users.length}
            icon="👥"
            color="primary"
          />
          <StatCard 
            title="Active"
            value={users.filter(u => u.role !== "VIEWER").length}
            icon="✓"
            color="success"
          />
          <StatCard 
            title="Admin"
            value={users.filter(u => u.role === "ADMIN").length}
            icon="⚙️"
            color="info"
          />
          <StatCard 
            title="Operators"
            value={users.filter(u => u.role !== "ADMIN" && u.role !== "VIEWER").length}
            icon="🔧"
            color="warning"
          />
        </div>
      </section>

      {/* Row 2: Manage Existing Users Table */}
      <section className="form-section">
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
      </section>

      {/* Row 3: Create New User Form */}
      <section className="form-section">
        <h2>Create New User</h2>
        <p className="form-helper">
          Use your administrator token to add operators for other factory roles.
        </p>
        <form className="form-card form-card-grid" onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                name="username"
                type="text"
                value={form.username}
                onChange={handleChange}
                disabled={submitting}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                value={form.password}
                onChange={handleChange}
                disabled={submitting}
                required
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="role">Role</label>
              <select
                id="role"
                name="role"
                value={form.role}
                onChange={handleChange}
                disabled={submitting}
              >
                {ROLE_OPTIONS.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="workstationId">Workstation</label>
              <select
                id="workstationId"
                name="workstationId"
                value={form.workstationId}
                onChange={handleChange}
                disabled={submitting}
              >
                <option value="">-- None --</option>
                {workstations.map((ws) => (
                  <option key={ws.id} value={ws.id}>
                    {ws.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <button type="submit" className="primary-link" disabled={submitting}>
            {submitting ? "Creating..." : "Create"}
          </button>
        </form>
        {feedback.message && (
          feedback.type === "error" ? (
            <p
              className="form-error"
              role="alert"
            >
              {feedback.message}
            </p>
          ) : (
            <output
              className="form-success"
              htmlFor="username password role workstationId"
            >
              {feedback.message}
            </output>
          )
        )}
        {createdUser && (
          <div className="form-success-details">
            <h3>Created User Details:</h3>
            <ul>
              <li><strong>ID:</strong> {createdUser.id}</li>
              <li><strong>Username:</strong> {createdUser.username}</li>
              <li><strong>Role:</strong> {createdUser.role}</li>
              {createdUser.workstationId && (
                <li><strong>Workstation ID:</strong> {createdUser.workstationId}</li>
              )}
            </ul>
          </div>
        )}
      </section>
    </div>
  );
}

export default UserManagementPage;
