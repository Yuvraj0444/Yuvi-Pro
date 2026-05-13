package com.skyways.user.service;

import com.skyways.common.security.SecretManagerService;
import com.skyways.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LogManager.getLogger(JwtService.class);
    private static final long TOKEN_EXPIRY_HOURS = 24;

    private final SecretManagerService secretManagerService;

    public JwtService(SecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }

    public String generateToken(User user) {
        String secret = secretManagerService.getSecret("JWT_SECRET");
        Instant now = Instant.now();

        return Jwts.builder()
            .subject(user.getUserId().toString())
            .claim("role", user.getRole().name())
            .claim("email", user.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    public Claims validateToken(String token) {
        try {
            String secret = secretManagerService.getSecret("JWT_SECRET");
            return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new com.skyways.common.exception.auth.TokenExpiredException();
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            throw new com.skyways.common.exception.auth.AuthenticationException("Invalid JWT token");
        }
    }

    public long getTokenExpirySeconds() {
        return TOKEN_EXPIRY_HOURS * 3600;
    }
}
