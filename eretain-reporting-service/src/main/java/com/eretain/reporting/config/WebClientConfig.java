package com.eretain.reporting.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Build a WebClient for inter-service calls with default auth headers.
     * Uses clone() to avoid mutating the shared load-balanced builder.
     */
    private WebClient buildServiceClient(WebClient.Builder builder, String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("X-User-Id", "1")
                .defaultHeader("X-User-Name", "reporting-service")
                .defaultHeader("X-User-Roles", "ADMINISTRATOR")
                .build();
    }

    @Bean
    public WebClient projectServiceClient(WebClient.Builder builder) {
        return buildServiceClient(builder, "http://eretain-project-service");
    }

    @Bean
    public WebClient allocationServiceClient(WebClient.Builder builder) {
        return buildServiceClient(builder, "http://eretain-allocation-service");
    }

    @Bean
    public WebClient timesheetServiceClient(WebClient.Builder builder) {
        return buildServiceClient(builder, "http://eretain-timesheet-service");
    }

    @Bean
    public WebClient authServiceClient(WebClient.Builder builder) {
        return buildServiceClient(builder, "http://eretain-auth-service");
    }

    @Bean
    public WebClient companyServiceClient(WebClient.Builder builder) {
        return buildServiceClient(builder, "http://eretain-company-service");
    }
}
