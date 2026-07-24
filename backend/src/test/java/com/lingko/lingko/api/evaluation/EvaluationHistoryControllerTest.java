package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryItemResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 활성 세션 인증을 통한 평가 기록 소유권을 검증한다.
 */
@WebMvcTest(EvaluationHistoryController.class)
class EvaluationHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationHistoryService historyService;

    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("GET /api/evaluations/me는 JWT principal 기준 학습 기록 page를 반환한다")
    void getMyEvaluationHistory() throws Exception {
        PracticeHistoryResponse response = PracticeHistoryResponse.builder()
                .items(List.of(PracticeHistoryItemResponse.builder()
                        .evaluationLogId(10L)
                        .sentenceId(1L)
                        .source("RECOMMENDED")
                        .originalText("맛있겠다.")
                        .standardPronunciation("마싯게따.")
                        .recognizedText("마싣게따")
                        .overallScore(82)
                        .gradeLabel("Good")
                        .summary("Good pronunciation.")
                        .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                                .accuracy(84)
                                .fluency(80)
                                .completeness(91)
                                .build())
                        .createdAt(LocalDateTime.of(2026, 6, 26, 15, 0))
                        .build()))
                .page(0)
                .size(10)
                .totalItems(1)
                .totalPages(1)
                .hasNext(false)
                .bestScore(82)
                .build();
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(historyService.findHistory(7L, 0, 10)).thenReturn(response);

        mockMvc.perform(get("/api/evaluations/me")
                        .header("Authorization", "Bearer valid-access-token")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].evaluationLogId").value(10L))
                .andExpect(jsonPath("$.items[0].source").value("RECOMMENDED"))
                .andExpect(jsonPath("$.items[0].overallScore").value(82))
                .andExpect(jsonPath("$.items[0].scoreBreakdown.accuracy").value(84))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.bestScore").value(82));
    }

    @Test
    @DisplayName("학습 기록은 Authorization bearer token이 필요하다")
    void authorizationHeaderIsRequired() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer(null))
                .thenThrow(new AuthException("Missing bearer token"));

        mockMvc.perform(get("/api/evaluations/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("유효하지 않은 bearer token은 401을 반환한다")
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer invalid-token"))
                .thenThrow(new AuthException("Invalid access token"));

        mockMvc.perform(get("/api/evaluations/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("학습 기록 page size는 50 이하로 제한한다")
    void sizeMustBeAtMost50() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);

        mockMvc.perform(get("/api/evaluations/me")
                        .header("Authorization", "Bearer valid-access-token")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
