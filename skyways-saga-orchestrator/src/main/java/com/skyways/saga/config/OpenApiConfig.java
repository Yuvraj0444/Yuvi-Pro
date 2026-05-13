package com.skyways.saga.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sagaOrchestratorOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SkyWays Saga Orchestrator API")
                .description("Choreography coordinator for the booking transaction saga — drives seat reservation, payment, confirmation, and compensation flows via Kafka. No public REST endpoints.")
                .version("1.0.0")
                .contact(new Contact().name("SkyWays Team").email("api@skyways.com")));
    }
}