package io.life.masterdata.exception;

/**
 * Exception thrown when there is an unauthorized access attempt.
 */
public class UnauthorizedException extends MasterdataException {
    
    public UnauthorizedException(String message) {
        super(message, "MASTERDATA_UNAUTHORIZED");
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, "MASTERDATA_UNAUTHORIZED");
    }
}
