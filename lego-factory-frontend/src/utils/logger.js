/**
 * Logger Utility - Environment-aware logging
 * 
 * Only outputs logs in development mode.
 * Provides consistent log formatting with timestamps and prefixes.
 * 
 * @example
 * import { logger } from '../utils/logger';
 * 
 * logger.info('ModulesSupermarket', 'Order confirmed', { orderId: 123 });
 * logger.debug('ProductionPlanning', 'Fetched orders', orders.length);
 * logger.warn('Inventory', 'Low stock detected');
 * logger.error('API', 'Request failed', error);
 */

const isDevelopment = import.meta.env.DEV || import.meta.env.MODE === 'development';

const formatTimestamp = () => {
  return new Date().toLocaleTimeString('en-US', { 
    hour12: false, 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  });
};

const formatPrefix = (level, context) => {
  const timestamp = formatTimestamp();
  const levelTag = level.toUpperCase().padEnd(5);
  return `[${timestamp}] [${levelTag}] [${context}]`;
};

export const logger = {
  /**
   * Debug level - verbose development logging
   */
  debug: (context, message, ...args) => {
    if (isDevelopment) {
      console.log(`%c${formatPrefix('DEBUG', context)}`, 'color: #6b7280', message, ...args);
    }
  },

  /**
   * Info level - general information
   */
  info: (context, message, ...args) => {
    if (isDevelopment) {
      console.log(`%c${formatPrefix('INFO', context)}`, 'color: #3b82f6', message, ...args);
    }
  },

  /**
   * Success level - successful operations
   */
  success: (context, message, ...args) => {
    if (isDevelopment) {
      console.log(`%c${formatPrefix('OK', context)}`, 'color: #10b981', message, ...args);
    }
  },

  /**
   * Warning level - potential issues
   */
  warn: (context, message, ...args) => {
    if (isDevelopment) {
      console.warn(`%c${formatPrefix('WARN', context)}`, 'color: #f59e0b', message, ...args);
    }
  },

  /**
   * Error level - errors (always logged)
   */
  error: (context, message, ...args) => {
    // Errors are always logged, even in production
    console.error(`${formatPrefix('ERROR', context)}`, message, ...args);
  },

  /**
   * Group related logs
   */
  group: (context, label) => {
    if (isDevelopment) {
      console.group(`${formatPrefix('GROUP', context)} ${label}`);
    }
  },

  /**
   * End log group
   */
  groupEnd: () => {
    if (isDevelopment) {
      console.groupEnd();
    }
  },

  /**
   * Table display for arrays/objects
   */
  table: (context, data, columns) => {
    if (isDevelopment) {
      console.log(`${formatPrefix('TABLE', context)}`);
      console.table(data, columns);
    }
  },
};

export default logger;
