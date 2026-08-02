package com.lingko.lingko.core.util;

import java.util.regex.Pattern;

/**
 * 사용자 연습 문장의 지원 문자 규칙을 한곳에서 적용한다.
 *
 * 앱을 우회한 API 요청도 동일하게 처리하도록 문장부호·기호 제거를 서버 경계 안에서 다시 수행한다.
 */
public final class PracticeSentenceNormalizer {

    private static final Pattern UNSUPPORTED_CHARACTERS = Pattern.compile("[\\p{P}\\p{S}]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private PracticeSentenceNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutSymbols = UNSUPPORTED_CHARACTERS.matcher(value).replaceAll("");
        return WHITESPACE.matcher(withoutSymbols).replaceAll(" ").trim();
    }
}
