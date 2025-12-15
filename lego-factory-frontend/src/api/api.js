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

// Add response interceptor to handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid, clear session and redirect to login
      localStorage.removeItem('authToken');
      localStorage.removeItem('authSession');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
