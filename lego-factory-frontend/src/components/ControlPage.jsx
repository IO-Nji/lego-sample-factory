import PropTypes from 'prop-types';
import PageHeader from './PageHeader';
import StatCard from './StatCard';
import ErrorNotification from './ErrorNotification';
import '../styles/StandardPage.css';

/**
 * ControlPage - Standardized layout for admin control pages
 * 
 * Layout Structure:
 * - Row 1: Statistics cards in responsive grid
 * - Row 2: Table (60%) + Optional Form (40%) OR Full-width table
 */
export default function ControlPage({ 
  title, 
  subtitle, 
  icon,
  statistics = [],
  tableContent,
  formContent,
  error,
  onErrorClose
}) {
  return (
    <div className="standard-page-container">
      <PageHeader
        title={title}
        subtitle={subtitle}
        icon={icon}
      />
      
      {error && <ErrorNotification message={error} onClose={onErrorClose} />}

      {/* Row 1: Statistics Cards Grid */}
      {statistics.length > 0 && (
        <section style={{ 
          marginTop: '1.5rem',
          marginBottom: '1rem'
        }}>
          <div style={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'center',
            gap: '1rem',
            maxWidth: '100%'
          }}>
            {statistics.map((stat, index) => (
              <StatCard
                key={index}
                value={stat.value}
                label={stat.label}
                variant={stat.variant || 'primary'}
                icon={stat.icon}
                onClick={stat.onClick}
              />
            ))}
          </div>
        </section>
      )}

      {/* Row 2: Table + Optional Form */}
      <section className="form-section" style={{ marginTop: '1.5rem' }}>
        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: formContent ? '60% 40%' : '100%', 
          gap: '1rem', 
          alignItems: 'start',
          maxWidth: '100%'
        }}>
          
          {/* Left Column: Table */}
          <div style={{ minWidth: 0 }}>
            {tableContent}
          </div>

          {/* Right Column: Form (if provided) */}
          {formContent && (
            <div style={{ 
              display: 'flex', 
              flexDirection: 'column',
              margin: '1.5rem',
              padding: '1.5rem',
              background: 'var(--card-bg)',
              borderRadius: 'var(--card-border-radius)',
              boxShadow: 'var(--shadow-sm)'
            }}>
              {formContent}
            </div>
          )}

        </div>
      </section>
    </div>
  );
}

ControlPage.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  icon: PropTypes.string,
  statistics: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    label: PropTypes.string.isRequired,
    variant: PropTypes.oneOf(['primary', 'secondary', 'success', 'danger', 'warning', 'info']),
    icon: PropTypes.string,
    onClick: PropTypes.func
  })),
  tableContent: PropTypes.node.isRequired,
  formContent: PropTypes.node,
  error: PropTypes.string,
  onErrorClose: PropTypes.func
};
