import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import api from "../api/api";
import { clearStoredSession, readStoredSession, storeSession } from "../api/apiConfig";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => readStoredSession());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const handler = (event) => {
      if (event.key === "authSession" || event.key === "authToken") {
        setSession(readStoredSession());
      }
    };

    window.addEventListener("storage", handler);
    return () => window.removeEventListener("storage", handler);
  }, []);

  const login = useCallback(async (username, password) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/login', {
        username: username.trim(),
        password,
      });

      const { token, tokenType, expiresAt, user } = response.data;
      const payload = { token, tokenType, expiresAt, user };
      storeSession(payload);
      setSession(payload);
      
      // Log the login event for audit trail
      try {
        await api.post('/orders/audit/login');
      } catch (auditError) {
        console.warn('Failed to log login event:', auditError);
        // Don't fail login if audit logging fails
      }
      
      return payload;
    } catch (error) {
      console.error('Login error:', error.response?.status, error.response?.data);
      const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        "Login failed. Check your credentials and try again.";
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearStoredSession();
    setSession(null);
    
    // Redirect to home page after logout
    // Use location.href for clean redirect without preserved state
    if (window.location.pathname !== '/') {
      window.location.href = '/';
    }
  }, []);

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      isAdmin: session?.user?.role === "ADMIN",
      isPlantWarehouse: session?.user?.role === "PLANT_WAREHOUSE",
      isModulesSupermarket: session?.user?.role === "MODULES_SUPERMARKET",
      isProductionPlanning: session?.user?.role === "PRODUCTION_PLANNING",
      isProductionControl: session?.user?.role === "PRODUCTION_CONTROL",
      isAssemblyControl: session?.user?.role === "ASSEMBLY_CONTROL",
      isManufacturingWorkstation: session?.user?.role === "MANUFACTURING_WORKSTATION",
      isAssemblyWorkstation: session?.user?.role === "ASSEMBLY_WORKSTATION",
      isPartsSupplyWarehouse: session?.user?.role === "PARTS_SUPPLY",
      loading,
      login,
      logout,
    }),
    [session, loading, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
