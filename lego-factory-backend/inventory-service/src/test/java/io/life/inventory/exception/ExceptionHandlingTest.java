package io.life.inventory.exception;

import io.life.inventory.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for exception handling in inventory-service.
 * 
 * Tests exception classes directly without HTTP layer complexity.
 * Validates:
 * - Error codes follow SERVICE_DOMAIN_ERROR format
 * - ApiErrorResponse structure is complete
 * - Fluent details API (addDetail method)
 * - Backward compatibility (original constructors)
 * - Exception hierarchy
 */
@SpringBootTest
public class ExceptionHandlingTest {

    @Test
    public void testInventoryException_HasErrorCode() {
        InventoryException exception = new InventoryException("Test error", "INVENTORY_TEST_ERROR");
        
        assertThat(exception.getErrorCode()).isEqualTo("INVENTORY_TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test error");
    }

    @Test
    public void testInventoryException_FluentDetailsAPI() {
        InventoryException exception = new InventoryException("Stock not found", "INVENTORY_NOT_FOUND")
                .addDetail("workstationId", 7L)
                .addDetail("itemType", "PRODUCT")
                .addDetail("itemId", 1L);
        
        Map<String, Object> details = exception.getDetails();
        assertThat(details).containsEntry("workstationId", 7L);
        assertThat(details).containsEntry("itemType", "PRODUCT");
        assertThat(details).containsEntry("itemId", 1L);
    }

    @Test
    public void testResourceNotFoundException_HasCorrectErrorCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Stock record not found");
        
        assertThat(exception.getErrorCode()).isEqualTo("INVENTORY_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Stock record not found");
    }

    @Test
    public void testApiErrorResponse_ContainsAllRequiredFields() {
        Map<String, Object> details = new HashMap<>();
        details.put("workstationId", 7L);
        details.put("itemType", "PRODUCT");
        
        ApiErrorResponse response = ApiErrorResponse.of(
                404, 
                "Not Found", 
                "INVENTORY_NOT_FOUND",
                "Stock record not found", 
                "/api/stock",
                details
        );
        
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getErrorCode()).isEqualTo("INVENTORY_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("Stock record not found");
        assertThat(response.getPath()).isEqualTo("/api/stock");
        assertThat(response.getDetails()).containsEntry("workstationId", 7L);
    }

    @Test
    public void testErrorCodeFormat_FollowsConvention() {
        InventoryException exception = new InventoryException("Test", "INVENTORY_VALIDATION_ERROR");
        
        String errorCode = exception.getErrorCode();
        assertThat(errorCode).matches("^[A-Z]+_[A-Z_]+$"); // Uppercase with underscores
        assertThat(errorCode).startsWith("INVENTORY_");
    }

    @Test
    public void testBackwardCompatibility_OriginalConstructorWorks() {
        // Original constructor (message only) should still work
        InventoryException exception = new InventoryException("Simple error message");
        
        assertThat(exception.getMessage()).isEqualTo("Simple error message");
        assertThat(exception.getErrorCode()).isNotNull();
    }
}
