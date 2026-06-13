package com.airtribe.draftly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.airtribe.draftly.config.AppProperties;

/**
 * Draftly - Gmail AI Reply Agent.
 *
 * Entry point for the Spring Boot backend. Scheduling is enabled so that
 * the retry scheduler can re-attempt failed email sends in the background.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class DraftlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(DraftlyApplication.class, args);
    }
}
