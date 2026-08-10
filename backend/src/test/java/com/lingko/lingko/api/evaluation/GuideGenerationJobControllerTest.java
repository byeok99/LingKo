package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobResponse;
import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobAccessDeniedException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobCapacityExceededException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobRateLimitExceededException;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guide Generation Job 컨트롤러 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(
        controllers = GuideGenerationJobController.class,
        properties = "guide-generation.jobs.api-enabled=true"
)
class GuideGenerationJobControllerTest {

    private static final String INTERNAL_TOKEN = "0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuideGenerationJobService guideGenerationJobService;

    @MockitoBean
    private GuideGenerationJobAccessGuard accessGuard;

    @Test
    @DisplayName("POST /api/pronunciation/guide-jobs는 비동기 가이드 생성 job을 생성한다")
    void createGuideGenerationJob() throws Exception {
        GuideGenerationJobResponse response = GuideGenerationJobResponse.builder()
                .jobId("job-1")
                .status(GuideGenerationJobStatus.PENDING)
                .cacheKey("cache-key")
                .build();

        when(guideGenerationJobService.submit(
                eq("마"),
                eq(VideoType.MOUTH),
                eq(List.of(List.of("https://example.com/a.png", "https://example.com/b.png")))
        )).thenReturn(response);

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "syllable": "마",
                                  "type": "MOUTH",
                                  "urlPairs": [["https://example.com/a.png", "https://example.com/b.png"]]
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.cacheKey").value("cache-key"));
    }

    @Test
    @DisplayName("GET /api/pronunciation/guide-jobs/{jobId}는 job 상태를 반환한다")
    void getGuideGenerationJob() throws Exception {
        GuideGenerationJobResponse response = GuideGenerationJobResponse.builder()
                .jobId("job-1")
                .status(GuideGenerationJobStatus.COMPLETED)
                .cacheKey("cache-key")
                .resultUrl("https://cdn.example.com/guides/ma.mp4")
                .build();
        when(guideGenerationJobService.find("job-1")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/pronunciation/guide-jobs/job-1")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resultUrl").value("https://cdn.example.com/guides/ma.mp4"));
    }

    @Test
    @DisplayName("존재하지 않는 guide job은 404를 반환한다")
    void getMissingGuideGenerationJob() throws Exception {
        when(guideGenerationJobService.find("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pronunciation/guide-jobs/missing")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUIDE_JOB_NOT_FOUND"));
    }

    @Test
    @DisplayName("guide job 요청은 빈 syllable을 거부한다")
    void createGuideGenerationJobValidatesSyllable() throws Exception {
        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "syllable": " ",
                                  "type": "MOUTH",
                                  "urlPairs": [["https://example.com/a.png", "https://example.com/b.png"]]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("내부 인증이 없으면 생성하지 않고 401을 반환한다")
    void createRequiresInternalAuthentication() throws Exception {
        doThrow(new AuthException("missing internal token"))
                .when(accessGuard).authorizeAndConsume(null, null);

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        verifyNoInteractions(guideGenerationJobService);
    }

    @Test
    @DisplayName("활성 일반 사용자는 내부 생성 권한이 없어 403을 반환한다")
    void createRejectsLearnerAuthorization() throws Exception {
        doThrow(new GuideJobAccessDeniedException())
                .when(accessGuard).authorizeAndConsume(null, "Bearer learner-token");

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("Authorization", "Bearer learner-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GUIDE_JOB_FORBIDDEN"));

        verifyNoInteractions(guideGenerationJobService);
    }

    @Test
    @DisplayName("내부 호출자 분당 제한을 넘으면 Retry-After와 429를 반환한다")
    void createRateLimitsInternalCaller() throws Exception {
        doThrow(new GuideJobRateLimitExceededException(17))
                .when(accessGuard).authorizeAndConsume(INTERNAL_TOKEN, null);

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "17"))
                .andExpect(jsonPath("$.code").value("GUIDE_JOB_RATE_LIMITED"));

        verifyNoInteractions(guideGenerationJobService);
    }

    @Test
    @DisplayName("실행 슬롯이 가득 차면 Retry-After와 429를 반환한다")
    void createRejectsWhenGenerationCapacityIsFull() throws Exception {
        when(accessGuard.authorizeAndConsume(INTERNAL_TOKEN, null))
                .thenReturn("guide-internal-service");
        when(guideGenerationJobService.submit(eq("마"), eq(VideoType.MOUTH), eq(List.of(
                List.of("https://replicate.delivery/a.png", "https://replicate.delivery/b.png")
        )))).thenThrow(new GuideJobCapacityExceededException());

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.code").value("GUIDE_JOB_CAPACITY_EXCEEDED"));
    }

    @Test
    @DisplayName("URL 하나가 최대 길이를 넘으면 job을 등록하지 않는다")
    void createRejectsOversizedUrl() throws Exception {
        when(accessGuard.authorizeAndConsume(INTERNAL_TOKEN, null))
                .thenReturn("guide-internal-service");
        String oversizedUrl = "https://replicate.delivery/" + "a".repeat(2_100);

        mockMvc.perform(post("/api/pronunciation/guide-jobs")
                        .header("X-LingKo-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "syllable": "마",
                                  "type": "MOUTH",
                                  "urlPairs": [["%s"]]
                                }
                                """.formatted(oversizedUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(guideGenerationJobService);
    }

    private String validRequest() {
        return """
                {
                  "syllable": "마",
                  "type": "MOUTH",
                  "urlPairs": [["https://replicate.delivery/a.png", "https://replicate.delivery/b.png"]]
                }
                """;
    }
}
