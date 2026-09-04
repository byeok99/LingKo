package com.lingko.lingko.core.domain.quota.exception;

/** AdMob callback의 서명 또는 지급 parameter를 신뢰할 수 없을 때 발생한다. */
public class AdMobSsvVerificationException extends RuntimeException {
    public AdMobSsvVerificationException(String message) {
        super(message);
    }

    public AdMobSsvVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
