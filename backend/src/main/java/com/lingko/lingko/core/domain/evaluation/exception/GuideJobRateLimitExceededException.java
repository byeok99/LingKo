package com.lingko.lingko.core.domain.evaluation.exception;

/** 내부 호출자의 제한 시간당 생성 요청 수가 초과됐음을 나타낸다. */
public class GuideJobRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public GuideJobRateLimitExceededException(long retryAfterSeconds) {
        super("Guide generation request rate exceeded");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
