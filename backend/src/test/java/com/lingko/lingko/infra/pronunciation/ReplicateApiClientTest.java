package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Replicate Api Client Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class ReplicateApiClientTest {

    @Test
    @DisplayName("Prediction 생성이 429이면 제한된 횟수 안에서 재시도한다")
    void retriesPredictionCreationAfterRateLimit() {
        AtomicInteger createAttempts = new AtomicInteger();
        WebClient webClient = webClient(request -> {
            if (request.method() == HttpMethod.POST && request.url().getPath().equals("/v1/predictions")) {
                if (createAttempts.incrementAndGet() == 1) {
                    return response(HttpStatus.TOO_MANY_REQUESTS, "{\"detail\":\"throttled\"}");
                }
                return response(HttpStatus.CREATED, "{\"id\":\"prediction-1\"}");
            }
            return response(HttpStatus.OK, "{\"status\":\"succeeded\",\"output\":\"https://delivery/video.mp4\"}");
        });

        String result = new ReplicateApiClient(webClient, new ObjectMapper(), retrySettings())
                .interpolate("https://guides/frame-1.png", "https://guides/frame-2.png");

        assertThat(result).isEqualTo("https://delivery/video.mp4");
        assertThat(createAttempts).hasValue(2);
    }

    @Test
    @DisplayName("Prediction polling timeout이면 원격 작업을 취소한다")
    void cancelsPredictionAfterPollingTimeout() {
        List<String> requests = new CopyOnWriteArrayList<>();
        List<String> cancelAfterHeaders = new CopyOnWriteArrayList<>();
        WebClient webClient = webClient(request -> {
            requests.add(request.method() + " " + request.url().getPath());
            if (request.url().getPath().equals("/v1/predictions")) {
                cancelAfterHeaders.add(request.headers().getFirst("Cancel-After"));
                return response(HttpStatus.CREATED, "{\"id\":\"prediction-2\"}");
            }
            if (request.url().getPath().endsWith("/cancel")) {
                return response(HttpStatus.OK, "{\"status\":\"canceled\"}");
            }
            return response(HttpStatus.OK, "{\"status\":\"starting\"}");
        });
        ReplicateSettings settings = retrySettings();
        settings.setMaxPollAttempts(1);

        assertThatThrownBy(() -> new ReplicateApiClient(webClient, new ObjectMapper(), settings)
                .interpolate("https://guides/frame-1.png", "https://guides/frame-2.png"))
                .isInstanceOf(VideoGenerationException.class)
                .hasRootCauseMessage("타임아웃: Prediction이 0초 내에 완료되지 않음");
        assertThat(requests).containsExactly(
                "POST /v1/predictions",
                "GET /v1/predictions/prediction-2",
                "POST /v1/predictions/prediction-2/cancel"
        );
        assertThat(cancelAfterHeaders).containsExactly("5s");
    }

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

    private ReplicateSettings retrySettings() {
        ReplicateSettings settings = new ReplicateSettings();
        settings.setApiKey("test-token");
        settings.setVersion("model-version");
        settings.setMaxPollAttempts(2);
        settings.setPollIntervalMs(1);
        settings.setCreateMaxAttempts(3);
        settings.setRetryInitialDelayMs(1);
        settings.setRetryMaxDelayMs(2);
        return settings;
    }

    private WebClient webClient(org.springframework.web.reactive.function.client.ExchangeFunction exchangeFunction) {
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private Mono<ClientResponse> response(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", "application/json")
                .body(body)
                .build());
    }
}
