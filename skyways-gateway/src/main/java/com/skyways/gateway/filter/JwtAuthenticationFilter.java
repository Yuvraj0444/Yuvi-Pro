package com.skyways.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LogManager.getLogger(JwtAuthenticationFilter.class);

    private static final String FALLBACK_SECRET = "smtCEhxliX8nJ4uORBK6FQrUf2g7IWYSeZNT5LoAdDqHyc3b";

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/flights/search",
        "/actuator",
        "/swagger-ui",
        "/webjars",
        "/v3/api-docs",
        "/user-service/v3/api-docs",
        "/flight-service/v3/api-docs",
        "/booking-service/v3/api-docs",
        "/payment-service/v3/api-docs",
        "/notification-service/v3/api-docs",
        "/saga-orchestrator/v3/api-docs"
    );

    // Resolved once at startup — same cascade as SecretManagerService (env → file → fallback)
    private final String jwtSecret = resolveJwtSecret();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            log.error("JWT validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * Resolves JWT_SECRET using the same three-tier cascade as SecretManagerService:
     *   1. OS environment variable JWT_SECRET
     *   2. scripts/secrets.env file (searched relative to CWD and parent dirs)
     *   3. Hard-coded dev fallback (matches scripts/secrets.env default)
     */
    private static String resolveJwtSecret() {
        // Tier 1 — environment variable
        String value = System.getenv("JWT_SECRET");
        if (value != null && !value.isBlank()) {
            log.info("Gateway JWT secret resolved from environment variable");
            return value;
        }

        // Tier 2 — JVM system property
        value = System.getProperty("JWT_SECRET");
        if (value != null && !value.isBlank()) {
            log.info("Gateway JWT secret resolved from system property");
            return value;
        }

        // Tier 3 — secrets.env file
        List<Path> candidates = List.of(
            Paths.get("scripts", "secrets.env"),
            Paths.get("..", "scripts", "secrets.env"),
            Paths.get("..", "..", "scripts", "secrets.env")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                String fromFile = readKeyFromFile(candidate, "JWT_SECRET");
                if (fromFile != null && !fromFile.isBlank()) {
                    log.info("Gateway JWT secret resolved from {}", candidate.toAbsolutePath());
                    return fromFile;
                }
            }
        }

        // Tier 4 — dev fallback (same value as secrets.env default)
        log.warn("JWT_SECRET not found in env/file — using built-in dev fallback. Set JWT_SECRET for production.");
        return FALLBACK_SECRET;
    }

    private static String readKeyFromFile(Path path, String key) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(key + "=")) {
                    String val = line.substring(key.length() + 1).trim();
                    return val.startsWith("REPLACE_WITH") ? null : val;
                }
            }
        } catch (IOException e) {
            log.warn("Could not read secrets file {}: {}", path, e.getMessage());
        }
        return null;
    }
}