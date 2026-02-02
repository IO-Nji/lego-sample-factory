package io.life.user_service.exception;

/**
 * Exception thrown when there is an unauthorized access attempt.
 * Used for failed logins, insufficient permissions, etc.
 */
public class UnauthorizedException extends UserServiceException {
    
    public UnauthorizedException(String message) {
        super(message, "USER_UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, "USER_UNAUTHORIZED");
    }
}
