package io.life.inventory.exception;

/**
 * Exception thrown when there is an unauthorized access attempt.
 * 
 * Error Code: INVENTORY_UNAUTHORIZED
 */
public class UnauthorizedException extends InventoryException {
    
    public UnauthorizedException(String message) {
        super(message, "INVENTORY_UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, "INVENTORY_UNAUTHORIZED", null, cause);
    }
}
