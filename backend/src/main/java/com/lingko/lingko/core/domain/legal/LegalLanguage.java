package com.lingko.lingko.core.domain.legal;

import java.util.Locale;

/**
 * 법무 문서가 제공되는 언어다.
 *
 * <p>문서는 한국어와 영어 두 벌만 존재한다. 지원하지 않는 언어를 요청받아도 오류로
 * 처리하지 않고 한국어로 되돌린다. 약관은 어떤 상황에서도 읽을 수 있어야 하는 문서라
 * 언어 선택 실패가 열람 자체를 막는 편이 더 나쁘기 때문이다.
 */
public enum LegalLanguage {

    KOREAN("ko", "한국어", "English"),
    ENGLISH("en", "English", "한국어");

    /** 기본 언어다. 알 수 없는 값이 오면 여기로 되돌린다. */
    public static final LegalLanguage DEFAULT = KOREAN;

    private final String code;
    private final String displayName;
    private final String otherDisplayName;

    LegalLanguage(String code, String displayName, String otherDisplayName) {
        this.code = code;
        this.displayName = displayName;
        this.otherDisplayName = otherDisplayName;
    }

    /** 파일명과 {@code lang} 파라미터에 쓰는 코드다. */
    public String code() {
        return code;
    }

    /** {@code <html lang>} 속성 값이다. */
    public String htmlLang() {
        return code;
    }

    /** 현재 언어의 표시 이름이다. */
    public String displayName() {
        return displayName;
    }

    /** 전환 링크에 쓰는 다른 언어의 표시 이름이다. */
    public String otherDisplayName() {
        return otherDisplayName;
    }

    /** 두 언어뿐이므로 전환 대상은 항상 나머지 하나다. */
    public LegalLanguage other() {
        return this == KOREAN ? ENGLISH : KOREAN;
    }

    /**
     * 요청 파라미터를 언어로 해석한다.
     *
     * @param value {@code null}이거나 지원하지 않는 값이면 {@link #DEFAULT}를 돌려준다
     */
    public static LegalLanguage from(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (LegalLanguage language : values()) {
            if (language.code.equals(normalized)) {
                return language;
            }
        }
        return DEFAULT;
    }
}
