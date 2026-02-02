package io.life.order.exception;

import java.util.Map;

/**
 * Exception thrown when an order state transition is invalid.
 * 
 * Error Code: ORDER_INVALID_STATE
 */
public class InvalidOrderStateException extends OrderProcessingException {

    public InvalidOrderStateException(String message) {
        super(message, "ORDER_INVALID_STATE");
    }

    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, "ORDER_INVALID_STATE", null, cause);
    }

    public InvalidOrderStateException(String currentState, String requestedState) {
        super(
            String.format("Cannot transition from state '%s' to state '%s'", currentState, requestedState),
            "ORDER_INVALID_STATE",
            Map.of(
                "currentState", currentState,
                "requestedState", requestedState
            )
        );
    }
}
