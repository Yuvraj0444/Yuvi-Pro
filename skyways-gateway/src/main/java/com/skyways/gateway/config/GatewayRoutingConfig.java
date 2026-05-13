package com.skyways.gateway.config;

import com.skyways.gateway.filter.LoggingFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator skyWaysRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            .route("user-service", r -> r
                .path("/api/v1/users/**", "/api/v1/auth/**")
                .filters(f -> f.filter(new LoggingFilter()))
                .uri("lb://skyways-user-service"))

            .route("flight-service", r -> r
                .path("/api/v1/flights/**")
                .filters(f -> f.filter(new LoggingFilter()))
                .uri("lb://skyways-flight-service"))

            .route("booking-service", r -> r
                .path("/api/v1/bookings/**")
                .filters(f -> f.filter(new LoggingFilter()))
                .uri("lb://skyways-booking-service"))

            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f.filter(new LoggingFilter()))
                .uri("lb://skyways-payment-service"))

            // API-docs proxy routes for Swagger UI aggregation
            .route("user-service-docs", r -> r
                .path("/user-service/v3/api-docs")
                .filters(f -> f.rewritePath("/user-service(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-user-service"))

            .route("flight-service-docs", r -> r
                .path("/flight-service/v3/api-docs")
                .filters(f -> f.rewritePath("/flight-service(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-flight-service"))

            .route("booking-service-docs", r -> r
                .path("/booking-service/v3/api-docs")
                .filters(f -> f.rewritePath("/booking-service(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-booking-service"))

            .route("payment-service-docs", r -> r
                .path("/payment-service/v3/api-docs")
                .filters(f -> f.rewritePath("/payment-service(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-payment-service"))

            .route("notification-service-docs", r -> r
                .path("/notification-service/v3/api-docs")
                .filters(f -> f.rewritePath("/notification-service(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-notification-service"))

            .route("saga-orchestrator-docs", r -> r
                .path("/saga-orchestrator/v3/api-docs")
                .filters(f -> f.rewritePath("/saga-orchestrator(?<remaining>/.*)", "${remaining}"))
                .uri("lb://skyways-saga-orchestrator"))

            .build();
    }
}