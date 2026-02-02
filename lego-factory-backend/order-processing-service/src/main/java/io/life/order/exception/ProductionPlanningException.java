package io.life.order.exception;

import java.util.Map;

/**
 * Exception thrown when production planning operations fail.
 * This includes SimAL integration errors, scheduling failures, and production order validation errors.
 * 
 * Error Code: ORDER_PRODUCTION_PLANNING_ERROR
 */
public class ProductionPlanningException extends OrderProcessingException {

    public ProductionPlanningException(String message) {
        super(message, "ORDER_PRODUCTION_PLANNING_ERROR");
    }

    public ProductionPlanningException(String message, Throwable cause) {
        super(message, "ORDER_PRODUCTION_PLANNING_ERROR", null, cause);
    }
    
    /**
     * Create exception with production order context.
     */
    public ProductionPlanningException(String message, Long productionOrderId, String reason) {
        super(
            message,
            "ORDER_PRODUCTION_PLANNING_ERROR",
            Map.of(
                "productionOrderId", productionOrderId,
                "reason", reason
            )
        );
    }
}
