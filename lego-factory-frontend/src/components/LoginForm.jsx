import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PropTypes from 'prop-types';
import { useAuth } from '../context/AuthContext';
import Button from './Button';
import '../styles/LoginForm.css';

/**
 * Standardized Compact Login Form Component
 * Minimal and consistent design for use across the application
 */
function LoginForm({ embedded = false, onSuccess, showHeader = true, showHelpText = false }) {
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
      {showHeader && (
        <div className="login-header">
          <h2>🔐 Sign In</h2>
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
            placeholder="Username"
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
            placeholder="Password"
            required
            autoComplete="current-password"
            disabled={loading}
          />
        </div>

        <Button
          type="submit"
          variant="primary"
          size="medium"
          fullWidth
          loading={loading}
          disabled={loading}
        >
          {loading ? 'Signing in...' : 'Sign In'}
        </Button>
      </form>

      {showHelpText && (
        <div className="login-footer">
          <p className="login-help-text">
            <strong>Quick Login Guide:</strong><br/>
            Generic password for all stations: <code>password</code><br/>
            <em>Hover over station cards in the Order Fulfillment Flow to see usernames</em>
          </p>
        </div>
      )}
    </div>
  );
}

LoginForm.propTypes = {
  embedded: PropTypes.bool,
  onSuccess: PropTypes.func,
  showHeader: PropTypes.bool,
  showHelpText: PropTypes.bool
};

export default LoginForm;
