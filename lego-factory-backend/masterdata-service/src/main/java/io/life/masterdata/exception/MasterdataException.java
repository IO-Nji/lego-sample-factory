package io.life.masterdata.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all masterdata service exceptions.
 * Provides machine-readable error codes and contextual details for debugging.
 * 
 * All masterdata exceptions should extend this class to ensure consistent error handling.
 * 
 * Error codes follow format: MASTERDATA_DOMAIN_ERROR
 * Examples: MASTERDATA_PRODUCT_NOT_FOUND, MASTERDATA_BOM_NOT_FOUND, MASTERDATA_VALIDATION_ERROR
 * 
 * @see ResourceNotFoundException
 * @see ValidationException
 */
public class MasterdataException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    /**
     * Constructor with message and default error code
     */
    public MasterdataException(String message) {
        super(message);
        this.errorCode = "MASTERDATA_ERROR";
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message and error code
     */
    public MasterdataException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message, error code, and details
     */
    public MasterdataException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    /**
     * Constructor with message, cause, and error code
     */
    public MasterdataException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    /**
     * Constructor with message, cause, error code, and details
     */
    public MasterdataException(String message, Throwable cause, String errorCode, Map<String, Object> details) {
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
    public MasterdataException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
