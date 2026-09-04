package com.lingko.lingko.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 외부 Google O Auth 설정을 type-safe application 설정으로 binding한다.
 *
 * 환경값 조회를 코드 곳곳에 분산하지 않고 시작 시 검증과 비밀정보 처리를 중앙화하기 위해 형식이 지정된 binding을 선택했다.
 */
@Configuration
@ConfigurationProperties(prefix = "google")
@Getter
@Setter
public class GoogleOAuthSettings {
    private String clientId;
}
