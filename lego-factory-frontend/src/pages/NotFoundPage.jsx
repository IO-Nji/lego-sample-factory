import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

function NotFoundPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handleGoHome = () => {
    navigate("/");
  };

  const handleGoToDashboard = () => {
    navigate("/dashboard");
  };

  return (
    <div className="not-found-page" style={{
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      minHeight: "70vh",
      padding: "2rem",
      textAlign: "center"
    }}>
      <h1 style={{ fontSize: "4rem", margin: "0", color: "#e63946" }}>404</h1>
      <h2 style={{ fontSize: "1.5rem", margin: "1rem 0", color: "#333" }}>Page Not Found</h2>
      <p style={{ fontSize: "1rem", color: "#666", maxWidth: "500px", marginBottom: "2rem" }}>
        The page you are looking for doesn't exist or you don't have permission to access it.
      </p>
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", justifyContent: "center" }}>
        <button
          onClick={handleGoHome}
          className="primary-link"
          style={{ padding: "0.75rem 1.5rem" }}
        >
          Go to Home
        </button>
        {isAuthenticated && (
          <button
            onClick={handleGoToDashboard}
            className="secondary-link"
            style={{ padding: "0.75rem 1.5rem" }}
          >
            Go to Dashboard
          </button>
        )}
      </div>
    </div>
  );
}

export default NotFoundPage;
