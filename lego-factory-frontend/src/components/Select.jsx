import { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styles from './Select.module.css';

/**
 * Select Component - Design System Compliant
 * 
 * A styled select dropdown component with form integration support.
 * Uses forwardRef for compatibility with form libraries.
 * 
 * @component
 * @example
 * // Basic select
 * <Select
 *   label="Status"
 *   options={[
 *     { value: 'pending', label: 'Pending' },
 *     { value: 'active', label: 'Active' },
 *     { value: 'completed', label: 'Completed' }
 *   ]}
 *   value={status}
 *   onChange={(e) => setStatus(e.target.value)}
 * />
 * 
 * // With placeholder and error
 * <Select
 *   label="Country"
 *   placeholder="Select a country"
 *   options={countries}
 *   error="Please select a country"
 * />
 */
const Select = forwardRef(function Select({
  label,
  options = [],
  placeholder,
  value,
  onChange,
  onBlur,
  error,
  helperText,
  disabled = false,
  required = false,
  fullWidth = false,
  className = '',
  id,
  name,
  ...props
}, ref) {
  const selectId = id || `select-${name || label?.toLowerCase().replace(/\s+/g, '-')}`;

  const containerClasses = [
    styles.container,
    fullWidth && styles.fullWidth,
    className
  ].filter(Boolean).join(' ');

  const selectClasses = [
    styles.select,
    error && styles.error,
    disabled && styles.disabled,
  ].filter(Boolean).join(' ');

  return (
    <div className={containerClasses}>
      {label && (
        <label htmlFor={selectId} className={styles.label}>
          {label}
          {required && <span className={styles.required} aria-label="required">*</span>}
        </label>
      )}
      
      <div className={styles.selectWrapper}>
        <select
          ref={ref}
          id={selectId}
          name={name}
          className={selectClasses}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          disabled={disabled}
          required={required}
          aria-invalid={error ? 'true' : 'false'}
          aria-describedby={error ? `${selectId}-error` : helperText ? `${selectId}-helper` : undefined}
          {...props}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
              disabled={option.disabled}
            >
              {option.label}
            </option>
          ))}
        </select>
        <span className={styles.arrow}>▼</span>
      </div>
      
      {error && (
        <div id={`${selectId}-error`} className={styles.errorText} role="alert">
          {error}
        </div>
      )}
      
      {helperText && !error && (
        <div id={`${selectId}-helper`} className={styles.helperText}>
          {helperText}
        </div>
      )}
    </div>
  );
});

Select.propTypes = {
  /** Select label */
  label: PropTypes.string,
  
  /** Array of options */
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      label: PropTypes.string.isRequired,
      disabled: PropTypes.bool,
    })
  ).isRequired,
  
  /** Placeholder option text */
  placeholder: PropTypes.string,
  
  /** Selected value */
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  
  /** Change handler */
  onChange: PropTypes.func,
  
  /** Blur handler */
  onBlur: PropTypes.func,
  
  /** Error message */
  error: PropTypes.string,
  
  /** Helper text */
  helperText: PropTypes.string,
  
  /** Disabled state */
  disabled: PropTypes.bool,
  
  /** Required field */
  required: PropTypes.bool,
  
  /** Full width */
  fullWidth: PropTypes.bool,
  
  /** Additional CSS classes */
  className: PropTypes.string,
  
  /** Select ID */
  id: PropTypes.string,
  
  /** Select name */
  name: PropTypes.string,
};

export default Select;
