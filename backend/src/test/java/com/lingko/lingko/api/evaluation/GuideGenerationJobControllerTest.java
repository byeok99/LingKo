package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.GuideGenerationJobResponse;
import com.lingko.lingko.core.domain.evaluation.dto.GuideGenerationJobStatus;
import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuideGenerationJobController.class)
class GuideGenerationJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GuideGenerationJobService guideGenerationJobService;

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

        mockMvc.perform(get("/api/pronunciation/guide-jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resultUrl").value("https://cdn.example.com/guides/ma.mp4"));
    }

    @Test
    @DisplayName("존재하지 않는 guide job은 404를 반환한다")
    void getMissingGuideGenerationJob() throws Exception {
        when(guideGenerationJobService.find("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pronunciation/guide-jobs/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUIDE_JOB_NOT_FOUND"));
    }

    @Test
    @DisplayName("guide job 요청은 빈 syllable을 거부한다")
    void createGuideGenerationJobValidatesSyllable() throws Exception {
        mockMvc.perform(post("/api/pronunciation/guide-jobs")
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
}
