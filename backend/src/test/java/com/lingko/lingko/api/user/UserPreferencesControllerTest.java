package com.lingko.lingko.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.user.dto.UserPreferencesResponse;
import com.lingko.lingko.api.user.dto.UserPreferencesUpdateRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.service.UserPreferencesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferencesController.class)
class UserPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserPreferencesService preferencesService;

    @MockBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    @Test
    @DisplayName("GET /api/users/me/preferences는 access token 사용자 설정을 반환한다")
    void getMyPreferences() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(preferencesService.findPreferences(7L)).thenReturn(new UserPreferencesResponse(
                "ko",
                "en",
                User.LearningLevel.INTERMEDIATE_1
        ));

        mockMvc.perform(get("/api/users/me/preferences")
                        .header("Authorization", "Bearer valid-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayLanguage").value("ko"))
                .andExpect(jsonPath("$.nativeLanguage").value("en"))
                .andExpect(jsonPath("$.targetLevel").value("INTERMEDIATE_1"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/preferences는 사용자 설정을 갱신한다")
    void updateMyPreferences() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);
        when(preferencesService.updatePreferences(eq(7L), eq(new UserPreferencesUpdateRequest(
                "ko",
                "ja",
                User.LearningLevel.BEGINNER_2
        )))).thenReturn(new UserPreferencesResponse(
                "ko",
                "ja",
                User.LearningLevel.BEGINNER_2
        ));

        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayLanguage", "ko",
                                "nativeLanguage", "ja",
                                "targetLevel", "BEGINNER_2"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayLanguage").value("ko"))
                .andExpect(jsonPath("$.nativeLanguage").value("ja"))
                .andExpect(jsonPath("$.targetLevel").value("BEGINNER_2"));
    }

    @Test
    @DisplayName("사용자 설정 조회는 Authorization bearer token이 필요하다")
    void authorizationHeaderIsRequired() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer(null))
                .thenThrow(new AuthException("Missing bearer token"));

        mockMvc.perform(get("/api/users/me/preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("사용자 설정 갱신은 입력값을 검증한다")
    void updatePreferencesValidatesRequest() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer valid-access-token")).thenReturn(7L);

        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer valid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayLanguage", "",
                                "nativeLanguage", "en",
                                "targetLevel", "BEGINNER_2"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("유효하지 않은 bearer token은 401을 반환한다")
    void invalidBearerTokenReturnsUnauthorized() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer invalid-token"))
                .thenThrow(new AuthException("Invalid access token"));

        mockMvc.perform(get("/api/users/me/preferences")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }
}
