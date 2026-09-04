package com.lingko.lingko.api.legal.dto;

/**
 * 현재 문서 버전에 대해 동의 화면을 다시 보여줘야 하는지 반환한다.
 */
public record LegalConsentStatusResponse(
        boolean required,
        String documentVersion
) {
}
