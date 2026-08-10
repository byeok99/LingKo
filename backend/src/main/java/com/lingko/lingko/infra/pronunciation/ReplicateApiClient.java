package com.lingko.lingko.infra.pronunciation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.ReplicateSettings;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Replicate prediction API를 호출하고 비동기 job 생명주기를 정규화한다.
 *
 * polling과 응답 검증을 이 경계에서 처리해 도메인 서비스가 공급자 전송 형식에 의존하지 않게 한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReplicateApiClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ReplicateSettings settings;

    private static final String BASE_URL = "https://api.replicate.com/v1";

    /**
     * Frame Interpolation
     *
     * @param frame1Url
     * @param frame2Url
     * @return 생성된 영상 URL
     */
    public String interpolate(String frame1Url, String frame2Url) {
        try {
            // 입력 URL은 presigned credential을 포함할 수 있어 로그에 원문을 남기지 않는다.
            log.debug("Frame Interpolation 시작");

            // 1. Prediction 생성
            String predictionId = createPrediction(frame1Url, frame2Url);
            log.debug("Prediction 생성됨: {}", predictionId);

            // 2. 완료까지 폴링
            String videoUrl = pollUntilComplete(predictionId);
            log.debug("Frame Interpolation 완료");

            return videoUrl;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Frame Interpolation interrupted", exception);
            throw new VideoGenerationException("Frame Interpolation interrupted", exception);
        } catch (Exception e) {
            log.error("Frame Interpolation 실패", e);
            throw new VideoGenerationException("Frame Interpolation 실패", e);
        }
    }

    /**
     * Prediction 생성
     */
    private String createPrediction(String frame1Url, String frame2Url) throws Exception {
        validateSettings();

        Map<String, Object> input = Map.of(
                "frame1", frame1Url,
                "frame2", frame2Url,
                "times_to_interpolate", 3
        );

        Map<String, Object> requestBody = Map.of(
                "version", settings.getVersion(),
                "input", input
        );

        String response = executeWithRetry("create prediction", () -> webClient.post()
                .uri(BASE_URL + "/predictions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey())
                // 공급자 작업도 로컬 polling 기한과 함께 종료해 timeout 이후 비용과 동시 실행 슬롯 누수를 막는다.
                .header("Cancel-After", cancelAfterHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block());

        JsonNode json = objectMapper.readTree(response);

        if (!json.has("id")) {
            throw new VideoGenerationException("Prediction 생성 실패: " + response);
        }

        return json.get("id").asText();
    }

    /**
     * Prediction 완료까지 폴링
     */
    private String pollUntilComplete(String predictionId) throws Exception {
        String url = BASE_URL + "/predictions/" + predictionId;
        int maxAttempts = settings.getMaxPollAttempts();
        int pollInterval = settings.getPollIntervalMs();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String response = executeWithRetry("poll prediction", () -> webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block());

            JsonNode json = objectMapper.readTree(response);
            String status = json.get("status").asText();

            // 30초마다 로그
            if (attempt > 0 && attempt % 6 == 0) {
                log.info("Prediction 대기 중: {} ({}초)", status, attempt * pollInterval / 1000);
            }

            // 성공
            if ("succeeded".equals(status)) {
                return extractVideoUrl(json.get("output"));
            }

            // 실패
            if ("failed".equals(status) || "canceled".equals(status) || "aborted".equals(status)) {
                String error = json.has("error") ? json.get("error").asText() : "Unknown error";
                throw new VideoGenerationException("Prediction 실패: " + error);
            }

            // 진행 중 - 대기
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException exception) {
                cancelPrediction(predictionId);
                throw exception;
            }
        }

        cancelPrediction(predictionId);
        throw new VideoGenerationException(
                String.format("타임아웃: Prediction이 %d초 내에 완료되지 않음",
                        maxAttempts * pollInterval / 1000)
        );
    }

    private String executeWithRetry(String operation, Supplier<String> request) throws InterruptedException {
        int maxAttempts = settings.getCreateMaxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return request.get();
            } catch (WebClientResponseException exception) {
                if (!isRetryable(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                long delayMs = retryDelayMs(attempt);
                log.warn(
                        "Replicate {} throttled or unavailable; retrying: status={}, attempt={}/{}, delayMs={}",
                        operation,
                        exception.getStatusCode().value(),
                        attempt,
                        maxAttempts,
                        delayMs
                );
                Thread.sleep(delayMs);
            }
        }
        throw new IllegalStateException("Replicate retry loop exhausted unexpectedly");
    }

    private boolean isRetryable(WebClientResponseException exception) {
        return exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError();
    }

    private long retryDelayMs(int completedAttempt) {
        long multiplier = 1L << Math.min(completedAttempt - 1, 30);
        long delay = Math.multiplyExact((long) settings.getRetryInitialDelayMs(), multiplier);
        return Math.min(delay, settings.getRetryMaxDelayMs());
    }

    private String cancelAfterHeader() {
        long timeoutMs = Math.multiplyExact(
                (long) settings.getMaxPollAttempts(),
                settings.getPollIntervalMs()
        );
        long timeoutSeconds = Math.max(5, (timeoutMs + 999) / 1_000);
        return timeoutSeconds + "s";
    }

    private void cancelPrediction(String predictionId) {
        try {
            webClient.post()
                    .uri(BASE_URL + "/predictions/" + predictionId + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey())
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Timed out Replicate prediction canceled: {}", predictionId);
        } catch (RuntimeException exception) {
            // 원래 timeout을 보존하되 공급자 작업이 남을 수 있음을 운영 로그로 드러낸다.
            log.warn("Failed to cancel timed out Replicate prediction: {}", predictionId, exception);
        }
    }

    /**
     * Output에서 영상 URL 추출
     */
    private String extractVideoUrl(JsonNode output) {
        if (output.isTextual()) {
            return output.asText();
        } else if (output.isArray() && !output.isEmpty()) {
            return output.get(0).asText();
        }
        throw new VideoGenerationException("예상치 못한 output 형식: " + output);
    }

    void validateSettings() {
        if (settings.getApiKey() == null || settings.getApiKey().isBlank()) {
            throw new VideoGenerationException("Replicate API key is not configured");
        }
        if (settings.getVersion() == null || settings.getVersion().isBlank()) {
            throw new VideoGenerationException("Replicate model version is not configured");
        }
        if (settings.getMaxPollAttempts() < 1 || settings.getPollIntervalMs() < 1) {
            throw new VideoGenerationException("Replicate polling settings must be positive");
        }
        if (settings.getCreateMaxAttempts() < 1
                || settings.getRetryInitialDelayMs() < 1
                || settings.getRetryMaxDelayMs() < settings.getRetryInitialDelayMs()) {
            throw new VideoGenerationException("Replicate retry settings are invalid");
        }
    }
}
