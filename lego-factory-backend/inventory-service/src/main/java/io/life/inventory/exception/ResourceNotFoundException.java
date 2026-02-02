package io.life.inventory.exception;

import java.util.Map;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * Error Code: INVENTORY_NOT_FOUND
 */
public class ResourceNotFoundException extends InventoryException {
    
    public ResourceNotFoundException(String message) {
        super(message, "INVENTORY_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            "INVENTORY_NOT_FOUND",
            Map.of(
                "resourceName", resourceName,
                "fieldName", fieldName,
                "fieldValue", fieldValue.toString()
            )
        );
    }
}
