package com.lingko.lingko.core.domain.quota.exception;

/** SSV 설정 또는 공개키 공급자가 준비되지 않아 보상을 안전하게 처리할 수 없을 때 발생한다. */
public class AdRewardUnavailableException extends RuntimeException {
    public AdRewardUnavailableException(String message) {
        super(message);
    }

    public AdRewardUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
