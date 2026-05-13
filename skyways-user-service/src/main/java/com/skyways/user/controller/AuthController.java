package com.skyways.user.controller;

import com.skyways.common.dto.ApiResponse;
import com.skyways.user.dto.LoginRequest;
import com.skyways.user.dto.LoginResponse;
import com.skyways.user.dto.RegisterRequest;
import com.skyways.user.entity.User;
import com.skyways.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and JWT-based login")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a new user", description = "Creates a user account and publishes user-registered event to Kafka")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("User registered successfully. userId=" + user.getUserId()));
    }

    @Operation(summary = "Login and receive JWT", description = "Validates credentials and returns a signed JWT for use in Authorization: Bearer headers")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = userService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
