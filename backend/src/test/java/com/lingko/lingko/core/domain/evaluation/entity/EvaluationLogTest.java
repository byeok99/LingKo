package com.lingko.lingko.core.domain.evaluation.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Evaluation Log Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class EvaluationLogTest {

    @Test
    @DisplayName("addSyllable은 aggregate 리스트와 역참조를 함께 설정한다")
    void addSyllableKeepsBidirectionalRelation() {
        EvaluationLog evaluationLog = EvaluationLog.builder()
                .user(User.builder()
                        .socialId("social-id")
                        .socialType(User.SocialType.GOOGLE)
                        .build())
                .originalWord("밥")
                .score(90)
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .standardPronunciation("밥")
                .pronunciationScore(new BigDecimal("90.00"))
                .build();
        EvaluationSyllable syllable = EvaluationSyllable.builder()
                .syllable(Syllable.builder().syllableChar("밥").build())
                .positionNo(0)
                .score(80)
                .feedback("Final consonant needs more clarity")
                .mouthGuideUrl("https://example.com/mouth/bap.png")
                .tongueGuideUrl("https://example.com/tongue/bap.png")
                .build();

        evaluationLog.addSyllable(syllable);

        assertThat(evaluationLog.getSyllableList()).containsExactly(syllable);
        assertThat(syllable.getEvaluationLog()).isSameAs(evaluationLog);
        assertThat(syllable.getPositionNo()).isZero();
        assertThat(syllable.getFeedback()).isEqualTo("Final consonant needs more clarity");
        assertThat(syllable.getMouthGuideUrl()).isEqualTo("https://example.com/mouth/bap.png");
        assertThat(syllable.getTongueGuideUrl()).isEqualTo("https://example.com/tongue/bap.png");
    }

    @Test
    @DisplayName("addSyllable은 null을 허용하지 않는다")
    void addSyllableRejectsNull() {
        EvaluationLog evaluationLog = EvaluationLog.builder()
                .user(User.builder()
                        .socialId("social-id")
                        .socialType(User.SocialType.GOOGLE)
                        .build())
                .originalWord("밥")
                .score(90)
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .standardPronunciation("밥")
                .pronunciationScore(new BigDecimal("90.00"))
                .build();

        assertThatThrownBy(() -> evaluationLog.addSyllable(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("syllable must not be null");
    }

    @Test
    @DisplayName("EvaluationLog는 평가 저장에 필요한 발음, 점수, 입력 source snapshot을 가진다")
    void evaluationLogKeepsEvaluationSnapshot() {
        EvaluationLog evaluationLog = EvaluationLog.builder()
                .user(User.builder()
                        .socialId("social-id")
                        .socialType(User.SocialType.GOOGLE)
                        .build())
                .originalWord("맛있겠다.")
                .score(82)
                .source(EvaluationLog.PracticeSource.RECOMMENDED)
                .sentenceId(1L)
                .standardPronunciation("마싯게따.")
                .recognizedText("마싣게따")
                .accuracyScore(new BigDecimal("84.25"))
                .fluencyScore(new BigDecimal("80.50"))
                .completenessScore(new BigDecimal("91.00"))
                .pronunciationScore(new BigDecimal("82.75"))
                .audioUrl("https://example.com/audio/evaluation.wav")
                .build();

        assertThat(evaluationLog.getSource()).isEqualTo(EvaluationLog.PracticeSource.RECOMMENDED);
        assertThat(evaluationLog.getSentenceId()).isEqualTo(1L);
        assertThat(evaluationLog.getStandardPronunciation()).isEqualTo("마싯게따.");
        assertThat(evaluationLog.getRecognizedText()).isEqualTo("마싣게따");
        assertThat(evaluationLog.getAccuracyScore()).isEqualByComparingTo("84.25");
        assertThat(evaluationLog.getFluencyScore()).isEqualByComparingTo("80.50");
        assertThat(evaluationLog.getCompletenessScore()).isEqualByComparingTo("91.00");
        assertThat(evaluationLog.getPronunciationScore()).isEqualByComparingTo("82.75");
        assertThat(evaluationLog.getAudioUrl()).isEqualTo("https://example.com/audio/evaluation.wav");
    }
}
