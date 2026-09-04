package com.lingko.lingko.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * HTTP 경계에서 사용하는 OAuth 로그인 요청 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 *
 * @param provider 지원할 OAuth 공급자 식별자이며 대소문자와 바깥 공백은 정규화된다
 * @param idToken 외부 공급자가 서명한 identity token 원문
 * @param rawNonce Apple 요청에 사용한 nonce 원문이며 Google에서는 null일 수 있다
 * @param displayName Apple 최초 승인에서만 받을 수 있는 이름이며 null이면 기존 이름을 보존한다
 */
public record OAuthLoginRequest(
        @NotBlank @Size(max = 20) String provider,
        @NotBlank @Size(max = 8192) String idToken,
        @Size(min = 32, max = 128)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String rawNonce,
        @Size(max = 100)
        @Pattern(regexp = "^[^\\p{Cntrl}]*$")
        String displayName
) {
    /** Google 호출부가 Apple 전용 선택값 없이 기존 계약을 유지하도록 제공하는 편의 생성자다. */
    public OAuthLoginRequest(String provider, String idToken) {
        this(provider, idToken, null, null);
    }

    /** provider 비교가 대소문자와 바깥 공백에 영향받지 않게 정규화한다. */
    public String normalizedProvider() {
        return provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT);
    }

    /** 외부 token 전송 과정에서 붙을 수 있는 바깥 공백만 제거한다. */
    public String trimmedIdToken() {
        return idToken == null ? "" : idToken.trim();
    }

    /**
     * Apple token은 요청별 nonce와 결합되어야 하며 Google은 nonce를 요구하지 않는다.
     */
    @AssertTrue(message = "Apple login requires a raw nonce")
    public boolean isProviderCredentialValid() {
        return !"APPLE".equals(normalizedProvider()) || !trimmedRawNonce().isEmpty();
    }

    /** Apple nonce 검증에 사용할 원문에서 바깥 공백만 제거한다. */
    public String trimmedRawNonce() {
        return rawNonce == null ? "" : rawNonce.trim();
    }

    /** Apple이 최초 승인에서만 주는 이름을 저장 가능한 단일 공백 형식으로 정규화한다. */
    public String normalizedDisplayName() {
        if (displayName == null) {
            return null;
        }
        String normalized = displayName.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
