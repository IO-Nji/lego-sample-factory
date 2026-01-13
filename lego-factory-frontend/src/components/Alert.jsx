import { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styles from './Alert.module.css';

/**
 * Alert Component - Design System Compliant
 * 
 * Displays contextual feedback messages with auto-dismiss capability.
 * Supports multiple variants for different message types.
 * 
 * @component
 * @example
 * // Success alert
 * <Alert variant="success" title="Success!">
 *   Your order has been created successfully.
 * </Alert>
 * 
 * // Error alert with close button
 * <Alert variant="danger" onClose={() => setError(null)}>
 *   An error occurred while processing your request.
 * </Alert>
 * 
 * // Info alert with auto-dismiss
 * <Alert variant="info" autoDismiss={5000} onDismiss={handleDismiss}>
 *   This message will disappear in 5 seconds.
 * </Alert>
 */
function Alert({
  children,
  variant = 'info',
  title,
  onClose,
  autoDismiss,
  onDismiss,
  icon,
  className = '',
}) {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    if (autoDismiss && typeof autoDismiss === 'number') {
      const timer = setTimeout(() => {
        setIsVisible(false);
        if (onDismiss) {
          onDismiss();
        }
      }, autoDismiss);

      return () => clearTimeout(timer);
    }
  }, [autoDismiss, onDismiss]);

  if (!isVisible) {
    return null;
  }

  const handleClose = () => {
    setIsVisible(false);
    if (onClose) {
      onClose();
    }
  };

  const alertClasses = [
    styles.alert,
    styles[variant],
    className
  ].filter(Boolean).join(' ');

  // Default icons for each variant
  const defaultIcons = {
    success: '✓',
    danger: '✕',
    warning: '⚠',
    info: 'ℹ',
  };

  const displayIcon = icon !== undefined ? icon : defaultIcons[variant];

  return (
    <div className={alertClasses} role="alert">
      {displayIcon && (
        <div className={styles.icon} aria-hidden="true">
          {displayIcon}
        </div>
      )}
      
      <div className={styles.content}>
        {title && <div className={styles.title}>{title}</div>}
        <div className={styles.message}>{children}</div>
      </div>
      
      {onClose && (
        <button
          className={styles.closeButton}
          onClick={handleClose}
          aria-label="Close alert"
          type="button"
        >
          ✕
        </button>
      )}
    </div>
  );
}

Alert.propTypes = {
  /** Alert content */
  children: PropTypes.node.isRequired,
  
  /** Alert variant */
  variant: PropTypes.oneOf(['success', 'danger', 'warning', 'info']),
  
  /** Optional title */
  title: PropTypes.string,
  
  /** Close button handler */
  onClose: PropTypes.func,
  
  /** Auto-dismiss after N milliseconds */
  autoDismiss: PropTypes.number,
  
  /** Callback when alert is dismissed (by auto-dismiss or close button) */
  onDismiss: PropTypes.func,
  
  /** Custom icon (overrides default) */
  icon: PropTypes.node,
  
  /** Additional CSS classes */
  className: PropTypes.string,
};

export default Alert;
