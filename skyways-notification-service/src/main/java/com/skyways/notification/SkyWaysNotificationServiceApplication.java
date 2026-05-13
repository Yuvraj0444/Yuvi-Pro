package com.skyways.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {"com.skyways.notification", "com.skyways.common"})
@EnableDiscoveryClient
@EnableRetry
public class SkyWaysNotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysNotificationServiceApplication.class, args);
    }
}
