package com.lingko.lingko.core.domain.legal;

/**
 * 현재 앱이 동의를 요구하는 법무 문서 버전을 한곳에서 정의한다.
 *
 * <p>버전은 문서 시행일과 같으며, 문서 개정 시 이 값을 올리면 기존 기록을 삭제하지 않고
 * 새 버전에 대한 재동의를 요구할 수 있다.</p>
 */
public final class LegalConsentPolicy {

    public static final String CURRENT_DOCUMENT_VERSION = "2026-08-07";

    private LegalConsentPolicy() {
    }
}
