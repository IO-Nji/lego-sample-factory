import PropTypes from 'prop-types';
import '../styles/FeatureCard.css';

/**
 * Standard Feature Card Component
 * Compact card that expands on hover to show description
 */
function FeatureCard({ icon, title, description }) {
  return (
    <div className="feature-card">
      <div className="feature-card-content">
        <div className="feature-card-icon">{icon}</div>
        <h3 className="feature-card-title">{title}</h3>
      </div>
      <div className="feature-card-description">
        <p>{description}</p>
      </div>
    </div>
  );
}

FeatureCard.propTypes = {
  icon: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
};

export default FeatureCard;
