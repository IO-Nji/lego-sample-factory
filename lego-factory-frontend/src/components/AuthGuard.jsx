import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { useEffect } from "react";

/**
 * AuthGuard component to protect routes and handle authentication state
 * Redirects to login page if user is not authenticated or token has expired
 */
export function AuthGuard({ children, requiredRole = null }) {
  const { isAuthenticated, session, logout } = useAuth();
  const location = useLocation();

  // Check if token has expired
  useEffect(() => {
    if (session?.expiresAt) {
      const expiresAt = new Date(session.expiresAt).getTime();
      const now = Date.now();
      
      if (now >= expiresAt) {
        logout();
      }
    }
  }, [session, logout]);

  // Check if user is authenticated
  if (!isAuthenticated || !session) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Additional token expiration check
  if (session?.expiresAt) {
    const expiresAt = new Date(session.expiresAt).getTime();
    const now = Date.now();
    
    if (now >= expiresAt) {
      return <Navigate to="/login" state={{ from: location, expired: true }} replace />;
    }
  }

  // Check if specific role is required
  if (requiredRole && session?.user?.role !== requiredRole) {
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
