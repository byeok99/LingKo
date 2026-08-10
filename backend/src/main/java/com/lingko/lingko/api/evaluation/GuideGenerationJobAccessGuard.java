package com.lingko.lingko.api.evaluation;

import com.lingko.lingko.core.config.GuideGenerationJobSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.ActiveSessionAuthenticator;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobAccessDeniedException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobRateLimitExceededException;
import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * guide-jobs가 내부 service token을 가진 호출자만 통과시키고 caller별 요청량을 제한한다.
 */
@Component
@Slf4j
public class GuideGenerationJobAccessGuard {

    static final String INTERNAL_CALLER_ID = "guide-internal-service";
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private final GuideGenerationJobSettings settings;
    private final ActiveSessionAuthenticator activeSessionAuthenticator;
    private final GuideGenerationJobTelemetry telemetry;
    private final Clock clock;
    private final ConcurrentHashMap<String, RateWindow> windowsByCaller = new ConcurrentHashMap<>();

    @Autowired
    public GuideGenerationJobAccessGuard(
            GuideGenerationJobSettings settings,
            ActiveSessionAuthenticator activeSessionAuthenticator,
            GuideGenerationJobTelemetry telemetry,
            ObjectProvider<Clock> clockProvider
    ) {
        this(
                settings,
                activeSessionAuthenticator,
                telemetry,
                clockProvider.getIfAvailable(Clock::systemUTC)
        );
    }

    GuideGenerationJobAccessGuard(
            GuideGenerationJobSettings settings,
            ActiveSessionAuthenticator activeSessionAuthenticator,
            GuideGenerationJobTelemetry telemetry,
            Clock clock
    ) {
        this.settings = settings;
        this.activeSessionAuthenticator = activeSessionAuthenticator;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    /**
     * 내부 token을 인증한 뒤 한 요청을 소비하고 감사 로그용 caller ID를 반환한다.
     */
    public String authorizeAndConsume(String internalToken, String authorization) {
        String callerId = authorize(internalToken, authorization);
        consumeRateLimit(callerId);
        return callerId;
    }

    /** 상태 polling은 비용 작업을 만들지 않으므로 인증만 하고 생성 요청 한도는 소비하지 않는다. */
    public String authorize(String internalToken, String authorization) {
        if (internalToken != null && !internalToken.isBlank()) {
            if (!secureEquals(settings.getInternalToken(), internalToken)) {
                telemetry.request("unauthorized");
                log.warn("Guide job authentication rejected: credential=internal");
                throw new AuthException("Invalid guide service token");
            }
            return INTERNAL_CALLER_ID;
        }

        if (authorization != null && !authorization.isBlank()) {
            Long userId;
            try {
                userId = activeSessionAuthenticator.authenticateBearer(authorization);
            } catch (AuthException exception) {
                telemetry.request("unauthorized");
                log.warn("Guide job authentication rejected: credential=bearer");
                throw exception;
            }
            telemetry.request("forbidden");
            log.warn("Guide job authorization rejected: principal=user, userId={}", userId);
            throw new GuideJobAccessDeniedException();
        }

        telemetry.request("unauthorized");
        throw new AuthException("Missing guide service token");
    }

    private void consumeRateLimit(String callerId) {
        Instant now = clock.instant();
        windowsByCaller.compute(callerId, (ignored, current) -> {
            Instant windowEnd = current == null ? now : current.startedAt().plus(RATE_WINDOW);
            if (current == null || !now.isBefore(windowEnd)) {
                return new RateWindow(now, 1);
            }
            if (current.count() >= settings.getRequestsPerMinute()) {
                long retryAfter = Math.max(1, Duration.between(now, windowEnd).toSeconds());
                telemetry.request("rate_limited");
                log.warn("Guide job rate limited: caller={}", callerId);
                throw new GuideJobRateLimitExceededException(retryAfter);
            }
            return new RateWindow(current.startedAt(), current.count() + 1);
        });
    }

    private boolean secureEquals(String expected, String actual) {
        byte[] expectedBytes = expected == null
                ? new byte[0]
                : expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private record RateWindow(Instant startedAt, int count) { }
}
