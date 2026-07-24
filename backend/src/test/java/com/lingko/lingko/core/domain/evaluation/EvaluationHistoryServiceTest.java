package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationHistoryService;
import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluation History 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evaluation_history;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(EvaluationHistoryService.class)
class EvaluationHistoryServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Autowired
    private EvaluationHistoryService historyService;

    @Test
    @DisplayName("학습 기록 service는 최신 기록 page와 최고 점수를 반환한다")
    void findsHistoryPageWithBestScore() {
        User user = persistUser("history-user");
        User otherUser = persistUser("other-user");
        Syllable syllable = Syllable.builder().syllableChar("마").build();
        entityManager.persist(syllable);
        EvaluationLog older = evaluationLog(user, 76, "밥을 먹었어요.");
        older.addSyllable(EvaluationSyllable.builder()
                .syllable(syllable)
                .positionNo(0)
                .score(76)
                .feedback("Older feedback")
                .build());
        EvaluationLog newer = evaluationLog(user, 91, "맛있겠다.");
        newer.addSyllable(EvaluationSyllable.builder()
                .syllable(syllable)
                .positionNo(0)
                .score(91)
                .feedback("Stable vowel shape")
                .mouthGuideUrl("https://example.com/mouth/ma.png")
                .build());
        evaluationLogRepository.save(older);
        evaluationLogRepository.save(newer);
        evaluationLogRepository.save(evaluationLog(otherUser, 100, "다른 사용자"));
        evaluationLogRepository.flush();
        entityManager.clear();

        PracticeHistoryResponse response = historyService.findHistory(user.getUserIdx(), 0, 1);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getEvaluationLogId()).isEqualTo(newer.getEvaluationLogIdx());
        assertThat(response.getItems().get(0).getOriginalText()).isEqualTo("맛있겠다.");
        assertThat(response.getItems().get(0).getOverallScore()).isEqualTo(91);
        assertThat(response.getItems().get(0).getScoreBreakdown().getAccuracy()).isEqualTo(92);
        assertThat(response.getItems().get(0).getCharacters()).hasSize(1);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(1);
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getBestScore()).isEqualTo(91);
    }

    private User persistUser(String socialId) {
        User user = User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build();
        entityManager.persist(user);
        return user;
    }

    private EvaluationLog evaluationLog(User user, int score, String originalText) {
        return EvaluationLog.builder()
                .user(user)
                .originalWord(originalText)
                .score(score)
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .standardPronunciation(originalText)
                .recognizedText(originalText)
                .accuracyScore(new BigDecimal("92.00"))
                .fluencyScore(new BigDecimal("90.00"))
                .completenessScore(new BigDecimal("88.00"))
                .pronunciationScore(BigDecimal.valueOf(score))
                .build();
    }
}
