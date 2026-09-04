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

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), null);
    }

    public static ErrorResponse of(String code, String message, List<FieldErrorDetail> details) {
        return new ErrorResponse(code, message, details, null);
    }

    public record FieldErrorDetail(
            String field,
            String reason
    ) {
    }
}
