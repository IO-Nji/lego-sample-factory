import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PropTypes from 'prop-types';
import { useAuth } from '../context/AuthContext';
import Button from './Button';
import '../styles/LoginForm.css';

/**
 * Reusable Login Form Component
 * Can be used standalone or embedded in other pages
 */
function LoginForm({ embedded = false, onSuccess }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(username, password);
      
      if (onSuccess) {
        onSuccess();
      } else {
        // Navigate to dashboard on successful login
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const containerClass = embedded ? 'login-form-embedded' : 'login-form-standalone';

  return (
    <div className={`login-form-container ${containerClass}`}>
      {!embedded && (
        <div className="login-header">
          <h2>🔐 Sign In to LEGO Factory</h2>
          <p>Enter your credentials to access the system</p>
        </div>
      )}

      {error && (
        <div className="login-error">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="login-form">
        <div className="form-group">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Enter your username"
            required
            autoComplete="username"
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
            required
            autoComplete="current-password"
            disabled={loading}
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="large"
          fullWidth
          loading={loading}
          disabled={loading}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </Button>
      </form>

      {!embedded && (
        <div className="login-footer">
          <p className="login-help-text">
            Default admin credentials: <code>lego_admin</code> / <code>password</code>
          </p>
        </div>
      )}
    </div>
  );
}

LoginForm.propTypes = {
  embedded: PropTypes.bool,
  onSuccess: PropTypes.func
};

export default LoginForm;
