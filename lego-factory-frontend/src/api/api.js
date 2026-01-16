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

// Add response interceptor to handle auth errors ONLY
// Let components handle their own API errors (404, 500, etc.)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const currentPath = globalThis.location?.pathname || '/';
    const requestUrl = error.config?.url || '';

    // ONLY handle authentication errors - 401 and 403
    // BUT: Don't log out if it's a background operation (like updating production order after SimAL schedule)
    // We check if the request URL contains certain patterns that shouldn't trigger logout
    const isBackgroundUpdate = requestUrl.includes('/schedule') && error.config?.method === 'patch';
    
    if ((status === 401 || status === 403) && !isBackgroundUpdate) {
      // Token expired, invalid, or unauthorized - clear session and redirect to home
      console.log(`${status} ${status === 401 ? 'Unauthorized' : 'Forbidden'} - clearing session`);
      localStorage.removeItem('authToken');
      localStorage.removeItem('authSession');
      
      // Redirect to home page if not already there
      if (currentPath !== '/') {
        console.log('Redirecting to home page');
        globalThis.location.href = '/?reason=session_expired';
      }
    } 
    // For all other errors (404, 500, network errors, etc.), 
    // let the calling component handle them
    else {
      console.warn('API error:', status || 'Network Error', requestUrl);
    }
    
    return Promise.reject(error);
  }
);

export default api;
