import { Navigate, useLocation, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import LoginForm from "../components/LoginForm";
import { useEffect, useState } from "react";

function LoginPage() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { isAuthenticated } = useAuth();
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const reason = searchParams.get('reason');
    const expired = location.state?.expired;

    if (reason === 'session_expired' || expired) {
      setErrorMessage("Your session has expired. Please sign in again.");
    } else if (reason === 'backend_unavailable') {
      setErrorMessage("Backend service is unavailable. Please try again.");
    } else if (reason === 'unauthenticated') {
      setErrorMessage("Please sign in to access this page.");
    } else if (location.state?.from) {
      setErrorMessage("Authentication required to access this page.");
    }
  }, [searchParams, location.state]);

  if (isAuthenticated) {
    // Redirect to intended destination or dashboard
    const from = location.state?.from?.pathname || "/dashboard";
    return <Navigate to={from} replace />;
  }

  return (
    <div className="login-page" style={{ 
      display: 'flex', 
      flexDirection: 'column',
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '80vh',
      padding: 'var(--spacing-md)',
      gap: '1rem'
    }}>
      {errorMessage && (
        <div style={{
          maxWidth: '400px',
          width: '100%',
          padding: '1rem',
          backgroundColor: '#fff3cd',
          border: '1px solid #ffeaa7',
          borderLeft: '3px solid #ffc107',
          borderRadius: '4px',
          color: '#856404',
          fontSize: '0.9rem',
          marginBottom: '0.5rem'
        }}>
          <strong>⚠️</strong> {errorMessage}
        </div>
      )}
      
      <LoginForm embedded={false} showHeader={true} showHelpText={false} />
      
      <div style={{ 
        marginTop: '1rem',
        textAlign: 'center'
      }}>
        <Link 
          to="/" 
          style={{
            color: 'var(--color-primary)',
            textDecoration: 'none',
            fontSize: '0.9rem',
            fontWeight: '500',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.25rem'
          }}
        >
          ← Back to Home
        </Link>
      </div>
    </div>
  );
}

export default LoginPage;
