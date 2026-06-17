package com.lingko.lingko.api.common;

import java.util.List;

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
