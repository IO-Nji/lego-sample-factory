package io.life.simal_integration_service.exception;

import java.util.Map;

/**
 * Exception thrown when there is a validation error (schedule data, task parameters, etc).
 * Error Code: SIMAL_VALIDATION_ERROR
 */
public class ValidationException extends SimalException {
    
    public ValidationException(String message) {
        super(message, "SIMAL_VALIDATION_ERROR");
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, "SIMAL_VALIDATION_ERROR");
    }
    
    /**
     * Create exception with field context
     */
    public ValidationException(String message, String field, Object invalidValue) {
        super(
            message,
            "SIMAL_VALIDATION_ERROR",
            Map.of(
                "field", field,
                "invalidValue", invalidValue.toString()
            )
        );
    }
}
