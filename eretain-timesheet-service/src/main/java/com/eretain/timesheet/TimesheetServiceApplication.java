package com.eretain.timesheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ComponentScan(basePackages = {"com.eretain.timesheet", "com.eretain.common"})
@EnableJpaAuditing
public class TimesheetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimesheetServiceApplication.class, args);
    }
}
