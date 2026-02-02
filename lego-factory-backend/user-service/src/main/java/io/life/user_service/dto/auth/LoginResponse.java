package io.life.user_service.dto.auth;

import io.life.user_service.annotation.ApiContract;
import java.time.Instant;

import io.life.user_service.dto.user.UserDto;

/**
 * LoginResponse - Authentication result with JWT token.
 * 
 * API Contract: Frontend receives this from POST /api/auth/login
 * Contains JWT token and user session data.
 */
@ApiContract(
    version = "v1",
    externalSource = "frontend",
    description = "Authentication response with JWT token and user data"
)
public class LoginResponse {

    private String token;
    private String tokenType;
    private Instant expiresAt;
    private UserDto user;

    public LoginResponse(String token, String tokenType, Instant expiresAt, UserDto user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UserDto getUser() {
        return user;
    }
}
