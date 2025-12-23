import axios from 'axios';

// Use relative path for production deployment via Nginx proxy
// In development, Vite proxy will handle the /api routing
const rawUrl = import.meta.env.VITE_API_GATEWAY_URL;
let API_GATEWAY_URL;
if (!rawUrl || rawUrl === '' ) {
  API_GATEWAY_URL = '/api';
} else {
  // Ensure baseURL includes /api when pointing to a gateway host
  const hasPath = /\/api\/?$/.test(rawUrl);
  API_GATEWAY_URL = hasPath ? rawUrl : `${rawUrl.replace(/\/$/, '')}/api`;
}

console.log('API_GATEWAY_URL configured as:', API_GATEWAY_URL);
console.log('VITE_API_GATEWAY_URL env var:', import.meta.env.VITE_API_GATEWAY_URL);

const api = axios.create({
  baseURL: API_GATEWAY_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

console.log('axios instance created with baseURL:', api.defaults.baseURL);

// Add request interceptor to include JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle 401/404/503 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const currentPath = globalThis.location.pathname;

    if (status === 401) {
      // Token expired or invalid, clear session and redirect to home
      localStorage.removeItem('authToken');
      localStorage.removeItem('authSession');
      
      // Only redirect if not already on login or home page
      if (currentPath !== '/' && currentPath !== '/login') {
        console.log('401 Unauthorized - redirecting to home page');
        globalThis.location.href = '/?reason=unauthenticated';
      }
    } else if (status === 404) {
      // API endpoint not found - could be backend down or misconfigured
      console.warn('404 Not Found:', error.config?.url);
      // Don't redirect for 404, let the calling component handle it
    } else if (status === 503 || status === 502 || status === 504) {
      // Backend service unavailable
      console.error('Backend service unavailable:', status);
      // Could redirect to error page or show global notification
    } else if (!status && error.code === 'ERR_NETWORK') {
      // Network error - backend is likely down
      console.error('Network error - backend may be down');
      // Redirect to home with error message if on protected route
      if (currentPath !== '/' && currentPath !== '/login') {
        globalThis.location.href = '/?reason=backend_down';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
