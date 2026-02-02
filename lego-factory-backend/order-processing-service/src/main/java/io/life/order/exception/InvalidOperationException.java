package io.life.order.exception;

import java.util.Map;

/**
 * Exception thrown when an invalid operation is attempted.
 * 
 * Error Code: ORDER_INVALID_OPERATION
 */
public class InvalidOperationException extends OrderProcessingException {

    public InvalidOperationException(String message) {
        super(message, "ORDER_INVALID_OPERATION");
    }

    public InvalidOperationException(String message, Throwable cause) {
        super(message, "ORDER_INVALID_OPERATION", null, cause);
    }
    
    /**
     * Create exception with operation context.
     */
    public InvalidOperationException(String message, String operation, String reason) {
        super(
            message,
            "ORDER_INVALID_OPERATION",
            Map.of(
                "operation", operation,
                "reason", reason
            )
        );
    }
}
