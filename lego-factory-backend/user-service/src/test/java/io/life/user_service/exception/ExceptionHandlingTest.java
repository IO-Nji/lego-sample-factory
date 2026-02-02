package io.life.user_service.exception;

import io.life.user_service.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for exception handling in user-service.
 */
@SpringBootTest
public class ExceptionHandlingTest {

    @Test
    public void testUserServiceException_HasErrorCode() {
        UserServiceException exception = new UserServiceException("Test error", "USER_TEST_ERROR");
        
        assertThat(exception.getErrorCode()).isEqualTo("USER_TEST_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Test error");
    }

    @Test
    public void testUserServiceException_FluentDetailsAPI() {
        UserServiceException exception = new UserServiceException("User not found", "USER_NOT_FOUND")
                .addDetail("userId", 123L)
                .addDetail("username", "test_user")
                .addDetail("operation", "getUserById");
        
        Map<String, Object> details = exception.getDetails();
        assertThat(details).containsEntry("userId", 123L);
        assertThat(details).containsEntry("username", "test_user");
    }

    @Test
    public void testResourceNotFoundException_HasCorrectErrorCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");
        
        assertThat(exception.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("User not found");
    }

    @Test
    public void testApiErrorResponse_ContainsAllRequiredFields() {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", 123L);
        
        ApiErrorResponse response = ApiErrorResponse.of(
                404, 
                "Not Found", 
                "USER_NOT_FOUND",
                "User not found", 
                "/api/users/123",
                details
        );
        
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getMessage()).isEqualTo("User not found");
    }

    @Test
    public void testErrorCodeFormat_FollowsConvention() {
        UserServiceException exception = new UserServiceException("Test", "USER_VALIDATION_ERROR");
        
        String errorCode = exception.getErrorCode();
        assertThat(errorCode).matches("^[A-Z]+_[A-Z_]+$");
        assertThat(errorCode).startsWith("USER_");
    }
}
