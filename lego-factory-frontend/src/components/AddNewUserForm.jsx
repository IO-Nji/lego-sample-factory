import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import Button from './Button';
import api from '../api/api';
import '../styles/AddNewUserForm.css';

/**
 * Standardized Compact Add New User Form Component
 * Minimal and consistent design matching LoginForm styling
 */
function AddNewUserForm({ workstations = [], onSuccess, onError }) {
  const [form, setForm] = useState({
    username: '',
    password: '',
    role: 'OPERATOR',
    workstationId: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', message: '' });

  const ROLE_OPTIONS = ['ADMIN', 'OPERATOR', 'VIEWER'];

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFeedback({ type: '', message: '' });
    setSubmitting(true);

    try {
      const payload = {
        username: form.username,
        password: form.password,
        role: form.role,
        workstationId: form.workstationId ? Number.parseInt(form.workstationId) : null
      };

      const response = await api.post('/users/create', payload);
      
      setFeedback({
        type: 'success',
        message: `User "${form.username}" created successfully!`
      });

      // Reset form
      setForm({
        username: '',
        password: '',
        role: 'OPERATOR',
        workstationId: ''
      });

      if (onSuccess) {
        onSuccess(response.data);
      }
    } catch (err) {
      const errorMsg = err.response?.data?.message || 'Failed to create user';
      setFeedback({
        type: 'error',
        message: errorMsg
      });

      if (onError) {
        onError(errorMsg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="add-user-form-container">
      <div className="add-user-header">
        <h2>👤 New User</h2>
      </div>

      {feedback.message && (
        <div className={`add-user-feedback ${feedback.type}`}>
          <span className="feedback-icon">
            {feedback.type === 'error' ? '⚠️' : '✓'}
          </span>
          <span>{feedback.message}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="add-user-form">
        <div className="form-group">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            type="text"
            value={form.username}
            onChange={handleChange}
            placeholder="Username"
            required
            disabled={submitting}
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
            placeholder="Password"
            required
            disabled={submitting}
          />
        </div>

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
                {ws.name || `Workstation ${ws.id}`}
              </option>
            ))}
          </select>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="medium"
          fullWidth
          loading={submitting}
          disabled={submitting}
        >
          {submitting ? 'Creating...' : 'Create User'}
        </Button>
      </form>
    </div>
  );
}

AddNewUserForm.propTypes = {
  workstations: PropTypes.array,
  onSuccess: PropTypes.func,
  onError: PropTypes.func
};

export default AddNewUserForm;
