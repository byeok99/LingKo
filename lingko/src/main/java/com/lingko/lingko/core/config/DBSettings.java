package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "db")
@Getter @Setter
public class DBSettings {
    private String host;
    private Integer port;
    private String user;
    private String username;
    private String password;
    private String driver;
}
