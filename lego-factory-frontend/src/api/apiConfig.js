import axios from "axios";

// API endpoint paths (relative to baseURL in api.js)
// The axios instance in api.js already has baseURL='/api'
// So these paths should NOT include '/api' prefix to avoid /api/api/... duplication

console.log("API Configuration loaded - endpoints will use axios baseURL from api.js");

export const LOGIN_ENDPOINT = `/auth/login`;
export const USERS_ENDPOINT = `/users`;
export const WORKSTATIONS_ENDPOINT = `/masterdata/workstations`;
export const PRODUCT_VARIANTS_ENDPOINT = `/masterdata/product-variants`;
export const MODULES_ENDPOINT = `/masterdata/modules`;
export const PARTS_ENDPOINT = `/masterdata/parts`;

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
