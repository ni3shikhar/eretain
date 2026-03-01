package com.eretain.allocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.eretain.allocation", "com.eretain.common"})
public class AllocationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllocationServiceApplication.class, args);
    }
}
