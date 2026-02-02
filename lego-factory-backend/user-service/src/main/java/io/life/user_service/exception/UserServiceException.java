package io.life.user_service.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all custom exceptions in the user service.
 * Provides machine-readable error codes and contextual details for debugging.
 * 
 * All user service exceptions should extend this class to ensure consistent error handling.
 * 
 * Error codes follow format: USER_DOMAIN_ERROR
 * Examples: USER_NOT_FOUND, USER_UNAUTHORIZED, USER_INVALID_CREDENTIALS
 */
public class UserServiceException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    /**
     * Constructor with message and default error code
     */
    public UserServiceException(String message) {
        super(message);
        this.errorCode = "USER_SERVICE_ERROR";
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message and error code
     */
    public UserServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message, error code, and details
     */
    public UserServiceException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    /**
     * Constructor with message and cause
     */
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "USER_SERVICE_ERROR";
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message, cause, and error code
     */
    public UserServiceException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message, cause, error code, and details
     */
    public UserServiceException(String message, Throwable cause, String errorCode, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    /**
     * Get the machine-readable error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get contextual details for debugging
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    /**
     * Add a detail entry (fluent API)
     */
    public UserServiceException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
