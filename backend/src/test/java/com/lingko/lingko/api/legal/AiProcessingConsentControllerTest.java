package com.lingko.lingko.api.legal;

import com.lingko.lingko.api.legal.dto.AiProcessingConsentResponse;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** AI 동의는 인증 계정에 귀속되고 누락된 명시적 선택은 저장되지 않는지 검증한다. */
@WebMvcTest(AiProcessingConsentController.class)
class AiProcessingConsentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ActiveSessionAuthenticator authenticator;
    @MockitoBean AiProcessingConsentService service;

    @Test
    void requiresAuthenticationForReadAndWrite() throws Exception {
        when(authenticator.authenticateBearer(null)).thenThrow(new AuthException("Missing bearer token"));
        mvc.perform(get("/api/legal/ai-consent")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/legal/ai-consent").contentType(MediaType.APPLICATION_JSON)
                .content("{\"granted\":true,\"noticeVersion\":\"2026-09-12\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void missingChoiceIsNotImplicitPermission() throws Exception {
        mvc.perform(post("/api/legal/ai-consent").contentType(MediaType.APPLICATION_JSON)
                .content("{\"noticeVersion\":\"2026-09-12\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void usesAuthenticatedOwnerForReadAndWithdrawal() throws Exception {
        when(authenticator.authenticateBearer("Bearer test")).thenReturn(7L);
        when(service.getStatus(7L)).thenReturn(new AiProcessingConsentResponse(false, "2026-09-12", null));
        mvc.perform(get("/api/legal/ai-consent").header("Authorization", "Bearer test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.granted").value(false));
        when(service.record(7L, false, "2026-09-12"))
                .thenReturn(new AiProcessingConsentResponse(false, "2026-09-12", null));
        mvc.perform(post("/api/legal/ai-consent").header("Authorization", "Bearer test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"granted\":false,\"noticeVersion\":\"2026-09-12\",\"userId\":99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.granted").value(false));
        verify(service).record(7L, false, "2026-09-12");
    }
}
