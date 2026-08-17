package com.lingko.lingko.core.domain.auth.service;

/**
 * OAuth identity verifier의 공급자 중립 도메인 경계를 정의한다.
 *
 * 업무 조율이 특정 외부 공급자 구현에 결합되지 않도록 interface를 선택했다.
 */
public interface OAuthIdentityVerifier {
    /** 이 구현이 검증하는 요청 provider 이름을 반환한다. */
    String provider();

    /**
     * 외부 identity token을 검증한다. {@code rawNonce}는 nonce를 지원하는 provider에만 전달된다.
     */
    OAuthIdentity verify(String idToken, String rawNonce);
}
