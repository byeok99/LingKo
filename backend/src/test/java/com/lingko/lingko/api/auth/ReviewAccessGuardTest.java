package com.lingko.lingko.api.auth;

import com.lingko.lingko.core.config.ReviewAccessSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.ReviewAccessRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 심사용 코드가 서버 설정과 요청 제한을 통과할 때만 지정 계정을 반환하는지 검증한다. */
class ReviewAccessGuardTest {

    private static final String VALID_CODE = "review-code-with-at-least-32-bytes";

    private ReviewAccessSettings settings;
    private MutableClock clock;
    private ReviewAccessGuard guard;

    @BeforeEach
    void setUp() throws Exception {
        settings = new ReviewAccessSettings();
        settings.setEnabled(true);
        settings.setCodeSha256(sha256Hex(VALID_CODE));
        settings.setUserId(73L);
        settings.setMaxAttempts(2);
        settings.setWindowSeconds(60);
        clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        guard = new ReviewAccessGuard(settings, clock);
    }

    @Test
    @DisplayName("활성화된 심사용 접근은 정확한 코드에 지정 사용자 ID를 반환한다")
    void authorizesConfiguredReviewAccount() {
        // When & Then
        assertThat(guard.authorizeAndConsume(VALID_CODE, "203.0.113.10")).isEqualTo(73L);
    }

    @Test
    @DisplayName("비활성화 상태와 잘못된 코드는 같은 인증 실패로 거부한다")
    void rejectsDisabledOrInvalidCode() {
        // When & Then: 잘못된 코드
        assertThatThrownBy(() -> guard.authorizeAndConsume("wrong-code-with-enough-length", "203.0.113.10"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Review access denied");

        // Given: 비활성화
        settings.setEnabled(false);

        // When & Then
        assertThatThrownBy(() -> guard.authorizeAndConsume(VALID_CODE, "203.0.113.11"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Review access denied");
    }

    @Test
    @DisplayName("심사용 접근 설정은 비활성화일 때만 빈 Secret을 허용한다")
    void validatesRequiredSettingsWhenEnabled() {
        // Given: 비활성화 기본값
        ReviewAccessSettings disabled = new ReviewAccessSettings();
        // Then
        assertThat(disabled.isSecureWhenEnabled()).isTrue();

        // When: Secret 없이 활성화
        disabled.setEnabled(true);
        // Then
        assertThat(disabled.isSecureWhenEnabled()).isFalse();

        // When: 필수 Secret을 채운다
        disabled.setCodeSha256(sha256HexUnchecked(VALID_CODE));
        disabled.setUserId(73L);
        // Then
        assertThat(disabled.isSecureWhenEnabled()).isTrue();
    }

    @Test
    @DisplayName("같은 원격 주소의 심사용 로그인 시도는 설정된 시간 창에서 제한한다")
    void rateLimitsAttemptsPerRemoteAddress() {
        // Given: 허용 횟수(2회)를 모두 실패로 소진한다
        assertThatThrownBy(() -> guard.authorizeAndConsume("first-wrong-code-with-enough-length", "203.0.113.10"))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> guard.authorizeAndConsume("second-wrong-code-with-enough-length", "203.0.113.10"))
                .isInstanceOf(AuthException.class);

        // When & Then: 시간 창 안에서는 정확한 코드도 제한한다
        assertThatThrownBy(() -> guard.authorizeAndConsume(VALID_CODE, "203.0.113.10"))
                .isInstanceOfSatisfying(
                        ReviewAccessRateLimitExceededException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(60)
                );

        // When: 시간 창이 지난다
        clock.advanceSeconds(60);
        // Then
        assertThat(guard.authorizeAndConsume(VALID_CODE, "203.0.113.10")).isEqualTo(73L);
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private static String sha256HexUnchecked(String value) {
        try {
            return sha256Hex(value);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
