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
}
