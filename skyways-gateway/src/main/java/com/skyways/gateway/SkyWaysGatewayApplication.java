package com.skyways.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// Exclude Spring Cloud Gateway's gRPC auto-configurations — this project uses REST routing only.
// These classes live in spring-cloud-gateway-server and activate whenever any io.grpc class is
// on the classpath. Excluding them here is a second defensive layer; the primary fix is the
// io.grpc:* Maven exclusions declared in pom.xml.
@SpringBootApplication(excludeName = {
    "org.springframework.cloud.gateway.config.GrpcSslBundleAutoConfiguration"
})
@EnableDiscoveryClient
public class SkyWaysGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyWaysGatewayApplication.class, args);
    }
}
