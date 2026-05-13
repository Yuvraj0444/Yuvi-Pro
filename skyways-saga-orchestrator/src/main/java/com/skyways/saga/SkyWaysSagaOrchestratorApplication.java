package com.skyways.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.skyways.saga", "com.skyways.common"})
@EnableDiscoveryClient
public class SkyWaysSagaOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysSagaOrchestratorApplication.class, args);
    }
}
