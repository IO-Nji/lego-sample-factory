package io.life.order.exception;

/**
 * Exception thrown when production planning operations fail.
 * This includes SimAL integration errors, scheduling failures, and production order validation errors.
 */
public class ProductionPlanningException extends RuntimeException {

    public ProductionPlanningException(String message) {
        super(message);
    }

    public ProductionPlanningException(String message, Throwable cause) {
        super(message, cause);
    }
}
