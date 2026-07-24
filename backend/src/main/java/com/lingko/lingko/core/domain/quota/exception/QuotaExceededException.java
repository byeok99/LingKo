package com.lingko.lingko.core.domain.quota.exception;

/**
 * 할당량 Exceeded 도메인 실패 조건을 나타낸다.
 *
 * API 계층이 불안정한 메시지 문자열을 파싱하지 않고 실패 종류를 안전하게 매핑하도록 전용 예외를 사용한다.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
