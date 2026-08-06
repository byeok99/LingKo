package com.lingko.lingko.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 표준 발음 한글을 초성·중성·종성으로 분해해 학습자용 로마자 가이드로 변환한다.
 *
 * 엄격한 지명 표기보다 읽기 보조가 목적이므로 음절은 하이픈, 단어는 공백으로 보존한다.
 */
public final class KoreanRomanizationUtil {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int VOWEL_COUNT = 21;
    private static final int FINAL_COUNT = 28;

    private static final List<String> INITIALS = List.of(
            "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
            "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    );
    private static final List<String> VOWELS = List.of(
            "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae",
            "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    );
    private static final List<String> FINALS = List.of(
            "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "l", "l", "l",
            "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t", "k", "t", "p", "t"
    );

    private KoreanRomanizationUtil() {
    }

    /** 표준 발음의 단어 경계를 유지한 로마자 가이드를 반환한다. */
    public static String romanize(String standardPronunciation) {
        String normalized = PracticeSentenceNormalizer.normalize(standardPronunciation);
        if (normalized.isEmpty()) {
            return "";
        }

        return List.of(normalized.split(" ")).stream()
                .map(KoreanRomanizationUtil::romanizeWord)
                .filter(value -> !value.isEmpty())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    /**
     * 어절 하나의 로마자를 반환한다. 음절은 하이픈으로 잇는다.
     *
     * 화면이 어절·음절마다 로마자를 병기하므로 문장 전체 결과를 잘라 쓰는 대신
     * 같은 규칙을 같은 함수로 적용한다. 문장을 잘라 쓰면 정규화로 어절 수가 달라졌을 때
     * 로마자와 한글이 어긋난다.
     */
    public static String romanizeSegment(String segment) {
        String normalized = PracticeSentenceNormalizer.normalize(segment);
        if (normalized.isEmpty()) {
            return "";
        }
        // 공백이 남아 있으면 어절 단위로 다시 나눠 문장 규칙과 같은 결과를 만든다.
        return romanize(normalized);
    }

    private static String romanizeWord(String word) {
        List<String> segments = new ArrayList<>();
        StringBuilder preservedRun = new StringBuilder();

        word.codePoints().forEach(codePoint -> {
            if (isHangulSyllable(codePoint)) {
                flushPreservedRun(segments, preservedRun);
                segments.add(romanizeSyllable(codePoint));
            } else {
                preservedRun.appendCodePoint(codePoint);
            }
        });
        flushPreservedRun(segments, preservedRun);
        return String.join("-", segments);
    }

    /**
     * 완성형 한글 음절 한 글자인지 판별한다.
     *
     * 취약 음절 집계처럼 문자열을 글자 단위로 쪼개는 쪽에서도 같은 기준이 필요해 공개한다.
     * 자모(ㄱ, ㅏ)나 구두점·공백은 발음 학습의 단위가 아니므로 false다.
     */
    public static boolean isHangulSyllable(int codePoint) {
        return codePoint >= HANGUL_BASE && codePoint <= HANGUL_END;
    }

    private static String romanizeSyllable(int codePoint) {
        int offset = codePoint - HANGUL_BASE;
        int initialIndex = offset / (VOWEL_COUNT * FINAL_COUNT);
        int vowelIndex = (offset % (VOWEL_COUNT * FINAL_COUNT)) / FINAL_COUNT;
        int finalIndex = offset % FINAL_COUNT;
        return INITIALS.get(initialIndex) + VOWELS.get(vowelIndex) + FINALS.get(finalIndex);
    }

    private static void flushPreservedRun(List<String> segments, StringBuilder preservedRun) {
        if (preservedRun.isEmpty()) {
            return;
        }
        segments.add(preservedRun.toString().toLowerCase(Locale.ROOT));
        preservedRun.setLength(0);
    }
}
