package com.skyways.flight.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig gdsConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(
                com.skyways.common.exception.flight.GDSConnectionException.class,
                java.io.IOException.class,
                org.springframework.web.client.ResourceAccessException.class
            )
            .build();

        CircuitBreakerConfig skyscannerConfig = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .recordExceptions(
                com.skyways.common.exception.flight.SkyscannerAPIException.class,
                java.io.IOException.class
            )
            .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("gds-cb", gdsConfig);
        registry.circuitBreaker("skyscanner-cb", skyscannerConfig);
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .retryExceptions(
                java.io.IOException.class,
                org.springframework.web.client.ResourceAccessException.class
            )
            .build();

        RetryRegistry registry = RetryRegistry.ofDefaults();
        registry.retry("gds-retry", retryConfig);
        registry.retry("skyscanner-retry", retryConfig);
        return registry;
    }
}
