package io.life.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.life.user_service.dto.auth.LoginRequest;
import io.life.user_service.dto.auth.LoginResponse;
import io.life.user_service.dto.user.UserMapper;
import io.life.user_service.entity.User;
import io.life.user_service.security.JwtTokenProvider;
import io.life.user_service.security.JwtTokenProvider.JwtToken;
import io.life.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Operation(summary = "User login", 
               description = "Authenticates user credentials and returns a JWT token for API access")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername().trim();
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        JwtToken token = tokenProvider.createToken(user);
        LoginResponse response = new LoginResponse(token.token(), "Bearer", token.expiresAt(), UserMapper.toDto(user));
        return ResponseEntity.ok(response);
    }

}
