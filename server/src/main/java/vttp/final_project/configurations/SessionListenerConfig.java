package vttp.final_project.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vttp.final_project.services.ApiMetricsService;

@Configuration
public class SessionListenerConfig {

    /**
     * Register the ApiMetricsService as an HttpSessionListener
     */
    @Bean
    public ApiMetricsService httpSessionListener() {
        return new ApiMetricsService(io.micrometer.core.instrument.Metrics.globalRegistry);
    }
}