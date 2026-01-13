import PropTypes from 'prop-types';
import styles from './LoadingSpinner.module.css';

/**
 * LoadingSpinner Component - Design System Compliant
 * 
 * A loading indicator component with multiple size and color variants.
 * Can be used inline or as a full-page overlay.
 * 
 * @component
 * @example
 * // Basic spinner
 * <LoadingSpinner />
 * 
 * // Large spinner with text
 * <LoadingSpinner size="large" text="Loading data..." />
 * 
 * // Full page overlay
 * <LoadingSpinner overlay text="Processing your request..." />
 * 
 * // Custom color
 * <LoadingSpinner variant="success" />
 */
function LoadingSpinner({
  size = 'medium',
  variant = 'primary',
  text,
  overlay = false,
  className = '',
}) {
  const spinnerClasses = [
    styles.spinner,
    styles[size],
    styles[variant],
    className
  ].filter(Boolean).join(' ');

  const content = (
    <div className={styles.container}>
      <div className={spinnerClasses}>
        <div className={styles.ring}></div>
        <div className={styles.ring}></div>
        <div className={styles.ring}></div>
        <div className={styles.ring}></div>
      </div>
      {text && <div className={styles.text}>{text}</div>}
    </div>
  );

  if (overlay) {
    return (
      <div className={styles.overlay} role="dialog" aria-modal="true" aria-label="Loading">
        {content}
      </div>
    );
  }

  return content;
}

LoadingSpinner.propTypes = {
  /** Spinner size */
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  
  /** Color variant */
  variant: PropTypes.oneOf(['primary', 'secondary', 'white']),
  
  /** Optional loading text */
  text: PropTypes.string,
  
  /** Show as full-page overlay */
  overlay: PropTypes.bool,
  
  /** Additional CSS classes */
  className: PropTypes.string,
};

export default LoadingSpinner;
