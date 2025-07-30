package com.cognizant.vibe.synthetictesting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration class for application-wide scheduler services.
 */
@Configuration
public class SchedulerConfig {

    /**
     * Creates a thread pool for running scheduled tasks like health checks.
     * The size can be configured based on expected load.
     * @return A centrally managed ScheduledExecutorService.
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService checkSchedulerExecutor() {
        // A pool of 10 threads should be sufficient for a moderate number of checks.
        // This can be externalized to application.properties if needed.
        return Executors.newScheduledThreadPool(10);
    }
}