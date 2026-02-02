package io.life.simal_integration_service.exception;

/**
 * Exception thrown when there is an unauthorized access attempt (invalid tokens, insufficient permissions).
 * Error Code: SIMAL_UNAUTHORIZED
 */
public class UnauthorizedException extends SimalException {
    
    public UnauthorizedException(String message) {
        super(message, "SIMAL_UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, "SIMAL_UNAUTHORIZED");
    }
}
