package com.skyways.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String userId;
    private String role;
}
