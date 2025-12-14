import axios from "axios";

// Force the correct API base URL for Docker deployment
// Use nginx proxy path instead of direct API Gateway port
const API_BASE_URL = "http://localhost";

// Determine the correct API base path
// Since we're using nginx proxy, always append /api
const apiBasePath = `${API_BASE_URL}/api`;

console.log("API Configuration:", { API_BASE_URL, apiBasePath });

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
