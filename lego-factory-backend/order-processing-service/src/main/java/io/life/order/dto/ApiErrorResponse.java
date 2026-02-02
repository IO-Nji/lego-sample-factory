package io.life.order.dto;

import io.life.order.annotation.ApiContract;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard API error response DTO.
 * Provides consistent error response structure across all endpoints.
 * 
 * Used by GlobalExceptionHandler to return standardized error information.
 * 
 * Example JSON:
 * {
 *   "timestamp": "2026-02-02T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "errorCode": "ORDER_NOT_FOUND",
 *   "message": "Order with ID 123 not found",
 *   "path": "/api/customer-orders/123",
 *   "details": {
 *     "entityType": "CustomerOrder",
 *     "entityId": 123
 *   }
 * }
 */
@ApiContract(
    version = "v1",
    externalSource = "All frontend clients",
    description = "Standard error response format for all API errors. " +
                  "Includes HTTP status, machine-readable error code, human-readable message, " +
                  "and optional debugging context in details map."
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

    public ApiErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
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

    // Static factory method for simple errors (backward compatibility)
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(status, error, message, path);
    }
    
    // Static factory method for full error context
    public static ApiErrorResponse of(int status, String error, String errorCode, String message, String path, Map<String, Object> details) {
        return new ApiErrorResponse(status, error, errorCode, message, path, details);
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
