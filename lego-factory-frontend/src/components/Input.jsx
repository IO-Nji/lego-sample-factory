import { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styles from './Input.module.css';

/**
 * Input Component - Design System Compliant
 * 
 * A flexible input component with support for different types, states, and addons.
 * Uses forwardRef for form library compatibility (React Hook Form, etc.).
 * 
 * @component
 * @example
 * // Basic input
 * <Input
 *   label="Email"
 *   type="email"
 *   placeholder="Enter your email"
 *   value={email}
 *   onChange={(e) => setEmail(e.target.value)}
 * />
 * 
 * // With error
 * <Input
 *   label="Password"
 *   type="password"
 *   error="Password must be at least 8 characters"
 * />
 * 
 * // With helper text and prefix
 * <Input
 *   label="Amount"
 *   type="number"
 *   prefix="$"
 *   helperText="Enter amount in USD"
 * />
 */
const Input = forwardRef(function Input({
  label,
  type = 'text',
  placeholder,
  value,
  onChange,
  onBlur,
  onFocus,
  error,
  helperText,
  disabled = false,
  required = false,
  readOnly = false,
  fullWidth = false,
  prefix,
  suffix,
  className = '',
  id,
  name,
  autoComplete,
  ...props
}, ref) {
  const inputId = id || `input-${name || label?.toLowerCase().replace(/\s+/g, '-')}`;

  const containerClasses = [
    styles.container,
    fullWidth && styles.fullWidth,
    className
  ].filter(Boolean).join(' ');

  const inputClasses = [
    styles.input,
    error && styles.error,
    disabled && styles.disabled,
    prefix && styles.hasPrefix,
    suffix && styles.hasSuffix,
  ].filter(Boolean).join(' ');

  return (
    <div className={containerClasses}>
      {label && (
        <label htmlFor={inputId} className={styles.label}>
          {label}
          {required && <span className={styles.required} aria-label="required">*</span>}
        </label>
      )}
      
      <div className={styles.inputWrapper}>
        {prefix && <span className={styles.prefix}>{prefix}</span>}
        
        <input
          ref={ref}
          id={inputId}
          name={name}
          type={type}
          className={inputClasses}
          placeholder={placeholder}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          onFocus={onFocus}
          disabled={disabled}
          readOnly={readOnly}
          required={required}
          autoComplete={autoComplete}
          aria-invalid={error ? 'true' : 'false'}
          aria-describedby={error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined}
          {...props}
        />
        
        {suffix && <span className={styles.suffix}>{suffix}</span>}
      </div>
      
      {error && (
        <div id={`${inputId}-error`} className={styles.errorText} role="alert">
          {error}
        </div>
      )}
      
      {helperText && !error && (
        <div id={`${inputId}-helper`} className={styles.helperText}>
          {helperText}
        </div>
      )}
    </div>
  );
});

Input.propTypes = {
  /** Input label */
  label: PropTypes.string,
  
  /** Input type */
  type: PropTypes.oneOf([
    'text', 'email', 'password', 'number', 'tel', 'url', 'search', 'date', 'time', 'datetime-local'
  ]),
  
  /** Placeholder text */
  placeholder: PropTypes.string,
  
  /** Input value */
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  
  /** Change handler */
  onChange: PropTypes.func,
  
  /** Blur handler */
  onBlur: PropTypes.func,
  
  /** Focus handler */
  onFocus: PropTypes.func,
  
  /** Error message */
  error: PropTypes.string,
  
  /** Helper text */
  helperText: PropTypes.string,
  
  /** Disabled state */
  disabled: PropTypes.bool,
  
  /** Required field */
  required: PropTypes.bool,
  
  /** Read-only state */
  readOnly: PropTypes.bool,
  
  /** Full width */
  fullWidth: PropTypes.bool,
  
  /** Prefix content (icon or text) */
  prefix: PropTypes.node,
  
  /** Suffix content (icon or text) */
  suffix: PropTypes.node,
  
  /** Additional CSS classes */
  className: PropTypes.string,
  
  /** Input ID */
  id: PropTypes.string,
  
  /** Input name */
  name: PropTypes.string,
  
  /** Autocomplete attribute */
  autoComplete: PropTypes.string,
};

export default Input;
