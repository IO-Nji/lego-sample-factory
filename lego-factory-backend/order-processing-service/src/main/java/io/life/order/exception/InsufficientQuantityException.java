package io.life.order.exception;

import java.util.Map;

/**
 * Exception thrown when there is insufficient quantity available.
 * 
 * Error Code: ORDER_INSUFFICIENT_STOCK
 */
public class InsufficientQuantityException extends OrderProcessingException {

    public InsufficientQuantityException(String message) {
        super(message, "ORDER_INSUFFICIENT_STOCK");
    }

    public InsufficientQuantityException(String message, Throwable cause) {
        super(message, "ORDER_INSUFFICIENT_STOCK", null, cause);
    }

    public InsufficientQuantityException(Integer required, Integer available) {
        super(
            String.format("Insufficient quantity. Required: %d, Available: %d", required, available),
            "ORDER_INSUFFICIENT_STOCK",
            Map.of(
                "required", required,
                "available", available,
                "shortage", required - available
            )
        );
    }
}
