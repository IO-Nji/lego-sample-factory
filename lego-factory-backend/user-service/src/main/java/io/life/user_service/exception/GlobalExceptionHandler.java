package io.life.user_service.exception;

import io.life.user_service.dto.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses across the user service.
 * Handles all exceptions and returns standardized error responses with error codes and context.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        String errorCode = ex.getErrorCode();
        Map<String, Object> details = ex.getDetails();
        
        logger.warn("Resource not found: {} (Code: {})", ex.getMessage(), errorCode);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                errorCode,
                ex.getMessage(),
                details,
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            ValidationException ex,
            WebRequest request) {
        
        String errorCode = ex.getErrorCode();
        Map<String, Object> details = ex.getDetails();
        
        logger.warn("Validation error: {} (Code: {})", ex.getMessage(), errorCode);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                errorCode,
                ex.getMessage(),
                details,
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle UnauthorizedException
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request) {
        
        String errorCode = ex.getErrorCode();
        Map<String, Object> details = ex.getDetails();
        
        logger.warn("Unauthorized access: {} (Code: {})", ex.getMessage(), errorCode);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                errorCode,
                ex.getMessage(),
                details,
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle MethodArgumentNotValidException (for @Valid annotation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        
        String detailsMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        logger.warn("Validation failed: {}", detailsMessage);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "USER_VALIDATION_ERROR",
                "Invalid request parameters: " + detailsMessage,
                details,
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle NoHandlerFoundException (404 for endpoints)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            WebRequest request) {
        
        logger.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "USER_ENDPOINT_NOT_FOUND",
                ex.getMessage(),
                Map.of("method", ex.getHttpMethod(), "url", ex.getRequestURL()),
                ex.getRequestURL()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle generic UserServiceException
     */
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleUserServiceException(
            UserServiceException ex,
            WebRequest request) {
        
        String errorCode = ex.getErrorCode();
        Map<String, Object> details = ex.getDetails();
        
        logger.error("User service error: {} (Code: {})", ex.getMessage(), errorCode, ex);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                errorCode,
                ex.getMessage(),
                details,
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle EntityNotFoundException
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {
        
        logger.warn("Entity not found: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "USER_ENTITY_NOT_FOUND",
                ex.getMessage(),
                Map.of("exceptionType", "EntityNotFoundException"),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle IllegalArgumentException (for business logic validation)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        logger.warn("Business logic validation error: {}", ex.getMessage());
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "USER_INVALID_ARGUMENT",
                ex.getMessage(),
                Map.of("exceptionType", "IllegalArgumentException"),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        logger.error("Unexpected error occurred", ex);
        
        ApiErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "USER_INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                Map.of("exceptionType", ex.getClass().getSimpleName()),
                request.getDescription(false).replace("uri=", "")
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Build standardized error response with full context
     */
    private ApiErrorResponse buildErrorResponse(HttpStatus status, String errorCode, String message, Map<String, Object> details, String path) {
        return ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                path,
                details
        );
    }

    /**
     * Build standardized error response (backward compatibility)
     */
    private ApiErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
