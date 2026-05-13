package com.skyways.flight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"com.skyways.flight", "com.skyways.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class SkyWaysFlightServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysFlightServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
