package com.lingko.lingko.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 표준 발음을 단어 공백과 음절 경계를 보존한 학습자용 로마자로 변환하는 계약을 검증한다. */
class KoreanRomanizationUtilTest {

    @Test
    @DisplayName("표준 발음을 음절은 하이픈, 단어는 공백으로 구분해 변환한다")
    void romanizesLearnerFriendlyPronunciation() {
        assertThat(KoreanRomanizationUtil.romanize("저는 커피를 조아해요"))
                .isEqualTo("jeo-neun keo-pi-reul jo-a-hae-yo");
    }

    @Test
    @DisplayName("초성과 종성의 발음값을 구분하고 공백·기호를 정규화한다")
    void romanizesInitialAndFinalConsonants() {
        assertThat(KoreanRomanizationUtil.romanize("  밥 라면!  "))
                .isEqualTo("bap ra-myeon");
        assertThat(KoreanRomanizationUtil.romanize(""))
                .isEmpty();
        assertThat(KoreanRomanizationUtil.romanize(null))
                .isEmpty();
    }

    @Test
    @DisplayName("어절과 음절 단위도 문장과 같은 규칙으로 로마자를 만든다")
    void romanizesSegmentsWithTheSameRule() {
        // 화면이 어절·음절마다 로마자를 병기하므로, 문장 결과를 잘라 쓰지 않고
        // 같은 함수로 만들어 한글과 로마자가 어긋나지 않게 한다.
        assertThat(KoreanRomanizationUtil.romanizeSegment("커피를")).isEqualTo("keo-pi-reul");
        assertThat(KoreanRomanizationUtil.romanizeSegment("조")).isEqualTo("jo");

        // 어절 로마자를 이어붙이면 문장 로마자와 같아야 한다.
        String joined = String.join(
                " ",
                KoreanRomanizationUtil.romanizeSegment("저는"),
                KoreanRomanizationUtil.romanizeSegment("커피를"),
                KoreanRomanizationUtil.romanizeSegment("조아해요")
        );
        assertThat(joined).isEqualTo(KoreanRomanizationUtil.romanize("저는 커피를 조아해요"));
    }

    @Test
    @DisplayName("빈 값과 기호만 있는 입력은 빈 문자열을 반환한다")
    void romanizeSegmentHandlesEmptyInput() {
        assertThat(KoreanRomanizationUtil.romanizeSegment("")).isEmpty();
        assertThat(KoreanRomanizationUtil.romanizeSegment("  ")).isEmpty();
        assertThat(KoreanRomanizationUtil.romanizeSegment("!?")).isEmpty();
    }
}
