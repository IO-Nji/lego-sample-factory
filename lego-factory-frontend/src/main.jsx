import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import './styles.css';
import { AuthProvider } from './context/AuthContext.jsx';
import { DashboardRefreshProvider } from './context/DashboardRefreshContext';
import "./api/apiConfig.js"; // Initialize axios interceptors

const rootElement = document.getElementById("root");

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <DashboardRefreshProvider>
          <App />
        </DashboardRefreshProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
