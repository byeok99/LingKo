package com.lingko.lingko.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Korean Phoneme Util Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class KoreanPhonemeUtilTest {

    @ParameterizedTest
    @CsvSource({
            "밥이, 바비",
            "옷이, 오시",
            "책을, 채글",
            "꽃은, 꼬츤"
    })
    void testToPronunciation_연음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "국물, 궁물",
            "먹는, 멍는",
            "닫는, 단는",
            "밥물, 밤물"
    })
    void testToPronunciation_비음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "국밥, 국빱",
            "먹고, 먹꼬",
            "닫다, 닫따",
            "집세, 집쎄"
    })
    void testToPronunciation_경음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "같이, 가치",
            "굳이, 구지",
            "해돋이, 해도지",
            "붙이다, 부치다"
    })
    void testToPronunciation_구개음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "신라, 실라",
            "칼날, 칼랄"
    })
    void testToPronunciation_유음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "국화, 구콰",
            "닫히다, 다티다",
            "입학, 이팍",
            "좋고, 조코",
            "좋다, 조타",
            "좋지, 조치"
    })
    void testToPronunciation_격음화(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "넋, 넉",
            "앉, 안",
            "닭, 닥",
            "삶, 삼",
            "읊, 읍",
            "값, 갑"
    })
    void testToPronunciation_종성7음(String input, String expected) {
        assertThat(KoreanPhonemeUtil.toPronunciation(input))
                .isEqualTo(expected);
    }

    @Test
    void testToPronunciation_비한글과공백은유지한다() {
        assertThat(KoreanPhonemeUtil.toPronunciation("밥이 good!"))
                .isEqualTo("바비 good!");
    }
}
