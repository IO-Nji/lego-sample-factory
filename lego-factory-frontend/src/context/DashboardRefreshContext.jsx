import React, { createContext, useContext, useCallback, useRef, useMemo } from "react";
import PropTypes from "prop-types";

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

  const value = useMemo(() => ({ refresh, subscribe }), [refresh, subscribe]);

  return (
    <DashboardRefreshContext.Provider value={value}>
      {children}
    </DashboardRefreshContext.Provider>
  );
}

DashboardRefreshProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export function useDashboardRefresh() {
  return useContext(DashboardRefreshContext);
}
