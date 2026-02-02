package io.life.simal_integration_service.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found (schedules, tasks, etc).
 * Error Code: SIMAL_NOT_FOUND
 */
public class ResourceNotFoundException extends SimalException {
    
    public ResourceNotFoundException(String message) {
        super(message, "SIMAL_NOT_FOUND");
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, "SIMAL_NOT_FOUND");
    }
    
    /**
     * Create exception with resource context
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            "SIMAL_NOT_FOUND",
            Map.of(
                "resourceName", resourceName,
                "fieldName", fieldName,
                "fieldValue", fieldValue.toString()
            )
        );
    }
}
