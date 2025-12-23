import PropTypes from 'prop-types';
import '../styles/StandardPage.css';

/**
 * StandardPageHeader - Reusable page header component
 * Provides consistent styling across all pages
 * Based on ProductsPage design pattern
 */
export default function PageHeader({ title, subtitle, actions, icon }) {
  return (
    <div className="standard-page-header">
      <div className="standard-header-content">
        <h1>{icon && <span>{icon} </span>}{title}</h1>
        {subtitle && <p className="standard-page-subtitle">{subtitle}</p>}
      </div>
      {actions && actions.length > 0 && (
        <div className="standard-header-actions">
          {actions.map((action, index) => (
            <button
              key={index}
              className={`standard-action-btn ${action.variant || ''}`}
              onClick={action.onClick}
              title={action.title}
              disabled={action.disabled}
            >
              {action.icon && <span>{action.icon} </span>}
              {action.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

PageHeader.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  actions: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      onClick: PropTypes.func.isRequired,
      icon: PropTypes.string,
      title: PropTypes.string,
      variant: PropTypes.string, // 'secondary', etc.
      disabled: PropTypes.bool,
    })
  ),
};

PageHeader.defaultProps = {
  subtitle: null,
  icon: null,
  actions: null,
};
