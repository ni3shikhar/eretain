package com.eretain.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.eretain.project", "com.eretain.common"})
public class ProjectServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
