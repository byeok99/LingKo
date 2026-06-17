package com.lingko.lingko.core.domain.evaluation.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .build();
        EvaluationSyllable syllable = EvaluationSyllable.builder()
                .syllable(Syllable.builder().syllableChar("밥").build())
                .score(80)
                .build();

        evaluationLog.addSyllable(syllable);

        assertThat(evaluationLog.getSyllableList()).containsExactly(syllable);
        assertThat(syllable.getEvaluationLog()).isSameAs(evaluationLog);
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
                .build();

        assertThatThrownBy(() -> evaluationLog.addSyllable(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("syllable must not be null");
    }
}
