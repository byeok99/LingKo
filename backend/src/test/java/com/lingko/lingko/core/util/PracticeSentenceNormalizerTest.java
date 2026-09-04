package com.lingko.lingko.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 앱을 우회한 문장도 특수기호 제거와 공백 정리 계약을 지키는지 검증한다.
 */
class PracticeSentenceNormalizerTest {

    @Test
    @DisplayName("Unicode 문장부호와 기호를 제거하고 연속 공백을 하나로 정리한다")
    void removesPunctuationAndSymbols() {
        assertThat(PracticeSentenceNormalizer.normalize(
                "  안녕하세요.!?  @LingKo #1 (연습)_테스트-좋아요😊₩  "
        )).isEqualTo("안녕하세요 LingKo 1 연습테스트좋아요");
    }

    @Test
    @DisplayName("null 또는 기호만 있는 문장은 빈 문자열로 정규화한다")
    void returnsEmptyForUnsupportedInput() {
        assertThat(PracticeSentenceNormalizer.normalize(null)).isEmpty();
        assertThat(PracticeSentenceNormalizer.normalize(".!?😊")).isEmpty();
    }
}
