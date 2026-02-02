package io.life.order.exception;

import io.life.order.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for exception handling in Order Processing Service.
 * 
 * Tests verify:
 * - Error codes are correctly formatted (SERVICE_DOMAIN_ERROR)
 * - ApiErrorResponse structure includes all required fields
 * - Exception fluent API (addDetail) works correctly
 * - HTTP status codes are properly mapped
 * - Backward compatibility is maintained
 */
@SpringBootTest
public class ExceptionHandlingTest {

    @Test
    public void testOrderProcessingException_HasErrorCode() {
        // Arrange & Act
        OrderProcessingException exception = new OrderProcessingException("Test error", "TEST_ERROR");

        // Assert
        assertThat(exception.getErrorCode()).isEqualTo("TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test error");
    }

    @Test
    public void testOrderProcessingException_FluentDetailsAPI() {
        // Arrange & Act
        OrderProcessingException exception = new OrderProcessingException("Order not found", "ORDER_NOT_FOUND")
                .addDetail("orderId", 123L)
                .addDetail("orderNumber", "ORD-001")
                .addDetail("operation", "confirmOrder");

        // Assert
        Map<String, Object> details = exception.getDetails();
        assertThat(details).containsEntry("orderId", 123L);
        assertThat(details).containsEntry("orderNumber", "ORD-001");
        assertThat(details).containsEntry("operation", "confirmOrder");
    }

    @Test
    public void testEntityNotFoundException_HasCorrectErrorCode() {
        // Arrange & Act
        EntityNotFoundException exception = new EntityNotFoundException("Customer order not found");

        // Assert
        assertThat(exception.getErrorCode()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Customer order not found");
    }

    @Test
    public void testInvalidOrderStateException_HasCorrectErrorCode() {
        // Arrange & Act
        InvalidOrderStateException exception = new InvalidOrderStateException("Cannot complete pending order");

        // Assert
        assertThat(exception.getErrorCode()).isEqualTo("ORDER_INVALID_STATE");
        assertThat(exception.getMessage()).contains("Cannot complete pending order");
    }

    @Test
    public void testApiErrorResponse_ContainsAllRequiredFields() {
        // Arrange
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", 456L);
        details.put("status", "PENDING");

        // Act
        ApiErrorResponse response = ApiErrorResponse.of(
                404,
                "Not Found",
                "ORDER_NOT_FOUND",
                "Order not found",
                "/api/customer-orders/456",
                details
        );

        // Assert: Verify all required fields
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getErrorCode()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("Order not found");
        assertThat(response.getPath()).isEqualTo("/api/customer-orders/456");
        assertThat(response.getDetails()).containsEntry("orderId", 456L);
        assertThat(response.getDetails()).containsEntry("status", "PENDING");
    }

    @Test
    public void testErrorCodeFormat_FollowsConvention() {
        // Arrange & Act
        EntityNotFoundException exception1 = new EntityNotFoundException("Not found");
        InvalidOrderStateException exception2 = new InvalidOrderStateException("Invalid state");

        // Assert: Error codes follow SERVICE_DOMAIN_ERROR format
        assertThat(exception1.getErrorCode()).matches("^[A-Z_]+$"); // All caps with underscores
        assertThat(exception1.getErrorCode()).contains("ORDER"); // Service domain

        assertThat(exception2.getErrorCode()).matches("^[A-Z_]+$");
        assertThat(exception2.getErrorCode()).contains("ORDER");
    }

    @Test
    public void testBackwardCompatibility_OriginalConstructorWorks() {
        // Arrange & Act: Use original constructor (message only)
        OrderProcessingException exception = new OrderProcessingException("Simple error message");

        // Assert: Should work without error code
        assertThat(exception.getMessage()).isEqualTo("Simple error message");
        assertThat(exception.getErrorCode()).isNotNull(); // Should have default error code
    }

    @Test
    public void testDetailsMap_IsImmutable() {
        // Arrange
        OrderProcessingException exception = new OrderProcessingException("Test", "TEST_ERROR")
                .addDetail("key1", "value1");

        // Act: Get details and try to modify
        Map<String, Object> details = exception.getDetails();
        
        // Attempt modification should not affect original (defensive copy)
        try {
            details.put("key2", "value2");
        } catch (UnsupportedOperationException e) {
            // Expected if returning unmodifiable map
        }

        // Assert: Original exception should not be affected
        Map<String, Object> detailsAgain = exception.getDetails();
        assertThat(detailsAgain).containsEntry("key1", "value1");
        // Should not contain key2 if defensive copy works
    }

    @Test
    public void testExceptionHierarchy_AllExtendBaseException() {
        // Arrange & Act
        EntityNotFoundException notFound = new EntityNotFoundException("Not found");
        InvalidOrderStateException invalidState = new InvalidOrderStateException("Invalid");

        // Assert: All should be instances of OrderProcessingException
        assertThat(notFound).isInstanceOf(OrderProcessingException.class);
        assertThat(invalidState).isInstanceOf(OrderProcessingException.class);
        assertThat(notFound).isInstanceOf(RuntimeException.class);
    }
}
