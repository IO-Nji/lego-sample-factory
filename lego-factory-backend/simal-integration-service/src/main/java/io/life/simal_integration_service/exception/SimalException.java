package io.life.simal_integration_service.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception class for all SimAL Integration service exceptions.
 * 
 * Supports error codes and contextual details for better debugging and API error responses.
 * 
 * Error Code Format: SIMAL_DOMAIN_ERROR
 * Examples:
 * - SIMAL_SCHEDULE_FAILED: Production scheduling failed
 * - SIMAL_TASK_NOT_FOUND: Task not found in schedule
 * - SIMAL_INVALID_PRODUCTION_ORDER: Invalid production order data
 * 
 * Usage:
 * <pre>
 * throw new SimalException(
 *     "Failed to schedule production order",
 *     "SIMAL_SCHEDULE_FAILED",
 *     Map.of("productionOrderId", orderId, "reason", "Insufficient capacity")
 * );
 * </pre>
 */
public class SimalException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    /**
     * Create exception with message only (uses default error code)
     */
    public SimalException(String message) {
        super(message);
        this.errorCode = "SIMAL_SERVICE_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and cause (uses default error code)
     */
    public SimalException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SIMAL_SERVICE_ERROR";
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message and error code
     */
    public SimalException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with message, cause, and error code
     */
    public SimalException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    /**
     * Create exception with full context (message, error code, details)
     */
    public SimalException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>(details);
    }
    
    /**
     * Create exception with full context including cause
     */
    public SimalException(String message, Throwable cause, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>(details);
    }
    
    /**
     * Get the error code for this exception
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get debugging details (returns defensive copy)
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }
    
    /**
     * Add a detail entry (fluent API)
     */
    public SimalException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
