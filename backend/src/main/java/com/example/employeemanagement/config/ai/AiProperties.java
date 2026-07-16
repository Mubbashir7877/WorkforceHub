package com.example.employeemanagement.config.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the AI_* environment variables (see application.properties, prefix "ai").
 * Never logged or exposed via API responses in full — see AiOperationalStatusService
 * for the safe, redacted view of this configuration.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled;
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
    private double temperature;
    private int maxTokens;
    private int topK;
    private double similarityThreshold;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
