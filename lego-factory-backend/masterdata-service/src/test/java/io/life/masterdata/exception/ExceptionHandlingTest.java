package io.life.masterdata.exception;

import io.life.masterdata.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for exception handling in masterdata-service.
 */
@SpringBootTest
public class ExceptionHandlingTest {

    @Test
    public void testMasterdataException_HasErrorCode() {
        MasterdataException exception = new MasterdataException("Test error", "MASTERDATA_TEST_ERROR");
        
        assertThat(exception.getErrorCode()).isEqualTo("MASTERDATA_TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test error");
    }

    @Test
    public void testMasterdataException_FluentDetailsAPI() {
        MasterdataException exception = new MasterdataException("Product not found", "MASTERDATA_NOT_FOUND")
                .addDetail("productId", 1L)
                .addDetail("productName", "LEGO Model Car")
                .addDetail("operation", "getProduct");
        
        Map<String, Object> details = exception.getDetails();
        assertThat(details).containsEntry("productId", 1L);
        assertThat(details).containsEntry("productName", "LEGO Model Car");
    }

    @Test
    public void testResourceNotFoundException_HasCorrectErrorCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");
        
        assertThat(exception.getErrorCode()).isEqualTo("MASTERDATA_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Product not found");
    }

    @Test
    public void testApiErrorResponse_ContainsAllRequiredFields() {
        Map<String, Object> details = new HashMap<>();
        details.put("productId", 1L);
        
        ApiErrorResponse response = ApiErrorResponse.of(
                404, 
                "Not Found", 
                "MASTERDATA_NOT_FOUND",
                "Product not found", 
                "/api/masterdata/products/1",
                details
        );
        
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getErrorCode()).isEqualTo("MASTERDATA_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("Product not found");
    }

    @Test
    public void testErrorCodeFormat_FollowsConvention() {
        MasterdataException exception = new MasterdataException("Test", "MASTERDATA_VALIDATION_ERROR");
        
        String errorCode = exception.getErrorCode();
        assertThat(errorCode).matches("^[A-Z]+_[A-Z_]+$");
        assertThat(errorCode).startsWith("MASTERDATA_");
    }

    @Test
    public void testBackwardCompatibility_OriginalConstructorWorks() {
        MasterdataException exception = new MasterdataException("Simple error message");
        
        assertThat(exception.getMessage()).isEqualTo("Simple error message");
        assertThat(exception.getErrorCode()).isNotNull();
    }
}
