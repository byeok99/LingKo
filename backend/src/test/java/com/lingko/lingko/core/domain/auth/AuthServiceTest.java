package com.lingko.lingko.core.domain.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.OAuthLoginRequest;
import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.config.JwtSettings;
import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import com.lingko.lingko.core.domain.auth.service.AuthService;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.auth.service.JwtTokenProvider;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentityVerifier;
import com.lingko.lingko.core.domain.auth.service.RefreshTokenHasher;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Auth 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=auth_service;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret-key=01234567890123456789012345678901",
        "jwt.access-token-expire-minutes=30",
        "jwt.refresh-token-expire-days=14",
        "jwt.algorithm=HS256"
})
@Import({
        AuthService.class,
        ActiveSessionAuthenticator.class,
        RefreshTokenHasher.class,
        AuthServiceTest.TestAuthConfig.class
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private ActiveSessionAuthenticator activeSessionAuthenticator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Google OAuth 로그인은 신규 사용자를 생성하고 JWT를 발급한다")
    void loginWithGoogleCreatesUserAndTokens() throws Exception {
        AuthTokenResponse response = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(1800L);
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");

        User user = userRepository.findBySocialIdAndSocialType("google-sub-123", User.SocialType.GOOGLE).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("LingKo User");

        JsonNode accessPayload = jwtPayload(response.getAccessToken());
        assertThat(accessPayload.get("sub").asText()).isEqualTo(String.valueOf(user.getUserIdx()));
        assertThat(accessPayload.get("typ").asText()).isEqualTo("access");

        RefreshTokenSession refreshSession = refreshTokenSessionRepository.findAll().getFirst();
        assertThat(accessPayload.get("sid").asText()).isEqualTo(refreshSession.getSessionId());
        assertThat(refreshSession.getCurrentTokenHash())
                .isEqualTo(refreshTokenHasher.hash(response.getRefreshToken()))
                .doesNotContain(response.getRefreshToken());
    }

    @Test
    @DisplayName("기존 Google 사용자는 중복 생성하지 않고 profile snapshot을 갱신한다")
    void loginWithGoogleReusesExistingUser() {
        User existing = User.builder()
                .socialId("google-sub-123")
                .socialType(User.SocialType.GOOGLE)
                .email("old@example.com")
                .name("Old Name")
                .build();
        entityManager.persist(existing);
        entityManager.flush();
        entityManager.clear();

        AuthTokenResponse response = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(response.getUser().getUserId()).isEqualTo(existing.getUserIdx());
        User updated = userRepository.findById(existing.getUserIdx()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo("user@example.com");
        assertThat(updated.getName()).isEqualTo("LingKo User");
    }

    @Test
    @DisplayName("지원하지 않는 OAuth provider는 거부한다")
    void unsupportedProviderIsRejected() {
        assertThatThrownBy(() -> authService.loginWithOAuth(new OAuthLoginRequest("APPLE", "valid-token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported OAuth provider");
    }

    @Test
    @DisplayName("JWT provider는 access token subject를 사용자 ID로 파싱한다")
    void jwtProviderParsesAccessTokenUserId() {
        JwtTokenProvider.TokenPair tokens = jwtTokenProvider.issueTokens(7L);

        assertThat(jwtTokenProvider.parseAccessTokenUserId(tokens.accessToken())).isEqualTo(7L);
    }

    @Test
    @DisplayName("JWT provider는 refresh token을 access token으로 사용할 수 없게 거부한다")
    void jwtProviderRejectsRefreshTokenAsAccessToken() {
        JwtTokenProvider.TokenPair tokens = jwtTokenProvider.issueTokens(7L);

        assertThatThrownBy(() -> jwtTokenProvider.parseAccessTokenUserId(tokens.refreshToken()))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Refresh Token 갱신은 같은 세션에서 token pair를 회전한다")
    void refreshRotatesTokenPair() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));
        JwtTokenProvider.RefreshTokenClaims originalClaims =
                jwtTokenProvider.parseRefreshToken(login.getRefreshToken());

        AuthTokenResponse refreshed = authService.refresh(new RefreshTokenRequest(login.getRefreshToken()));
        JwtTokenProvider.RefreshTokenClaims refreshedClaims =
                jwtTokenProvider.parseRefreshToken(refreshed.getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(login.getRefreshToken());
        assertThat(refreshedClaims.sessionId()).isEqualTo(originalClaims.sessionId());
        assertThat(refreshedClaims.tokenId()).isNotEqualTo(originalClaims.tokenId());
        assertThat(refreshedClaims.expiresAt()).isEqualTo(originalClaims.expiresAt());

        RefreshTokenSession session = refreshTokenSessionRepository.findById(originalClaims.sessionId()).orElseThrow();
        assertThat(session.getCurrentTokenHash()).isEqualTo(refreshTokenHasher.hash(refreshed.getRefreshToken()));
        assertThat(session.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("회전 전 Refresh Token 재사용은 세션 전체를 폐기한다")
    void reusedRefreshTokenRevokesSession() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));
        AuthTokenResponse refreshed = authService.refresh(new RefreshTokenRequest(login.getRefreshToken()));
        String sessionId = jwtTokenProvider.parseRefreshToken(login.getRefreshToken()).sessionId();

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(login.getRefreshToken())))
                .isInstanceOf(AuthException.class);

        RefreshTokenSession session = refreshTokenSessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.isRevoked()).isTrue();
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshed.getRefreshToken())))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("로그아웃은 현재 Refresh Token 세션을 폐기한다")
    void logoutRevokesRefreshSession() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));
        String sessionId = jwtTokenProvider.parseRefreshToken(login.getRefreshToken()).sessionId();

        assertThat(activeSessionAuthenticator.authenticateBearer("Bearer " + login.getAccessToken()))
                .isEqualTo(login.getUser().getUserId());
        authService.logout(new RefreshTokenRequest(login.getRefreshToken()));

        RefreshTokenSession session = refreshTokenSessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.isRevoked()).isTrue();
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(login.getRefreshToken())))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> activeSessionAuthenticator.authenticateBearer(
                "Bearer " + login.getAccessToken()
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("회원 탈퇴 재확인은 Access Token 사용자와 현재 Refresh Token 소유자가 같아야 한다")
    void validatesCurrentRefreshTokenForAccountDeletion() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));

        authService.validateCurrentRefreshToken(
                login.getUser().getUserId(),
                new RefreshTokenRequest(login.getRefreshToken())
        );

        assertThatThrownBy(() -> authService.validateCurrentRefreshToken(
                login.getUser().getUserId() + 1,
                new RefreshTokenRequest(login.getRefreshToken())
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("회전 전 Refresh Token으로는 회원 탈퇴를 승인하지 않는다")
    void rejectsStaleRefreshTokenForAccountDeletion() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));
        authService.refresh(new RefreshTokenRequest(login.getRefreshToken()));

        assertThatThrownBy(() -> authService.validateCurrentRefreshToken(
                login.getUser().getUserId(),
                new RefreshTokenRequest(login.getRefreshToken())
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("DB 세션 절대 만료가 지나면 유효한 JWT도 갱신할 수 없다")
    void expiredDatabaseSessionIsRejected() {
        AuthTokenResponse login = authService.loginWithOAuth(new OAuthLoginRequest("GOOGLE", "valid-token"));
        String sessionId = jwtTokenProvider.parseRefreshToken(login.getRefreshToken()).sessionId();
        entityManager.createNativeQuery("""
                        UPDATE auth_refresh_sessions
                        SET expires_at = DATEADD('DAY', -1, CURRENT_TIMESTAMP)
                        WHERE session_id = :sessionId
                        """)
                .setParameter("sessionId", sessionId)
                .executeUpdate();
        entityManager.clear();

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(login.getRefreshToken())))
                .isInstanceOf(AuthException.class);
    }

    private JsonNode jwtPayload(String token) throws Exception {
        String payload = token.split("\\.")[1];
        byte[] decoded = Base64.getUrlDecoder().decode(payload);

        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestAuthConfig {
        @Bean
        @Primary
        JwtSettings jwtSettings() {
            JwtSettings settings = new JwtSettings();
            settings.setSecretKey("01234567890123456789012345678901");
            settings.setAccessTokenExpireMinutes(30);
            settings.setRefreshTokenExpireDays(14);
            settings.setAlgorithm("HS256");

            return settings;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        JwtTokenProvider jwtTokenProvider(JwtSettings jwtSettings, ObjectMapper objectMapper) {
            return new JwtTokenProvider(jwtSettings, objectMapper);
        }

        @Bean
        OAuthIdentityVerifier googleIdentityVerifier() {
            return idToken -> {
                if (!"valid-token".equals(idToken)) {
                    throw new IllegalArgumentException("invalid token");
                }

                return new OAuthIdentity(
                        "google-sub-123",
                        "user@example.com",
                        "LingKo User",
                        "https://example.com/profile.png"
                );
            };
        }
    }
}
