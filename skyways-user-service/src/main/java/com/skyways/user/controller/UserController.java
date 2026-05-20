package com.skyways.user.controller;

import com.skyways.common.dto.ApiResponse;
import com.skyways.user.dto.UserProfileDto;
import com.skyways.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile", description = "Fetch and update the authenticated user's profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        UserProfileDto profile = userService.getProfile(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody UserProfileDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        UserProfileDto updated = userService.updateProfile(UUID.fromString(userId), dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}