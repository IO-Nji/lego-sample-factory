package io.life.inventory.exception;

import java.util.Map;

/**
 * Exception thrown when there is a validation error.
 * 
 * Error Code: INVENTORY_VALIDATION_ERROR
 */
public class ValidationException extends InventoryException {
    
    public ValidationException(String message) {
        super(message, "INVENTORY_VALIDATION_ERROR");
    }

    public ValidationException(String message, Throwable cause) {
        super(message, "INVENTORY_VALIDATION_ERROR", null, cause);
    }
    
    public ValidationException(String message, String field, Object invalidValue) {
        super(
            message,
            "INVENTORY_VALIDATION_ERROR",
            Map.of(
                "field", field,
                "invalidValue", invalidValue != null ? invalidValue.toString() : "null"
            )
        );
    }
}
