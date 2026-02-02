package io.life.simal_integration_service.dto;

import io.life.simal_integration_service.annotation.ApiContract;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized API error response for SimAL Integration service.
 * 
 * Provides consistent error format with machine-readable error codes
 * and contextual debugging details.
 * 
 * Structure:
 * - timestamp: When the error occurred
 * - status: HTTP status code
 * - error: HTTP reason phrase
 * - errorCode: Machine-readable code (e.g., SIMAL_SCHEDULE_FAILED)
 * - message: Human-readable error description
 * - path: Request path where error occurred
 * - details: Additional debugging context (productionOrderId, taskId, etc.)
 */
@ApiContract(
    version = "v1",
    externalSource = "order-processing-service, production planners",
    description = "Standard error response format for all SimAL API errors. Includes machine-readable error codes and debugging context for production scheduling issues."
)
public class ApiErrorResponse {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;
    private String path;
    private Map<String, Object> details;
    
    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.details = new HashMap<>();
    }
    
    public ApiErrorResponse(int status, String error, String errorCode, String message, String path, Map<String, Object> details) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Static factory for responses with error code and details
     */
    public static ApiErrorResponse of(int status, String error, String errorCode, String message, String path, Map<String, Object> details) {
        return new ApiErrorResponse(status, error, errorCode, message, path, details);
    }
    
    /**
     * Static factory for simple responses (backward compatibility)
     */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, null, message, path, null);
    }
    
    // Getters and Setters
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
