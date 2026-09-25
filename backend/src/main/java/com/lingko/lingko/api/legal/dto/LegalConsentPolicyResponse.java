package com.lingko.lingko.api.legal.dto;

/**
 * 로그인 전에 동의 화면이 사용할 서버의 현재 법무 문서 버전을 반환한다.
 */
public record LegalConsentPolicyResponse(
        String documentVersion
) {
}
