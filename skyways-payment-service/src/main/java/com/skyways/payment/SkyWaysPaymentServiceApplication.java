package com.skyways.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.skyways.payment", "com.skyways.common"})
@EnableDiscoveryClient
public class SkyWaysPaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysPaymentServiceApplication.class, args);
    }
}
