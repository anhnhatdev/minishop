package com.minishop.userservice.controller;

import com.minishop.userservice.dto.request.UpdateUserRequest;
import com.minishop.userservice.dto.request.UpdateUserStatusRequest;
import com.minishop.userservice.dto.response.UserResponse;
import com.minishop.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for user profile and admin management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserResponse userResponse = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile (fullName, phone)")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        String email = authentication.getName();
        UserResponse userResponse = userService.updateCurrentUserProfile(email, request);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: List all registered users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update user account status (ACTIVE, LOCKED, PENDING_VERIFY)")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse updatedUser = userService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(updatedUser);
    }
}
