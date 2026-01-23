package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="replicate")
@Getter @Setter
public class ReplicateSettings {
    private String apiKey;
    private String version;
    private int maxPollAttempts;
    private int pollIntervalMs;
}
