package com.lingko.lingko.core.domain.legal;

import java.util.Locale;

/**
 * 공개 서빙 대상인 법무 문서의 종류다.
 *
 * <p>경로 문자열과 파일명을 한곳에 묶어, URL이 바뀌어도 리소스 위치를 찾아 헤매지 않게 한다.
 * 문서 원본은 저장소의 {@code docs/legal/}이며 여기의 리소스는 그 사본이다.
 * 두 벌이 어긋나면 {@code LegalDocumentSourceSyncTest}가 실패한다.
 */
public enum LegalDocument {

    /** 이용약관. 가입 시 필수 동의 대상이다. */
    TERMS("terms", "terms-of-service"),

    /** 개인정보 처리방침. 동의가 아니라 공개·확인 대상이다. */
    PRIVACY("privacy", "privacy-policy");

    private final String path;
    private final String baseFileName;

    LegalDocument(String path, String baseFileName) {
        this.path = path;
        this.baseFileName = baseFileName;
    }

    /** URL의 마지막 구간이다. 예: {@code /legal/terms}의 {@code terms}. */
    public String path() {
        return path;
    }

    /**
     * 주어진 언어의 Markdown 리소스 경로를 만든다.
     *
     * @param language {@link LegalLanguage}가 이미 검증한 언어
     */
    public String resourcePath(LegalLanguage language) {
        return "legal/%s.%s.md".formatted(baseFileName, language.code());
    }

    /**
     * URL 구간을 문서 종류로 해석한다.
     *
     * @return 알 수 없는 구간이면 {@code null}. 호출자는 이를 404로 다뤄야 하며,
     *         예외를 던지지 않는 이유는 사용자가 임의로 입력할 수 있는 값이라
     *         정상적인 흐름에서도 자주 어긋나기 때문이다.
     */
    public static LegalDocument fromPath(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        for (LegalDocument document : values()) {
            if (document.path.equals(normalized)) {
                return document;
            }
        }
        return null;
    }
}
