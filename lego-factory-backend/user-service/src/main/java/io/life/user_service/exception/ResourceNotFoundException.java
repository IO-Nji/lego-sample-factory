package io.life.user_service.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found.
 * Typically used for users, roles, and workstations.
 */
public class ResourceNotFoundException extends UserServiceException {
    
    public ResourceNotFoundException(String message) {
        super(message, "USER_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            "USER_NOT_FOUND",
            Map.of(
                "resourceName", resourceName,
                "fieldName", fieldName,
                "fieldValue", fieldValue != null ? fieldValue.toString() : "null"
            )
        );
    }
}
