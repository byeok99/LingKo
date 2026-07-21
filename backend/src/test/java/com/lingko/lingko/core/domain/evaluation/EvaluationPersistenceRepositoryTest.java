package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evaluation_repository;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
class EvaluationPersistenceRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Test
    @DisplayName("EvaluationLog는 확장된 평가 snapshot과 글자별 결과를 cascade 저장한다")
    void savesEvaluationSnapshotWithCharacterResults() {
        User user = User.builder()
                .socialId("social-id")
                .socialType(User.SocialType.GOOGLE)
                .build();
        Syllable syllable = Syllable.builder()
                .syllableChar("싯")
                .mouthUrl("https://example.com/mouth/sit.png")
                .tongueUrl("https://example.com/tongue/sit.png")
                .build();
        entityManager.persist(user);
        entityManager.persist(syllable);

        EvaluationLog evaluationLog = EvaluationLog.builder()
                .user(user)
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
        evaluationLog.addSyllable(EvaluationSyllable.builder()
                .syllable(syllable)
                .score(68)
                .positionNo(1)
                .feedback("Keep the tongue closer for the sibilant sound")
                .mouthGuideUrl("https://example.com/mouth/sit.png")
                .tongueGuideUrl("https://example.com/tongue/sit.png")
                .build());

        EvaluationLog saved = evaluationLogRepository.saveAndFlush(evaluationLog);
        entityManager.clear();

        EvaluationLog found = evaluationLogRepository.findById(saved.getEvaluationLogIdx()).orElseThrow();
        assertThat(found.getSource()).isEqualTo(EvaluationLog.PracticeSource.RECOMMENDED);
        assertThat(found.getSentenceId()).isEqualTo(1L);
        assertThat(found.getStandardPronunciation()).isEqualTo("마싯게따.");
        assertThat(found.getRecognizedText()).isEqualTo("마싣게따");
        assertThat(found.getAccuracyScore()).isEqualByComparingTo("84.25");
        assertThat(found.getFluencyScore()).isEqualByComparingTo("80.50");
        assertThat(found.getCompletenessScore()).isEqualByComparingTo("91.00");
        assertThat(found.getPronunciationScore()).isEqualByComparingTo("82.75");
        assertThat(found.getAudioUrl()).isEqualTo("https://example.com/audio/evaluation.wav");
        assertThat(found.getSyllableList()).hasSize(1);
        EvaluationSyllable characterResult = found.getSyllableList().get(0);
        assertThat(characterResult.getPositionNo()).isEqualTo(1);
        assertThat(characterResult.getFeedback()).isEqualTo("Keep the tongue closer for the sibilant sound");
        assertThat(characterResult.getMouthGuideUrl()).isEqualTo("https://example.com/mouth/sit.png");
        assertThat(characterResult.getTongueGuideUrl()).isEqualTo("https://example.com/tongue/sit.png");
    }
}
