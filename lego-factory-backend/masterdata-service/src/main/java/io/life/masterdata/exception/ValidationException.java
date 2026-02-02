package io.life.masterdata.exception;

import java.util.Map;

/**
 * Exception thrown when there is a validation error.
 * Used for BOM validation, product configuration errors, etc.
 */
public class ValidationException extends MasterdataException {
    
    public ValidationException(String message) {
        super(message, "MASTERDATA_VALIDATION_ERROR");
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause, "MASTERDATA_VALIDATION_ERROR");
    }
    
    public ValidationException(String message, String field, Object invalidValue) {
        super(
            message,
            "MASTERDATA_VALIDATION_ERROR",
            Map.of(
                "field", field,
                "invalidValue", invalidValue != null ? invalidValue.toString() : "null"
            )
        );
    }
}
