package com.lingko.lingko.api.auth;

import com.lingko.lingko.core.config.ReviewAccessSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.ReviewAccessRateLimitExceededException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/** 심사용 접근 코드를 상수 시간에 검증하고 서버가 관찰한 원격 주소별 시도량을 제한한다. */
@Component
public class ReviewAccessGuard {

    private static final int MAX_TRACKED_SOURCES = 10_000;

    private final ReviewAccessSettings settings;
    private final Clock clock;
    private final ConcurrentHashMap<String, RateWindow> windowsBySource = new ConcurrentHashMap<>();

    /** 운영에서는 UTC clock을 사용하고 테스트에서만 결정적 clock으로 교체할 수 있게 구성한다. */
    @Autowired
    public ReviewAccessGuard(ReviewAccessSettings settings, ObjectProvider<Clock> clockProvider) {
        this(settings, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    ReviewAccessGuard(ReviewAccessSettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** 한 시도를 소비한 뒤 코드가 맞을 때만 환경 설정의 기존 review 사용자 ID를 반환한다. */
    public Long authorizeAndConsume(String accessCode, String remoteAddress) {
        if (!settings.isEnabled()) {
            throw denied();
        }

        String source = remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : remoteAddress;
        consumeRateLimit(source);
        if (!secureHashMatches(accessCode)) {
            throw denied();
        }
        return settings.getUserId();
    }

    private void consumeRateLimit(String source) {
        Instant now = clock.instant();
        Duration windowDuration = Duration.ofSeconds(settings.getWindowSeconds());
        if (windowsBySource.size() >= MAX_TRACKED_SOURCES && !windowsBySource.containsKey(source)) {
            // 공격자가 임의 주소를 대량 생성해 process memory를 계속 늘리지 못하도록 만료 창만 정리하고 상한을 지킨다.
            windowsBySource.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().startedAt().plus(windowDuration))
            );
            if (windowsBySource.size() >= MAX_TRACKED_SOURCES) {
                throw new ReviewAccessRateLimitExceededException(settings.getWindowSeconds());
            }
        }

        windowsBySource.compute(source, (ignored, current) -> {
            Instant windowEnd = current == null ? now : current.startedAt().plus(windowDuration);
            if (current == null || !now.isBefore(windowEnd)) {
                return new RateWindow(now, 1);
            }
            if (current.count() >= settings.getMaxAttempts()) {
                long retryAfter = Math.max(1, Duration.between(now, windowEnd).toSeconds());
                throw new ReviewAccessRateLimitExceededException(retryAfter);
            }
            return new RateWindow(current.startedAt(), current.count() + 1);
        });
    }

    private boolean secureHashMatches(String accessCode) {
        if (accessCode == null) {
            throw denied();
        }
        try {
            byte[] expected = HexFormat.of().parseHex(settings.getCodeSha256());
            byte[] actual = MessageDigest.getInstance("SHA-256")
                    .digest(accessCode.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException | NoSuchAlgorithmException exception) {
            // 활성 설정은 startup validation도 통과해야 하지만 런타임 변경에도 fail-closed 한다.
            throw denied();
        }
    }

    private AuthException denied() {
        // 기능 활성 여부·코드 불일치·설정 오류를 같은 실패로 보여 공격자의 추론 단서를 없앤다.
        return new AuthException("Review access denied");
    }

    private record RateWindow(Instant startedAt, int count) { }
}
