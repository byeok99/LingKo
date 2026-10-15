package com.lingko.lingko.api.common;

import java.util.List;

/**
 * LingKo API가 반환하는 공통 오류 envelope를 정의한다.
 *
 * 클라이언트가 내부 예외 형식에 의존하지 않고 모든 실패를 일관되게 처리하도록 공통 구조를 선택했다.
 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldErrorDetail> details,
        String requestId
) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    /** 현재 HTTP 요청의 ID를 포함한 상세 항목 없는 오류 응답을 만든다. */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), RequestIdContext.current());
    }

    /** 현재 HTTP 요청의 ID와 validation 상세 항목을 함께 담은 오류 응답을 만든다. */
    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        // HTTP 요청 밖의 직접 호출에서는 null이며, 요청 중에는 응답 Header와 같은 MDC 값을 사용한다.
        return new ErrorResponse(code, message, details, RequestIdContext.current());
    }

    public record FieldErrorDetail(
            String field,
            String reason
    ) {
    }
}
