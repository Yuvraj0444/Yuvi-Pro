package com.skyways.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Scope this filter chain to API and actuator paths only.
        // Anything outside this scope (Swagger UI, OpenAPI docs, webjars) is never
        // processed by Spring Security at all — no matcher lookup, no filter execution.
        http.securityMatcher(new OrRequestMatcher(
            new AntPathRequestMatcher("/api/v1/**"),
            new AntPathRequestMatcher("/actuator/**")
        ));

        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/users/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}