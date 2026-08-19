package com.lingko.lingko.core.domain.auth.exception;

/** 같은 원격 주소의 심사용 로그인 시도가 허용된 시간 창을 초과했음을 나타낸다. */
public class ReviewAccessRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    /** 클라이언트가 다시 시도할 수 있는 최소 대기 시간을 1초 이상으로 보존한다. */
    public ReviewAccessRateLimitExceededException(long retryAfterSeconds) {
        super("Review access request rate exceeded");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /** HTTP {@code Retry-After} header로 전달할 남은 시간이다. */
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
