package com.eretain.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${eretain.api.gateway-url}")
    private String gatewayUrl;

    @Bean
    public WebClient apiClient() {
        return WebClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }
}
