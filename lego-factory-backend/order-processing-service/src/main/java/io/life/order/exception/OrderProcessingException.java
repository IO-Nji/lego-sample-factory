package io.life.order.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all order processing domain exceptions.
 * 
 * Provides context through error codes and additional details map for debugging.
 * All domain-specific exceptions should extend this class.
 * 
 * Error Code Format: ORDER_DOMAIN_ERROR
 * 
 * @see EntityNotFoundException
 * @see InvalidOrderStateException
 * @see InsufficientQuantityException
 */
public class OrderProcessingException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    /**
     * Create exception with message only.
     * Uses default error code: ORDER_PROCESSING_ERROR
     */
    public OrderProcessingException(String message) {
        super(message);
        this.errorCode = "ORDER_PROCESSING_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and cause.
     * Uses default error code: ORDER_PROCESSING_ERROR
     */
    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ORDER_PROCESSING_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and specific error code.
     * 
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code (e.g., "ORDER_NOT_FOUND")
     */
    public OrderProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with full context.
     * 
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code
     * @param details Additional context for debugging (order ID, item ID, etc.)
     */
    public OrderProcessingException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Create exception with full context and underlying cause.
     * 
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code
     * @param details Additional context for debugging
     * @param cause Underlying exception that caused this error
     */
    public OrderProcessingException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Get the machine-readable error code.
     * 
     * @return Error code (e.g., "ORDER_NOT_FOUND")
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get additional context details for debugging.
     * 
     * @return Immutable copy of details map
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }
    
    /**
     * Add a detail entry to the exception context.
     * Supports fluent API for easy chaining.
     * 
     * @param key Detail key (e.g., "orderNumber", "itemId")
     * @param value Detail value
     * @return This exception for method chaining
     */
    public OrderProcessingException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
