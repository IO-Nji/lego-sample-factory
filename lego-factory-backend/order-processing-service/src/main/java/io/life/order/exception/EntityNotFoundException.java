package io.life.order.exception;

import java.util.Map;

/**
 * Exception thrown when an entity is not found in the database.
 * 
 * Error Code: ORDER_NOT_FOUND
 */
public class EntityNotFoundException extends OrderProcessingException {

    public EntityNotFoundException(String message) {
        super(message, "ORDER_NOT_FOUND");
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, "ORDER_NOT_FOUND", null, cause);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(
            String.format("%s with ID %d not found", entityName, id),
            "ORDER_NOT_FOUND",
            Map.of("entityType", entityName, "entityId", id)
        );
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super(
            String.format("%s with identifier %s not found", entityName, identifier),
            "ORDER_NOT_FOUND",
            Map.of("entityType", entityName, "identifier", identifier)
        );
    }
}
