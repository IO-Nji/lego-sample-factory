package io.life.user_service.exception;

import java.util.Map;

/**
 * Exception thrown when there is a validation error.
 * Used for password validation, username validation, etc.
 */
public class ValidationException extends UserServiceException {
    
    public ValidationException(String message) {
        super(message, "USER_VALIDATION_ERROR");
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, "USER_VALIDATION_ERROR");
    }
    
    public ValidationException(String message, String field, Object invalidValue) {
        super(
            message,
            "USER_VALIDATION_ERROR",
            Map.of(
                "field", field,
                "invalidValue", invalidValue != null ? invalidValue.toString() : "null"
            )
        );
    }
}
