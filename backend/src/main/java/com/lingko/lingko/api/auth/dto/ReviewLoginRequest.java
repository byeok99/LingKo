package com.lingko.lingko.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** App Review 담당자가 전달받은 4~128자 운영 코드로 제한된 심사용 세션을 요청한다. */
public record ReviewLoginRequest(
        @NotBlank
        @Size(min = 4, max = 128)
        String accessCode
) {
    /** validation이 실제 전송값에 적용되도록 객체 생성과 동시에 앞뒤 공백을 정규화한다. */
    public ReviewLoginRequest {
        accessCode = accessCode == null ? null : accessCode.trim();
    }

    /** 앞뒤 입력 실수만 제거하며 코드 자체의 대소문자와 내부 문자는 그대로 보존한다. */
    public String trimmedAccessCode() {
        return accessCode;
    }
}
