package com.lingko.lingko.api.quota;

import com.lingko.lingko.api.quota.dto.PracticeQuotaResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 활성 세션 인증 경계를 통한 할당량 접근을 검증한다.
 */
@WebMvcTest(PracticeQuotaController.class)
class PracticeQuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PracticeQuotaService quotaService;

    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("GET /api/quota/today는 JWT 사용자 기준 오늘 quota를 반환한다")
    void getTodayQuota() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(quotaService.getTodayQuota(7L)).thenReturn(new PracticeQuotaResponse(
                LocalDate.of(2026, 6, 29),
                5,
                2,
                1,
                4,
                OffsetDateTime.of(2026, 6, 29, 1, 30, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 29, 0, 48, 12, 0, ZoneOffset.ofHours(9))
        ));

        mockMvc.perform(get("/api/quota/today")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-06-29"))
                .andExpect(jsonPath("$.freeLimit").value(5))
                .andExpect(jsonPath("$.freeUsed").value(2))
                .andExpect(jsonPath("$.rewardedAvailable").value(1))
                .andExpect(jsonPath("$.remainingPractices").value(4))
                .andExpect(jsonPath("$.nextRefillAt").value("2026-06-29T01:30:00+09:00"))
                .andExpect(jsonPath("$.serverTime").value("2026-06-29T00:48:12+09:00"));
    }

    @Test
    @DisplayName("quota 조회는 Authorization bearer token이 필요하다")
    void authorizationHeaderIsRequired() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer(null))
                .thenThrow(new AuthException("Missing bearer token"));

        mockMvc.perform(get("/api/quota/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("유효하지 않은 bearer token은 401을 반환한다")
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer invalid-token"))
                .thenThrow(new AuthException("Invalid access token"));

        mockMvc.perform(get("/api/quota/today")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/quota/ad-rewards는 인증 사용자와 event ID로 1회 보상을 지급한다")
    void claimAdReward() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(quotaService.grantAdReward(7L, "reward-event-123")).thenReturn(new PracticeQuotaResponse(
                LocalDate.of(2026, 6, 29),
                5,
                3,
                1,
                3,
                OffsetDateTime.of(2026, 6, 29, 1, 30, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 29, 0, 48, 12, 0, ZoneOffset.ofHours(9))
        ));

        mockMvc.perform(post("/api/quota/ad-rewards")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("{\"rewardEventId\":\"reward-event-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardedAvailable").value(1))
                .andExpect(jsonPath("$.remainingPractices").value(3));
    }

    @Test
    @DisplayName("광고 보상 event ID는 비어 있거나 너무 길 수 없다")
    void validatesRewardEventId() throws Exception {
        mockMvc.perform(post("/api/quota/ad-rewards")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("{\"rewardEventId\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/quota/ad-rewards")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType("application/json")
                        .content("{\"rewardEventId\":\"" + "x".repeat(81) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
