/**
 * useDashboardData - Universal Dashboard Data Fetching Hook
 * 
 * Provides a standardized way to fetch and manage dashboard data
 * with loading states, error handling, and auto-refresh.
 * 
 * @example
 * const { data, loading, error, refresh, refreshing } = useDashboardData({
 *   fetchFn: fetchAdminData,
 *   refreshInterval: 30000,
 *   transformFn: transformResponse,
 * });
 */

import { useState, useEffect, useCallback, useRef } from 'react';

const useDashboardData = ({
  fetchFn,
  refreshInterval = 30000, // Default 30s auto-refresh
  transformFn = (data) => data,
  deps = [],
  initialData = null,
}) => {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  
  const lastDataRef = useRef(null);
  const mountedRef = useRef(true);

  // Fetch data function
  const fetchData = useCallback(async (isRefresh = false) => {
    if (!fetchFn) return;
    
    try {
      if (isRefresh) {
        setRefreshing(true);
      } else if (!data) {
        setLoading(true);
      }
      setError(null);

      const response = await fetchFn();
      
      // Only update if component is still mounted
      if (!mountedRef.current) return;
      
      const transformedData = transformFn(response);
      
      // Shallow comparison to prevent unnecessary re-renders
      const dataString = JSON.stringify(transformedData);
      if (lastDataRef.current !== dataString) {
        setData(transformedData);
        lastDataRef.current = dataString;
        setLastUpdated(new Date());
      }
    } catch (err) {
      if (!mountedRef.current) return;
      setError(err.message || 'Failed to load data');
      console.error('Dashboard data fetch error:', err);
    } finally {
      if (mountedRef.current) {
        setLoading(false);
        setRefreshing(false);
      }
    }
  }, [fetchFn, transformFn, data]);

  // Manual refresh handler
  const refresh = useCallback(() => {
    fetchData(true);
  }, [fetchData]);

  // Dismiss error
  const dismissError = useCallback(() => {
    setError(null);
  }, []);

  // Initial fetch and interval setup
  useEffect(() => {
    mountedRef.current = true;
    
    fetchData(false);
    
    let interval;
    if (refreshInterval > 0) {
      interval = setInterval(() => fetchData(true), refreshInterval);
    }
    
    return () => {
      mountedRef.current = false;
      if (interval) clearInterval(interval);
    };
  }, [fetchData, refreshInterval, ...deps]);

  return {
    data,
    loading,
    refreshing,
    error,
    refresh,
    dismissError,
    lastUpdated,
    isInitialLoad: loading && !data,
  };
};

export default useDashboardData;
