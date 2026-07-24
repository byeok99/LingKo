package com.lingko.lingko.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.AuthUserResponse;
import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인, 갱신 토큰 회전, 현재 기기 로그아웃의 HTTP 계약을 검증한다.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Google OAuth 로그인은 access/refresh token과 사용자 정보를 반환한다")
    void googleOauthLoginReturnsTokens() throws Exception {
        when(authService.loginWithOAuth(any())).thenReturn(AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken("access.jwt")
                .refreshToken("refresh.jwt")
                .expiresInSeconds(1800L)
                .user(AuthUserResponse.builder()
                        .userId(7L)
                        .email("user@example.com")
                        .name("LingKo User")
                        .profileImageUrl("https://example.com/profile.png")
                        .build())
                .build());

        mockMvc.perform(post("/api/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "provider", "GOOGLE",
                                "idToken", "valid-google-id-token"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.jwt"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800L))
                .andExpect(jsonPath("$.user.userId").value(7L))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    @DisplayName("idToken은 필수다")
    void idTokenIsRequired() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "provider", "GOOGLE",
                                "idToken", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("검증 실패한 OAuth token은 401을 반환한다")
    void invalidOauthTokenReturnsUnauthorized() throws Exception {
        when(authService.loginWithOAuth(any())).thenThrow(new AuthException("Invalid OAuth token"));

        mockMvc.perform(post("/api/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "provider", "GOOGLE",
                                "idToken", "invalid-token"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("Refresh Token 갱신은 회전된 token pair를 반환한다")
    void refreshReturnsRotatedTokens() throws Exception {
        when(authService.refresh(any())).thenReturn(AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken("next-access.jwt")
                .refreshToken("next-refresh.jwt")
                .expiresInSeconds(1800L)
                .user(AuthUserResponse.builder()
                        .userId(7L)
                        .email("user@example.com")
                        .name("LingKo User")
                        .build())
                .build());

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "current-refresh.jwt"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("next-access.jwt"))
                .andExpect(jsonPath("$.refreshToken").value("next-refresh.jwt"));
    }

    @Test
    @DisplayName("로그아웃은 현재 Refresh Token 세션을 폐기하고 204를 반환한다")
    void logoutRevokesSession() throws Exception {
        doNothing().when(authService).logout(any(RefreshTokenRequest.class));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "current-refresh.jwt"
                        ))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Refresh Token은 빈 값일 수 없다")
    void refreshTokenIsRequired() throws Exception {
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", ""
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("폐기된 Refresh Token은 401을 반환한다")
    void revokedRefreshTokenReturnsUnauthorized() throws Exception {
        when(authService.refresh(any())).thenThrow(new AuthException("Refresh token revoked"));

        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "revoked-refresh.jwt"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }
}
