import PropTypes from 'prop-types';
import styles from './Badge.module.css';

/**
 * Badge Component - Design System Compliant
 * 
 * A small label component for displaying status, counts, or categories.
 * Supports multiple variants and sizes.
 * 
 * @component
 * @example
 * // Status badge
 * <Badge variant="success">Active</Badge>
 * 
 * // Count badge
 * <Badge variant="primary" pill>5</Badge>
 * 
 * // Dot badge
 * <Badge variant="danger" dot />
 */
function Badge({
  children,
  variant = 'primary',
  size = 'medium',
  pill = false,
  dot = false,
  className = '',
}) {
  const badgeClasses = [
    styles.badge,
    styles[variant],
    styles[size],
    pill && styles.pill,
    dot && styles.dot,
    className
  ].filter(Boolean).join(' ');

  return (
    <span className={badgeClasses}>
      {!dot && children}
    </span>
  );
}

Badge.propTypes = {
  /** Badge content */
  children: PropTypes.node,
  
  /** Color variant */
  variant: PropTypes.oneOf([
    'primary',
    'secondary',
    'success',
    'danger',
    'warning',
    'info',
    'gray'
  ]),
  
  /** Badge size */
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  
  /** Use pill shape (fully rounded) */
  pill: PropTypes.bool,
  
  /** Show as dot indicator only */
  dot: PropTypes.bool,
  
  /** Additional CSS classes */
  className: PropTypes.string,
};

export default Badge;
