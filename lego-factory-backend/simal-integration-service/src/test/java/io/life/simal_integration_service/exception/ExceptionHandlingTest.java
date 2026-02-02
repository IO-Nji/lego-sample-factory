package io.life.simal_integration_service.exception;

import io.life.simal_integration_service.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for exception handling in simal-integration-service.
 */
@SpringBootTest
public class ExceptionHandlingTest {

    @Test
    public void testSimalException_HasErrorCode() {
        SimalException exception = new SimalException("Test error", "SIMAL_TEST_ERROR");
        
        assertThat(exception.getErrorCode()).isEqualTo("SIMAL_TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test error");
    }

    @Test
    public void testSimalException_FluentDetailsAPI() {
        SimalException exception = new SimalException("Schedule not found", "SIMAL_NOT_FOUND")
                .addDetail("scheduleId", 1L)
                .addDetail("productionOrderId", 10L)
                .addDetail("operation", "getSchedule");
        
        Map<String, Object> details = exception.getDetails();
        assertThat(details).containsEntry("scheduleId", 1L);
        assertThat(details).containsEntry("productionOrderId", 10L);
    }

    @Test
    public void testResourceNotFoundException_HasCorrectErrorCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Schedule not found");
        
        assertThat(exception.getErrorCode()).isEqualTo("SIMAL_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Schedule not found");
    }

    @Test
    public void testApiErrorResponse_ContainsAllRequiredFields() {
        Map<String, Object> details = new HashMap<>();
        details.put("scheduleId", 1L);
        
        ApiErrorResponse response = ApiErrorResponse.of(
                404, 
                "Not Found", 
                "SIMAL_NOT_FOUND",
                "Schedule not found", 
                "/api/simal/schedules/1",
                details
        );
        
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getErrorCode()).isEqualTo("SIMAL_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("Schedule not found");
    }

    @Test
    public void testErrorCodeFormat_FollowsConvention() {
        SimalException exception = new SimalException("Test", "SIMAL_VALIDATION_ERROR");
        
        String errorCode = exception.getErrorCode();
        assertThat(errorCode).matches("^[A-Z]+_[A-Z_]+$");
        assertThat(errorCode).startsWith("SIMAL_");
    }
}
