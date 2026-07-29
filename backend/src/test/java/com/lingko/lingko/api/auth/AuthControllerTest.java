package com.lingko.lingko.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.AuthUserResponse;
import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.user.service.AccountDeletionService;
import com.lingko.lingko.core.domain.user.service.AccountDeletionUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @MockitoBean
    private ActiveSessionAuthenticator activeSessionAuthenticator;
    @MockitoBean
    private AccountDeletionService accountDeletionService;

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

    @Test
    @DisplayName("회원 탈퇴는 Access Token과 현재 Refresh Token을 검증하고 204를 반환한다")
    void deleteAccountRemovesAuthenticatedUser() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access.jwt")).thenReturn(7L);

        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer access.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "current-refresh.jwt"
                        ))))
                .andExpect(status().isNoContent());

        verify(accountDeletionService).deleteAccount(
                7L,
                new RefreshTokenRequest("current-refresh.jwt")
        );
    }

    @Test
    @DisplayName("회원 탈퇴 S3 정리 실패는 내부 원인을 숨긴 재시도 가능 503으로 반환한다")
    void deleteAccountReturnsSafeRetryableFailure() throws Exception {
        when(activeSessionAuthenticator.authenticateBearer("Bearer access.jwt")).thenReturn(7L);
        doThrow(new AccountDeletionUnavailableException(
                new IllegalStateException("sensitive S3 detail")
        )).when(accountDeletionService).deleteAccount(
                7L,
                new RefreshTokenRequest("current-refresh.jwt")
        );

        mockMvc.perform(delete("/api/auth/account")
                        .header("Authorization", "Bearer access.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", "current-refresh.jwt"
                        ))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DELETION_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "Account deletion is temporarily unavailable. Please try again."
                ));
    }
}
