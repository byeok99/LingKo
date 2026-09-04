package com.lingko.lingko.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Settings Load Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@SpringBootTest
@Tag("external")
public class SettingsLoadTest {

    @Autowired
    private AzureSettings azureSettings;

    @Autowired
    private JwtSettings jwtSettings;

    @Test
    @DisplayName("Azure 설정값이 YAML에서 제대로 로드되어야 한다.")
    void azureSettingsLoadTest() {
        String key = azureSettings.getSecretKey();
        String region = azureSettings.getRegion();

        assertThat(key).isNotEmpty();
        assertThat(region).isNotEmpty();
    }

    @Test
    @DisplayName("JWT 설정값이 YAML에서 제대로 로드되어야 한다.")
    void jwtSettingsLoadTest() {
        // 준비 및 실행
        int expireMinutes = jwtSettings.getAccessTokenExpireMinutes();
        String algorithm = jwtSettings.getAlgorithm();

        // Then
        assertThat(expireMinutes).isGreaterThan(0);
        assertThat(algorithm).isEqualTo("HS256"); // YAML에 쓴 값과 비교
    }
}
