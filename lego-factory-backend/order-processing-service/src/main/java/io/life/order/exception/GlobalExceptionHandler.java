package io.life.order.exception;

import io.life.order.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Global exception handler for the Order Processing Service.
 * Provides consistent error response format across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {
        logger.warn("Entity not found: {} (Code: {})", ex.getMessage(), ex.getErrorCode());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }

    /**
     * Handle invalid order state exceptions
     */
    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStateException(
            InvalidOrderStateException ex,
            WebRequest request) {
        logger.warn("Invalid order state: {} (Code: {})", ex.getMessage(), ex.getErrorCode());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }

    /**
     * Handle invalid operation exceptions
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOperationException(
            InvalidOperationException ex,
            WebRequest request) {
        logger.warn("Invalid operation: {} (Code: {})", ex.getMessage(), ex.getErrorCode());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }

    /**
     * Handle insufficient quantity exceptions
     */
    @ExceptionHandler(InsufficientQuantityException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientQuantityException(
            InsufficientQuantityException ex,
            WebRequest request) {
        logger.warn("Insufficient quantity: {} (Code: {})", ex.getMessage(), ex.getErrorCode());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }
    
    /**
     * Handle production planning exceptions
     */
    @ExceptionHandler(ProductionPlanningException.class)
    public ResponseEntity<ApiErrorResponse> handleProductionPlanningException(
            ProductionPlanningException ex,
            WebRequest request) {
        logger.error("Production planning error: {} (Code: {})", ex.getMessage(), ex.getErrorCode(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }

    /**
     * Handle order processing exceptions
     */
    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderProcessingException(
            OrderProcessingException ex,
            WebRequest request) {
        logger.error("Order processing error: {} (Code: {})", ex.getMessage(), ex.getErrorCode(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getDescription(false)
        );
    }

    /**
     * Handle illegal state exceptions (from service layer state validations)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getDescription(false)
        );
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getDescription(false)
        );
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        logger.error("Unexpected error occurred", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred: " + ex.getMessage(),
                null,
                request.getDescription(false)
        );
    }

    /**
     * Build standard error response using ApiErrorResponse DTO (backward compatible).
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path) {
        ApiErrorResponse error = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return new ResponseEntity<>(error, status);
    }
    
    /**
     * Build standard error response with full context (errorCode and details).
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            Map<String, Object> details,
            String path) {
        ApiErrorResponse error = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                path,
                details
        );
        return new ResponseEntity<>(error, status);
    }
}
