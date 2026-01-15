import axios from "axios";

// API Configuration - uses relative path for production deployment
// In production, nginx-root-proxy routes /api/* to api-gateway
const rawUrl = import.meta.env.VITE_API_GATEWAY_URL || "/api";

// Ensure we don't double-append /api
const apiBasePath = rawUrl.includes('/api') ? rawUrl : `${rawUrl}/api`;

console.log("API Configuration:", { VITE_API_GATEWAY_URL: rawUrl, apiBasePath });

export const LOGIN_ENDPOINT = `${apiBasePath}/auth/login`;
export const USERS_ENDPOINT = `${apiBasePath}/users`;
export const WORKSTATIONS_ENDPOINT = `${apiBasePath}/masterdata/workstations`;
export const PRODUCT_VARIANTS_ENDPOINT = `${apiBasePath}/masterdata/product-variants`;
export const MODULES_ENDPOINT = `${apiBasePath}/masterdata/modules`;
export const PARTS_ENDPOINT = `${apiBasePath}/masterdata/parts`;

// Configure axios to automatically include JWT token in all requests
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("authToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle 401 responses by clearing session
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("authSession");
      localStorage.removeItem("authToken");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export function readStoredSession() {
  const raw = localStorage.getItem("authSession");
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    localStorage.removeItem("authSession");
    return null;
  }
}

export function storeSession(session) {
  localStorage.setItem("authSession", JSON.stringify(session));
  localStorage.setItem("authToken", session.token);
}

export function clearStoredSession() {
  localStorage.removeItem("authSession");
  localStorage.removeItem("authToken");
}
