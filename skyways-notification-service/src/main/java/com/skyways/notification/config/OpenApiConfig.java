package com.skyways.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SkyWays Notification Service API")
                .description("Kafka-driven email notification service — listens on booking-confirmed and booking-cancelled topics, sends SendGrid emails with retry and DLQ fallback. No public REST endpoints.")
                .version("1.0.0")
                .contact(new Contact().name("SkyWays Team").email("api@skyways.com")));
    }
}