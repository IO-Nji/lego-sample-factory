import React, { createContext, useContext, useCallback, useRef } from "react";

const DashboardRefreshContext = createContext({
  refresh: () => {},
  subscribe: () => {},
});

export function DashboardRefreshProvider({ children }) {
  const listeners = useRef([]);

  const refresh = useCallback(() => {
    listeners.current.forEach((cb) => cb());
  }, []);

  const subscribe = useCallback((cb) => {
    listeners.current.push(cb);
    return () => {
      listeners.current = listeners.current.filter((fn) => fn !== cb);
    };
  }, []);

  return (
    <DashboardRefreshContext.Provider value={{ refresh, subscribe }}>
      {children}
    </DashboardRefreshContext.Provider>
  );
}

export function useDashboardRefresh() {
  return useContext(DashboardRefreshContext);
}
