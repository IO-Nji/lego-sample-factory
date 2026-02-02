package io.life.inventory.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all inventory service domain exceptions.
 * 
 * Provides context through error codes and additional details map for debugging.
 * All domain-specific exceptions should extend this class.
 * 
 * Error Code Format: INVENTORY_DOMAIN_ERROR
 */
public class InventoryException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    /**
     * Create exception with message only.
     * Uses default error code: INVENTORY_ERROR
     */
    public InventoryException(String message) {
        super(message);
        this.errorCode = "INVENTORY_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and cause.
     * Uses default error code: INVENTORY_ERROR
     */
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVENTORY_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and specific error code.
     * 
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code (e.g., "INVENTORY_NOT_FOUND")
     */
    public InventoryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with full context.
     * 
     * @param message Human-readable error message
     * @param errorCode Machine-readable error code
     * @param details Additional context for debugging
     */
    public InventoryException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Create exception with full context and underlying cause.
     */
    public InventoryException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }
    
    /**
     * Add a detail entry to the exception context.
     * Supports fluent API for easy chaining.
     */
    public InventoryException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
