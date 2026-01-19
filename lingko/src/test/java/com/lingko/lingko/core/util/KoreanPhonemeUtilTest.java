package com.lingko.lingko.core.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class KoreanPhonemeUtilTest {

    @Test
    void testToPronunciation_연음화() {
        assertThat(KoreanPhonemeUtil.toPronunciation("밥이"))
                .isEqualTo("바비");
    }

    @Test
    void testToPronunciation_비음화() {
        assertThat(KoreanPhonemeUtil.toPronunciation("국물"))
                .isEqualTo("궁물");
    }

    @Test
    void testToPronunciation_경음화() {
        assertThat(KoreanPhonemeUtil.toPronunciation("국밥"))
                .isEqualTo("국빱");
    }

    @Test
    void testToPronunciation_구개음화() {
        assertThat(KoreanPhonemeUtil.toPronunciation("같이"))
                .isEqualTo("가치");
    }
}