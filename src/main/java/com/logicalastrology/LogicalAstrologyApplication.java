package com.logicalastrology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogicalAstrologyApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogicalAstrologyApplication.class, args);
    }
}
