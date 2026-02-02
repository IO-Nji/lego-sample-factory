package io.life.user_service.dto.auth;

import io.life.user_service.annotation.ApiContract;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest - Authentication credentials.
 * 
 * API Contract: Frontend sends this to POST /api/auth/login
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "User authentication credentials for login"
)
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
