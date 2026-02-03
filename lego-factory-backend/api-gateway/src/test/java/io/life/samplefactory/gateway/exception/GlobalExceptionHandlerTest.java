package io.life.samplefactory.gateway.exception;

import io.life.samplefactory.gateway.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for GlobalExceptionHandler.
 * Tests all exception handler methods for proper HTTP status, error format, and logging.
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Nested
    @DisplayName("ResourceNotFoundException Handling")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should return 404 status for resource not found")
        void shouldReturn404ForResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Customer order 123 not found");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("Should include exception message in response")
        void shouldIncludeExceptionMessage() {
            String message = "Order item not found";
            ResourceNotFoundException ex = new ResourceNotFoundException(message);
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            assertThat(response.getBody().getError()).isEqualTo(message);
        }

        @Test
        @DisplayName("Should include request path in response")
        void shouldIncludeRequestPath() {
            when(webRequest.getDescription(false)).thenReturn("uri=/api/customer-orders/42");
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            assertThat(response.getBody().getPath()).isEqualTo("/api/customer-orders/42");
        }

        @Test
        @DisplayName("Should set message to 'Resource not found'")
        void shouldSetMessageToResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestamp() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ValidationException Handling")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should return 400 status for validation error")
        void shouldReturn400ForValidationError() {
            ValidationException ex = new ValidationException("Invalid quantity: must be positive");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should include validation error message")
        void shouldIncludeValidationErrorMessage() {
            String validationMessage = "Product ID cannot be null";
            ValidationException ex = new ValidationException(validationMessage);
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getBody().getError()).isEqualTo(validationMessage);
        }

        @Test
        @DisplayName("Should set message to 'Validation error'")
        void shouldSetMessageToValidationError() {
            ValidationException ex = new ValidationException("Details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Validation error");
        }

        @Test
        @DisplayName("Should handle empty validation message")
        void shouldHandleEmptyMessage() {
            ValidationException ex = new ValidationException("");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getError()).isEmpty();
        }
    }

    @Nested
    @DisplayName("UnauthorizedException Handling")
    class UnauthorizedExceptionTests {

        @Test
        @DisplayName("Should return 401 status for unauthorized access")
        void shouldReturn401ForUnauthorized() {
            UnauthorizedException ex = new UnauthorizedException("Invalid credentials");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should include unauthorized error message")
        void shouldIncludeUnauthorizedErrorMessage() {
            String authMessage = "Token expired";
            UnauthorizedException ex = new UnauthorizedException(authMessage);
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(ex, webRequest);
            
            assertThat(response.getBody().getError()).isEqualTo(authMessage);
        }

        @Test
        @DisplayName("Should set message to 'Unauthorized'")
        void shouldSetMessageToUnauthorized() {
            UnauthorizedException ex = new UnauthorizedException("Details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("Should handle missing token scenario")
        void shouldHandleMissingToken() {
            UnauthorizedException ex = new UnauthorizedException("Authorization header missing");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException Handling")
    class MethodArgumentNotValidExceptionTests {

        @Test
        @DisplayName("Should return 400 status for method argument not valid")
        void shouldReturn400ForInvalidMethodArgument() {
            MethodArgumentNotValidException ex = createMethodArgumentNotValidException(
                    List.of(new FieldError("order", "quantity", "must be positive"))
            );
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should include field error details")
        void shouldIncludeFieldErrorDetails() {
            MethodArgumentNotValidException ex = createMethodArgumentNotValidException(
                    List.of(new FieldError("order", "quantity", "must be positive"))
            );
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);
            
            assertThat(response.getBody().getDetails()).contains("quantity");
            assertThat(response.getBody().getDetails()).contains("must be positive");
        }

        @Test
        @DisplayName("Should handle multiple field errors")
        void shouldHandleMultipleFieldErrors() {
            MethodArgumentNotValidException ex = createMethodArgumentNotValidException(
                    List.of(
                            new FieldError("order", "quantity", "must be positive"),
                            new FieldError("order", "productId", "cannot be null")
                    )
            );
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);
            
            assertThat(response.getBody().getDetails()).contains("quantity");
            assertThat(response.getBody().getDetails()).contains("productId");
        }

        @Test
        @DisplayName("Should set message to 'Validation failed'")
        void shouldSetMessageToValidationFailed() {
            MethodArgumentNotValidException ex = createMethodArgumentNotValidException(
                    List.of(new FieldError("order", "field", "error"))
            );
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("Should handle empty field errors")
        void shouldHandleEmptyFieldErrors() {
            MethodArgumentNotValidException ex = createMethodArgumentNotValidException(List.of());
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        private MethodArgumentNotValidException createMethodArgumentNotValidException(List<FieldError> fieldErrors) {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            
            return ex;
        }
    }

    @Nested
    @DisplayName("RuntimeException Handling")
    class RuntimeExceptionTests {

        @Test
        @DisplayName("Should return 500 status for runtime exception")
        void shouldReturn500ForRuntimeException() {
            RuntimeException ex = new RuntimeException("Database connection failed");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should include exception message for runtime error")
        void shouldIncludeExceptionMessageForRuntimeError() {
            String errorMessage = "Service unavailable";
            RuntimeException ex = new RuntimeException(errorMessage);
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);
            
            assertThat(response.getBody().getError()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Should set message to 'Service error'")
        void shouldSetMessageToServiceError() {
            RuntimeException ex = new RuntimeException("Details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Service error");
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            NullPointerException ex = new NullPointerException("Null value encountered");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void shouldHandleIllegalArgumentException() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 status for generic exception")
        void shouldReturn500ForGenericException() {
            Exception ex = new Exception("Unexpected error");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should mask actual error message for security")
        void shouldMaskActualErrorMessage() {
            Exception ex = new Exception("Sensitive internal error details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(ex, webRequest);
            
            // Should NOT expose internal details
            assertThat(response.getBody().getError()).isEqualTo("An unexpected error occurred. Please try again later.");
        }

        @Test
        @DisplayName("Should set message to 'Internal server error'")
        void shouldSetMessageToInternalServerError() {
            Exception ex = new Exception("Details");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(ex, webRequest);
            
            assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
        }

        @Test
        @DisplayName("Should handle IOException")
        void shouldHandleIOException() throws Exception {
            Exception ex = new java.io.IOException("File not readable");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGlobalException(ex, webRequest);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Error Response Format")
    class ErrorResponseFormatTests {

        @Test
        @DisplayName("Should include all required fields in error response")
        void shouldIncludeAllRequiredFields() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
            when(webRequest.getDescription(false)).thenReturn("uri=/api/orders/1");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);
            
            ErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isPositive();
            assertThat(body.getMessage()).isNotEmpty();
            assertThat(body.getError()).isNotNull();
            assertThat(body.getPath()).isNotEmpty();
            assertThat(body.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should strip 'uri=' prefix from path")
        void shouldStripUriPrefixFromPath() {
            when(webRequest.getDescription(false)).thenReturn("uri=/api/test/endpoint");
            ValidationException ex = new ValidationException("Error");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getBody().getPath()).isEqualTo("/api/test/endpoint");
            assertThat(response.getBody().getPath()).doesNotContain("uri=");
        }

        @Test
        @DisplayName("Should handle path without uri prefix")
        void shouldHandlePathWithoutUriPrefix() {
            when(webRequest.getDescription(false)).thenReturn("/api/direct/path");
            ValidationException ex = new ValidationException("Error");
            
            ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(ex, webRequest);
            
            assertThat(response.getBody().getPath()).isEqualTo("/api/direct/path");
        }
    }
}
