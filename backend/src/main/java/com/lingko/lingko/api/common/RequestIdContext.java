package com.lingko.lingko.api.common;

import org.slf4j.MDC;

/**
 * 현재 HTTP 요청의 상관관계 ID를 MDC와 API 오류 응답이 같은 이름으로 공유하게 한다.
 *
 * HTTP 요청 밖에서 실행되는 Worker에는 값이 없으며, 그 경우 {@link #current()}는 {@code null}을 반환한다.
 */
final class RequestIdContext {

    static final String MDC_KEY = "requestId";

    private RequestIdContext() {
    }

    static String current() {
        return MDC.get(MDC_KEY);
    }
}
