import { useState, useEffect } from "react";
import { Navigate, useNavigate, useLocation } from "react-router-dom";

import { useAuth } from "../context/AuthContext.jsx";

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, login, loading } = useAuth();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  // Check if user was redirected here due to session expiry or unauthorized access
  useEffect(() => {
    const state = location.state;
    if (state?.reason === 'expired') {
      setMessage("Your session has expired. Please sign in again.");
    } else if (state?.reason === 'unauthenticated') {
      setMessage("Please sign in to access this page.");
    }
  }, [location.state]);

  if (isAuthenticated) {
    // Redirect to intended destination or dashboard
    const from = location.state?.from?.pathname || "/dashboard";
    return <Navigate to={from} replace />;
  }

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    console.log('LoginPage handleSubmit called');
    event.preventDefault();
    event.stopPropagation();
    setError("");
    setMessage("");

    if (!form.username.trim() || !form.password) {
      setError("Username and password are required.");
      return;
    }

    try {
      console.log('Calling login function...');
      await login(form.username, form.password);
      // Redirect to intended destination or dashboard
      const from = location.state?.from?.pathname || "/dashboard";
      navigate(from, { replace: true });
    } catch (requestError) {
      console.error('Login request error:', requestError);
      setError(requestError.message);
    }
  };

  return (
    <div className="login-page">
      <section className="form-section">
        <h2>Sign In</h2>
        <p className="form-helper">Enter your LEGO factory credentials.</p>
        {message && (
          <p className="form-info" role="status" style={{ 
            backgroundColor: "#fff3cd", 
            color: "#856404", 
            padding: "0.75rem", 
            borderRadius: "4px", 
            marginBottom: "1rem",
            border: "1px solid #ffeaa7"
          }}>
            {message}
          </p>
        )}
        <form 
          className="form-card" 
          onSubmit={handleSubmit} 
          action="#"
          method="post"
          noValidate
        >
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            type="text"
            autoComplete="username"
            value={form.username}
            onChange={handleChange}
            disabled={loading}
            required
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={handleChange}
            disabled={loading}
            required
          />

          <button type="submit" className="primary-link" disabled={loading}>
            {loading ? "Signing in..." : "Sign in"}
          </button>
        </form>
        <button
          type="button"
          className="secondary-link"
          style={{ marginTop: 16 }}
          onClick={() => navigate("/")}
        >
          Back to Home
        </button>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
      </section>
    </div>
  );
}

export default LoginPage;
