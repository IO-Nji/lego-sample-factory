import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

/**
 * AuthGuard component to protect routes and handle authentication state
 * Redirects to home page if user is not authenticated or token has expired
 */
export function AuthGuard({ children, requiredRole = null }) {
  const { isAuthenticated, session } = useAuth();
  const location = useLocation();

  // Check if token has expired
  if (session?.expiresAt) {
    const expiresAt = new Date(session.expiresAt).getTime();
    const now = Date.now();
    
    if (now >= expiresAt) {
      console.log('Token expired - clearing session and redirecting to home');
      localStorage.removeItem('authToken');
      localStorage.removeItem('authSession');
      return <Navigate to="/" state={{ from: location, reason: 'expired' }} replace />;
    }
  }

  // Check if user is authenticated
  if (!isAuthenticated) {
    console.log('Not authenticated - redirecting to home');
    return <Navigate to="/" state={{ from: location, reason: 'unauthenticated' }} replace />;
  }

  // Check if specific role is required
  if (requiredRole && session?.user?.role !== requiredRole) {
    console.log(`Role check failed. Required: ${requiredRole}, Got: ${session?.user?.role}`);
    return <Navigate to="/dashboard" state={{ from: location, reason: 'unauthorized' }} replace />;
  }

  return children;
}

/**
 * AdminGuard - shorthand for admin-only routes
 */
export function AdminGuard({ children }) {
  return <AuthGuard requiredRole="ADMIN">{children}</AuthGuard>;
}
