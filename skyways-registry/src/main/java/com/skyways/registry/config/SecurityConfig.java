package com.skyways.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Opens the Eureka dashboard and all endpoints without authentication.
     *
     * CSRF must be disabled for /eureka/** — Eureka peer replication uses
     * plain POSTs with no CSRF token, and Spring Security 6 would block them.
     *
     * For production: remove permitAll() and re-enable HTTP Basic here,
     * then set EUREKA_USER / EUREKA_PASSWORD in the deployment secrets.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/eureka/**", "/actuator/**")
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }
}