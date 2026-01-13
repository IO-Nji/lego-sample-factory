import PropTypes from 'prop-types';
import styles from './Button.module.css';

/**
 * Button Component - Design System Compliant
 * 
 * A versatile button component with multiple variants, sizes, and states.
 * Uses CSS Modules and design system variables for consistency.
 * 
 * @component
 * @example
 * // Primary button
 * <Button variant="primary" onClick={handleClick}>Submit</Button>
 * 
 * // Button with icon
 * <Button variant="success" icon={<CheckIcon />}>Save</Button>
 * 
 * // Loading button
 * <Button loading={true}>Processing...</Button>
 * 
 * // Full width button
 * <Button fullWidth variant="danger">Delete Account</Button>
 */
function Button({ 
  children, 
  variant = 'primary', 
  size = 'medium', 
  disabled = false, 
  onClick, 
  type = 'button',
  className = '',
  icon = null,
  fullWidth = false,
  loading = false,
  ariaLabel,
  ...props 
}) {
  // Build className from CSS modules
  const buttonClasses = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth && styles.fullWidth,
    loading && styles.loading,
    className
  ].filter(Boolean).join(' ');

  return (
    <button
      type={type}
      className={buttonClasses}
      onClick={onClick}
      disabled={disabled || loading}
      aria-label={ariaLabel}
      aria-busy={loading}
      {...props}
    >
      {loading && (
        <span className={styles.spinner} aria-hidden="true">
          ⟳
        </span>
      )}
      {icon && !loading && (
        <span className={styles.icon} aria-hidden="true">
          {icon}
        </span>
      )}
      <span>{children}</span>
    </button>
  );
}

Button.propTypes = {
  /** Button content */
  children: PropTypes.node.isRequired,
  
  /** Visual style variant */
  variant: PropTypes.oneOf([
    'primary', 
    'secondary', 
    'success', 
    'danger', 
    'warning', 
    'outline', 
    'ghost', 
    'link'
  ]),
  
  /** Button size */
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  
  /** Disabled state */
  disabled: PropTypes.bool,
  
  /** Click handler */
  onClick: PropTypes.func,
  
  /** HTML button type */
  type: PropTypes.oneOf(['button', 'submit', 'reset']),
  
  /** Additional CSS classes */
  className: PropTypes.string,
  
  /** Optional icon element (displayed before text) */
  icon: PropTypes.node,
  
  /** Make button full width of container */
  fullWidth: PropTypes.bool,
  
  /** Loading state (disables button and shows spinner) */
  loading: PropTypes.bool,
  
  /** Accessibility label */
  ariaLabel: PropTypes.string,
};

export default Button;
