package io.life.user_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.life.user_service.dto.user.UserCreateRequest;
import io.life.user_service.dto.user.UserDto;
import io.life.user_service.dto.user.UserMapper;
import io.life.user_service.dto.user.UserUpdateRequest;
import io.life.user_service.entity.User;
import io.life.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Users", description = "User management operations (Admin only except /me)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user", description = "Returns the currently authenticated user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public UserDto currentUser(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));
        return UserMapper.toDto(user);
    }

    @Operation(summary = "List all users", description = "Returns all users in the system (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of users returned"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> listUsers() {
        return UserMapper.toDtoList(userService.findAll());
    }

    @Operation(summary = "Get user by ID", description = "Returns a specific user by their ID (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found and returned"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto getUserById(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.findById(id)
            .map(UserMapper::toDto)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    @Operation(summary = "Create user", description = "Creates a new user account (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.registerUser(request.getUsername(), request.getPassword(), request.getRole(),
            request.getWorkstationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toDto(user));
    }

    @Operation(summary = "Update user", description = "Updates an existing user's details (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(
            @Parameter(description = "User ID") @PathVariable Long id, 
            @Valid @RequestBody UserUpdateRequest request) {
        User user = userService.updateUser(id, request.getUsername(), request.getRole(), request.getWorkstationId(),
            request.getPassword());
        return UserMapper.toDto(user);
    }

    @Operation(summary = "Delete user", description = "Deletes a user account (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteUser(id);
    }
}
