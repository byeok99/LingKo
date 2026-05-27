package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google")
@Getter @Setter
public class GoogleSettings {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
