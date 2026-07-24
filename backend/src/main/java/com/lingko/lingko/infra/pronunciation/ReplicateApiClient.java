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

import java.util.Map;

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
            log.debug("Frame Interpolation 시작: {} -> {}", frame1Url, frame2Url);

            // 1. Prediction 생성
            String predictionId = createPrediction(frame1Url, frame2Url);
            log.debug("Prediction 생성됨: {}", predictionId);

            // 2. 완료까지 폴링
            String videoUrl = pollUntilComplete(predictionId);
            log.debug("Frame Interpolation 완료: {}", videoUrl);

            return videoUrl;

        } catch (Exception e) {
            log.error("Frame Interpolation 실패: {} -> {}", frame1Url, frame2Url, e);
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

        String response = webClient.post()
                .uri(BASE_URL + "/predictions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

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
            String response = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiKey())  // ✅ Settings 사용
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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
            if ("failed".equals(status) || "canceled".equals(status)) {
                String error = json.has("error") ? json.get("error").asText() : "Unknown error";
                throw new VideoGenerationException("Prediction 실패: " + error);
            }

            // 진행 중 - 대기
            Thread.sleep(pollInterval);
        }

        throw new VideoGenerationException(
                String.format("타임아웃: Prediction이 %d초 내에 완료되지 않음",
                        maxAttempts * pollInterval / 1000)
        );
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
    }
}
