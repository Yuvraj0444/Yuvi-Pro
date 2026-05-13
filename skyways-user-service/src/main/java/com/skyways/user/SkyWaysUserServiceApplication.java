package com.skyways.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// scanBasePackages must include com.skyways.common so that beans defined there
// (SecretManagerService, TripleDESEncryptor, MDCTraceFilter) are picked up.
// Without this, @SpringBootApplication only scans com.skyways.user.* by default.
@SpringBootApplication(scanBasePackages = {"com.skyways.user", "com.skyways.common"})
@EnableDiscoveryClient
public class SkyWaysUserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysUserServiceApplication.class, args);
    }
}
