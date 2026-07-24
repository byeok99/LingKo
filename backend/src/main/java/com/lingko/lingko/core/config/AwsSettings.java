package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 Aws 설정을 type-safe application 설정으로 binding한다.
 *
 * 환경값 조회를 코드 곳곳에 분산하지 않고 시작 시 검증과 비밀정보 처리를 중앙화하기 위해 형식이 지정된 binding을 선택했다.
 */
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
