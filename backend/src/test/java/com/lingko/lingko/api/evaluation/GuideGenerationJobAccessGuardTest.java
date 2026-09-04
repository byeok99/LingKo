package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.core.config.GuideGenerationJobSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobAccessDeniedException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobRateLimitExceededException;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 가이드 생성 비용 경계가 내부 호출자만 허용하고 호출자별 요청량을 제한하는지 검증한다.
 */
class GuideGenerationJobAccessGuardTest {

    private static final String INTERNAL_TOKEN = "0123456789abcdef0123456789abcdef";

    private ActiveSessionAuthenticator authenticator;
    private GuideGenerationJobSettings settings;
    private GuideGenerationJobTelemetry telemetry;
    private MutableClock clock;
    private GuideGenerationJobAccessGuard guard;

    @BeforeEach
    void setUp() {
        authenticator = mock(ActiveSessionAuthenticator.class);
        settings = new GuideGenerationJobSettings();
        settings.setApiEnabled(true);
        settings.setInternalToken(INTERNAL_TOKEN);
        settings.setRequestsPerMinute(2);
        telemetry = mock(GuideGenerationJobTelemetry.class);
        clock = new MutableClock(Instant.parse("2026-08-09T00:00:00Z"));
        guard = new GuideGenerationJobAccessGuard(
                settings,
                authenticator,
                telemetry,
                clock
        );
    }

    @Test
    @DisplayName("내부 token이 없거나 틀리면 인증 실패다")
    void rejectsMissingOrInvalidInternalToken() {
        assertThatThrownBy(() -> guard.authorizeAndConsume(null, null))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> guard.authorizeAndConsume("wrong", null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("활성 일반 사용자 token은 인증됐어도 내부 생성 권한이 없어 403 대상이다")
    void rejectsAuthenticatedLearner() {
        when(authenticator.authenticateBearer("Bearer learner-token")).thenReturn(42L);

        assertThatThrownBy(() -> guard.authorizeAndConsume(null, "Bearer learner-token"))
                .isInstanceOf(GuideJobAccessDeniedException.class);
    }

    @Test
    @DisplayName("유효하지 않은 일반 Bearer token도 401 대상과 감사 지표로 기록한다")
    void recordsInvalidBearerAsUnauthorized() {
        doThrow(new AuthException("invalid bearer"))
                .when(authenticator).authenticateBearer("Bearer invalid-token");

        assertThatThrownBy(() -> guard.authorizeAndConsume(null, "Bearer invalid-token"))
                .isInstanceOf(AuthException.class);

        verify(telemetry).request("unauthorized");
    }

    @Test
    @DisplayName("정확한 내부 token은 고정된 감사 caller ID로 승인한다")
    void authorizesInternalCaller() {
        assertThat(guard.authorizeAndConsume(INTERNAL_TOKEN, null))
                .isEqualTo("guide-internal-service");
    }

    @Test
    @DisplayName("같은 내부 호출자가 분당 제한을 넘으면 retry 시각이 있는 429 대상이다")
    void rateLimitsInternalCallerPerMinute() {
        guard.authorizeAndConsume(INTERNAL_TOKEN, null);
        guard.authorizeAndConsume(INTERNAL_TOKEN, null);

        assertThatThrownBy(() -> guard.authorizeAndConsume(INTERNAL_TOKEN, null))
                .isInstanceOfSatisfying(
                        GuideJobRateLimitExceededException.class,
                        exception -> assertThat(exception.retryAfterSeconds()).isEqualTo(60)
                );

        clock.advanceSeconds(60);
        assertThat(guard.authorizeAndConsume(INTERNAL_TOKEN, null))
                .isEqualTo("guide-internal-service");
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
