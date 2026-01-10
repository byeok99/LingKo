package com.lingko.lingko.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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

        System.out.println(">>> Azure Key: " + key);
        System.out.println(">>> Azure Region: " + region);

        assertThat(key).isNotEmpty();
        assertThat(region).isNotEmpty();
    }

    @Test
    @DisplayName("JWT 설정값이 YAML에서 제대로 로드되어야 한다.")
    void jwtSettingsLoadTest() {
        // Given & When
        int expireMinutes = jwtSettings.getAccessTokenExpireMinutes();
        String algorithm = jwtSettings.getAlgorithm();

        // Then
        System.out.println(">>> JWT Expire: " + expireMinutes);
        System.out.println(">>> JWT Algorithm: " + algorithm);

        assertThat(expireMinutes).isGreaterThan(0);
        assertThat(algorithm).isEqualTo("HS256"); // YAML에 쓴 값과 비교
    }
}
