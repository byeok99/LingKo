package com.lingko.lingko.api.legal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.legal.dto.LegalConsentStatusResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.legal.service.LegalConsentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 약관 동의 상태 endpoint의 인증·validation·사용자 귀속 계약을 검증한다.
 */
@WebMvcTest(LegalConsentController.class)
class LegalConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LegalConsentService legalConsentService;

    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("상태 조회는 Bearer token 사용자의 현재 동의 필요 여부를 반환한다")
    void getStatusUsesAuthenticatedUser() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access.jwt")).thenReturn(7L);
        when(legalConsentService.getStatus(7L))
                .thenReturn(new LegalConsentStatusResponse(true, "2026-08-07"));

        mockMvc.perform(get("/api/legal/consent")
                        .header("Authorization", "Bearer access.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.documentVersion").value("2026-08-07"));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 동의 상태를 조회할 수 없다")
    void getStatusRequiresAuthentication() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer(null))
                .thenThrow(new AuthException("Missing bearer token"));

        mockMvc.perform(get("/api/legal/consent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("필수 항목이 false인 제출은 저장 전에 거부한다")
    void submitRejectsMissingRequiredConsent() throws Exception {
        mockMvc.perform(post("/api/legal/consent")
                        .header("Authorization", "Bearer access.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "termsAgreed", false,
                                "privacyAcknowledged", true,
                                "marketingOptIn", false,
                                "documentVersion", "2026-08-07",
                                "agreedAt", "2026-08-07T01:02:03Z"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("제출 사용자는 body가 아니라 Bearer token에서 결정한다")
    void submitUsesAuthenticatedUser() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access.jwt")).thenReturn(7L);
        when(legalConsentService.record(eq(7L), any()))
                .thenReturn(new LegalConsentStatusResponse(false, "2026-08-07"));

        mockMvc.perform(post("/api/legal/consent")
                        .header("Authorization", "Bearer access.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "termsAgreed", true,
                                "privacyAcknowledged", true,
                                "marketingOptIn", false,
                                "documentVersion", "2026-08-07",
                                "agreedAt", "2026-08-07T01:02:03Z"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").value(false));

        verify(legalConsentService).record(eq(7L), any());
    }
}
