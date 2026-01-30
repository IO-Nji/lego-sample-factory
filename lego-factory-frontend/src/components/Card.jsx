import PropTypes from 'prop-types';
import styles from './Card.module.css';

/**
 * Card Component - Design System Compliant
 * 
 * A flexible container component for grouping related content.
 * Supports multiple variants, interactive states, and optional header/footer.
 * 
 * @component
 * @example
 * // Basic card
 * <Card>
 *   <h3>Card Title</h3>
 *   <p>Card content goes here</p>
 * </Card>
 * 
 * // Card with header and footer
 * <Card 
 *   header={<h3>Order #12345</h3>}
 *   footer={<Button>View Details</Button>}
 * >
 *   <p>Order details...</p>
 * </Card>
 * 
 * // Elevated card
 * <Card variant="elevated">
 *   <p>This card has a shadow</p>
 * </Card>
 * 
 * // Interactive card
 * <Card interactive onClick={handleClick}>
 *   <p>Click me!</p>
 * </Card>
 * 
 * // Framed card (double-border style for dashboard panels)
 * <Card variant="framed" title="ORDER STATUS">
 *   <PieChart data={data} />
 * </Card>
 */
function Card({
  children,
  variant = 'default',
  header = null,
  footer = null,
  title = null,
  interactive = false,
  onClick,
  className = '',
  padding = 'normal',
  style = {},
  ...props
}) {
  const isFramed = variant === 'framed';
  
  const cardClasses = [
    styles.card,
    styles[variant],
    !isFramed && styles[`padding-${padding}`],
    interactive && styles.interactive,
    onClick && styles.clickable,
    className
  ].filter(Boolean).join(' ');

  const handleClick = () => {
    if (onClick && typeof onClick === 'function') {
      onClick();
    }
  };

  const handleKeyDown = (e) => {
    if (onClick && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      onClick();
    }
  };

  // Framed variant: outer frame + inner content panel with double border effect
  if (isFramed) {
    return (
      <div
        className={cardClasses}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        role={onClick ? 'button' : undefined}
        tabIndex={onClick ? 0 : undefined}
        style={style}
        {...props}
      >
        {title && <h3 className={styles.framedTitle}>{title}</h3>}
        <div className={styles.framedInner}>
          {header && <div className={styles.header}>{header}</div>}
          <div className={styles.framedBody}>{children}</div>
          {footer && <div className={styles.footer}>{footer}</div>}
        </div>
      </div>
    );
  }

  return (
    <div
      className={cardClasses}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      style={style}
      {...props}
    >
      {header && <div className={styles.header}>{header}</div>}
      <div className={styles.body}>{children}</div>
      {footer && <div className={styles.footer}>{footer}</div>}
    </div>
  );
}

Card.propTypes = {
  /** Card content */
  children: PropTypes.node.isRequired,
  
  /** Visual style variant: 'default', 'elevated', 'outlined', or 'framed' (double-border dashboard style) */
  variant: PropTypes.oneOf(['default', 'elevated', 'outlined', 'framed']),
  
  /** Optional header content */
  header: PropTypes.node,
  
  /** Optional footer content */
  footer: PropTypes.node,
  
  /** Title for framed variant (displayed above inner panel) */
  title: PropTypes.string,
  
  /** Enable hover effects */
  interactive: PropTypes.bool,
  
  /** Click handler (makes card clickable) */
  onClick: PropTypes.func,
  
  /** Additional CSS classes */
  className: PropTypes.string,
  
  /** Padding size (not applicable to framed variant) */
  padding: PropTypes.oneOf(['none', 'small', 'normal', 'large']),
  
  /** Inline styles */
  style: PropTypes.object,
};

export default Card;
