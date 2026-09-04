package com.lingko.lingko.api.legal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 인증된 사용자가 선택한 현재 약관 항목을 서버 기록 경계로 전달한다.
 *
 * <p>사용자 ID와 서버 기록 시각은 body에서 받지 않는다. 사용자는 Bearer token으로
 * 식별하고 증거 시각은 서버가 생성해 위조 가능한 입력에 의존하지 않는다.</p>
 */
public record LegalConsentRequest(
        @AssertTrue Boolean termsAgreed,
        @AssertTrue Boolean privacyAcknowledged,
        @NotNull Boolean marketingOptIn,
        @NotBlank @Size(max = 32) @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String documentVersion,
        @NotNull Instant agreedAt
) {
}
