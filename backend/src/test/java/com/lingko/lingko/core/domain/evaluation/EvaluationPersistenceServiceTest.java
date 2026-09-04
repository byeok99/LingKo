package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.api.evaluation.dto.GuideStatus;
import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.SyllableRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Evaluation Persistence 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:evaluation_persistence_service;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(EvaluationPersistenceService.class)
class EvaluationPersistenceServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EvaluationPersistenceService persistenceService;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Autowired
    private SyllableRepository syllableRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("평가 결과 저장 service는 log와 글자별 결과를 cascade 저장한다")
    void savesEvaluationResultWithCharacterDetails() {
        User user = persistUser();
        syllableRepository.save(Syllable.builder()
                .syllableChar("마")
                .mouthUrl("https://example.com/mouth/ma.png")
                .tongueUrl("https://example.com/tongue/ma.png")
                .build());

        EvaluationLog saved = persistenceService.saveResult(
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.RECOMMENDED)
                        .sentenceId(1L)
                        .originalText("맛있겠다.")
                        .standardPronunciation("마싯게따.")
                        .audioUrl("https://example.com/audio/evaluation.wav")
                        .result(result(
                                character(0, "마", 94),
                                character(1, "싯", 68)
                        ))
                        .build()
        );

        entityManager.clear();

        EvaluationLog found = evaluationLogRepository.findById(saved.getEvaluationLogIdx()).orElseThrow();
        assertThat(found.getUser().getUserIdx()).isEqualTo(user.getUserIdx());
        assertThat(found.getSource()).isEqualTo(EvaluationLog.PracticeSource.RECOMMENDED);
        assertThat(found.getSentenceId()).isEqualTo(1L);
        assertThat(found.getOriginalWord()).isEqualTo("맛있겠다.");
        assertThat(found.getStandardPronunciation()).isEqualTo("마싯게따.");
        assertThat(found.getRecognizedText()).isEqualTo("마싣게따");
        assertThat(found.getScore()).isEqualTo(82);
        assertThat(found.getAccuracyScore()).isEqualByComparingTo("84.00");
        assertThat(found.getFluencyScore()).isEqualByComparingTo("80.00");
        assertThat(found.getCompletenessScore()).isEqualByComparingTo("91.00");
        assertThat(found.getPronunciationScore()).isEqualByComparingTo("82.00");
        assertThat(found.getAudioUrl()).isEqualTo("https://example.com/audio/evaluation.wav");
        assertThat(found.getSyllableList())
                .extracting(EvaluationSyllable::getPositionNo)
                .containsExactly(0, 1);
        assertThat(found.getSyllableList().get(1).getFeedback())
                .isEqualTo("Keep the tongue closer for the sibilant sound");
        assertThat(syllableRepository.findById("싯")).isPresent();
    }

    @Test
    @DisplayName("단어 점수는 한 번만 저장하고 하위 음절은 nullable 점수와 단어 위치를 보존한다")
    void savesWordScoresAndGuideOnlySyllables() {
        User user = persistUser();
        PracticeWordResultResponse kimchi = PracticeWordResultResponse.builder()
                .position(0)
                .text("김치찌개")
                .score(82)
                .scoreStatus(ScoreStatus.AVAILABLE)
                .syllables(List.of(
                        guideOnlyCharacter(0, "김"),
                        guideOnlyCharacter(1, "치")
                ))
                .build();
        PracticeResultResponse result = PracticeResultResponse.builder()
                .overallScore(82)
                .gradeLabel("Good")
                .summary("Good pronunciation.")
                .recognizedText("김치찌개")
                .characterScoreStatus(ScoreStatus.UNAVAILABLE)
                .wordScoreStatus(ScoreStatus.AVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(84)
                        .fluency(80)
                        .completeness(91)
                        .build())
                .characters(List.of())
                .weakCharacters(List.of())
                .words(List.of(kimchi))
                .build();

        EvaluationLog saved = persistenceService.saveResult(
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.CUSTOM)
                        .originalText("김치찌개")
                        .standardPronunciation("김치찌개")
                        .result(result)
                        .build()
        );
        entityManager.clear();

        EvaluationLog found = evaluationLogRepository.findById(saved.getEvaluationLogIdx()).orElseThrow();
        assertThat(found.getWordList()).singleElement().satisfies(word -> {
            assertThat(word.getPositionNo()).isZero();
            assertThat(word.getWordText()).isEqualTo("김치찌개");
            assertThat(word.getScore()).isEqualTo(82);
        });
        assertThat(found.getSyllableList()).extracting(EvaluationSyllable::getScore).containsOnlyNulls();
        assertThat(found.getSyllableList()).extracting(EvaluationSyllable::getWordPosition).containsOnly(0);
    }

    @Test
    @DisplayName("단어 점수를 신뢰할 수 없으면 파생 가능한 단어 행을 저장하지 않는다")
    void skipsWordRowsWhenWordScoresAreUnavailable() {
        User user = persistUser();
        // 단어 경계를 판단하지 못한 결과는 문장 전체를 한 단어처럼 담아 내려온다.
        String sentence = "김치".repeat(60);
        PracticeWordResultResponse unreliableWord = PracticeWordResultResponse.builder()
                .position(0)
                .text(sentence)
                .score(null)
                .scoreStatus(ScoreStatus.UNAVAILABLE)
                .syllables(List.of(
                        guideOnlyCharacter(0, "김"),
                        guideOnlyCharacter(1, "치")
                ))
                .build();
        PracticeResultResponse result = PracticeResultResponse.builder()
                .overallScore(70)
                .gradeLabel("Fair")
                .summary("Keep practicing.")
                .recognizedText(sentence)
                .characterScoreStatus(ScoreStatus.UNAVAILABLE)
                .wordScoreStatus(ScoreStatus.UNAVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(70)
                        .fluency(70)
                        .completeness(70)
                        .build())
                .characters(List.of(
                        guideOnlyCharacter(0, "김"),
                        guideOnlyCharacter(1, "치")
                ))
                .weakCharacters(List.of())
                .words(List.of(unreliableWord))
                .build();

        EvaluationLog saved = persistenceService.saveResult(
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.CUSTOM)
                        .originalText("김치")
                        .standardPronunciation("김치")
                        .result(result)
                        .build()
        );
        entityManager.clear();

        EvaluationLog found = evaluationLogRepository.findById(saved.getEvaluationLogIdx()).orElseThrow();
        // word_text 길이를 넘기는 문장 blob이 저장되지 않고, 음절만 단어 위치 없이 남는다.
        assertThat(found.getWordList()).isEmpty();
        assertThat(found.getSyllableList())
                .extracting(EvaluationSyllable::getWordPosition)
                .containsOnlyNulls();
        assertThat(found.getSyllableList()).hasSize(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("글자별 결과 저장 실패 시 평가 log도 rollback된다")
    void rollsBackLogWhenCharacterResultFails() {
        User user = persistUserOutsideTransaction();

        EvaluationPersistenceService.SaveEvaluationResultCommand command =
                EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                        .user(user)
                        .source(EvaluationLog.PracticeSource.CUSTOM)
                        .originalText("가가")
                        .standardPronunciation("가가")
                        .result(result(
                                character(0, "가", 90),
                                character(0, "가", 70)
                        ))
                        .build();

        assertThatThrownBy(() -> persistenceService.saveResult(command))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(evaluationLogRepository.count()).isZero();
    }

    private User persistUser() {
        User user = User.builder()
                .socialId("social-id")
                .socialType(User.SocialType.GOOGLE)
                .build();
        entityManager.persist(user);
        return user;
    }

    private User persistUserOutsideTransaction() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = User.builder()
                    .socialId("rollback-user")
                    .socialType(User.SocialType.GOOGLE)
                    .build();
            entityManager.persist(user);
            entityManager.flush();
            entityManager.clear();
            return user;
        });
    }

    private PracticeResultResponse result(GuideCharacterResponse... characters) {
        return PracticeResultResponse.builder()
                .overallScore(82)
                .gradeLabel("Good")
                .summary("Good pronunciation.")
                .recognizedText("마싣게따")
                .characterScoreStatus(ScoreStatus.AVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(84)
                        .fluency(80)
                        .completeness(91)
                        .build())
                .characters(List.of(characters))
                .weakCharacters(List.of())
                .build();
    }

    private GuideCharacterResponse character(int position, String text, int score) {
        return GuideCharacterResponse.builder()
                .position(position)
                .text(text)
                .pronunciationText(text)
                .score(score)
                .scoreStatus(ScoreStatus.AVAILABLE)
                .guideType("TONGUE")
                .guideStatus(GuideStatus.AVAILABLE)
                .mouthGuideUrl("https://example.com/mouth/" + position + ".png")
                .tongueGuideUrl("https://example.com/tongue/" + position + ".png")
                .note("Keep the tongue closer for the sibilant sound")
                .build();
    }

    private GuideCharacterResponse guideOnlyCharacter(int position, String text) {
        return GuideCharacterResponse.builder()
                .position(position)
                .text(text)
                .pronunciationText(text)
                .score(null)
                .scoreStatus(ScoreStatus.UNAVAILABLE)
                .guideType("TONGUE")
                .guideStatus(GuideStatus.AVAILABLE)
                .tongueGuideUrl("https://example.com/tongue/" + position + ".png")
                .note("Open the guide")
                .build();
    }
}
