package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter @Setter
public class JwtSettings {
    private String secretKey;
    private Integer accessTokenExpireMinutes;
    private Integer refreshTokenExpireDays;
    private String algorithm;
}