package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter @Setter
public class AwsSettings {
    private S3 s3;
    private Credentials credentials;

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region;
    }

    @Getter
    @Setter
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }

}
