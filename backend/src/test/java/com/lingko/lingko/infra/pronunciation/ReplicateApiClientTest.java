package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Replicate Api Client Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class ReplicateApiClientTest {

    @Test
    @DisplayName("Replicate API key가 없으면 명확한 예외를 던진다")
    void validateSettingsRejectsMissingApiKey() {
        ReplicateSettings settings = new ReplicateSettings();
        settings.setVersion("model-version");
        ReplicateApiClient client = new ReplicateApiClient(WebClient.create(), new ObjectMapper(), settings);

        assertThatThrownBy(client::validateSettings)
                .isInstanceOf(VideoGenerationException.class)
                .hasMessage("Replicate API key is not configured");
    }

    @Test
    @DisplayName("Replicate model version이 없으면 명확한 예외를 던진다")
    void validateSettingsRejectsMissingVersion() {
        ReplicateSettings settings = new ReplicateSettings();
        settings.setApiKey("short");
        ReplicateApiClient client = new ReplicateApiClient(WebClient.create(), new ObjectMapper(), settings);

        assertThatThrownBy(client::validateSettings)
                .isInstanceOf(VideoGenerationException.class)
                .hasMessage("Replicate model version is not configured");
    }
}
