package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.Syllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.SyllableRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationHistoryService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 평가 기록 조회(#45)와 평가 결과 저장(#46)이 실행하는 JDBC statement 수를 측정한다.
 *
 * <p>두 경로는 lazy 연관을 항목마다 초기화해 응답 항목 수나 문자 수에 비례해 query가 늘어난다.
 * 응답 본문과 점수는 그대로이므로 기능 test로는 이 특성을 관찰할 수 없다. 그래서 이 test는
 * <strong>결과가 아니라 query 수</strong>를 측정해 출력한다.</p>
 *
 * <p><strong>현재는 측정 전용이다.</strong> 최적화 전후를 같은 조건에서 비교하기 위해 어느 쪽
 * 코드에서도 통과해야 하므로 query 수에 상한을 걸지 않고 기록만 한다. 측정값은 test 출력의
 * {@code [query-count]} 줄에서 확인한다.</p>
 *
 * <p>최적화를 적용한 뒤에는 아래 invariant를 단정으로 바꿔 회귀 guard로 쓴다. 정확한 숫자가
 * 아니라 상한과 증가폭으로 두어야 무해한 구현 변경에 깨지지 않는다.</p>
 * <ul>
 *   <li>기록 조회: statement 수가 page 항목 수에 비례해 늘지 않는다</li>
 *   <li>평가 저장: 문자당 증가량이 {@code evaluation_syllable} 행 insert 1건으로 수렴한다</li>
 * </ul>
 *
 * <p>측정은 Hibernate {@code Statistics}의 prepared statement 수를 사용하며, H2(MODE=MySQL)
 * 기준이라 절대 latency가 아니라 <strong>query 횟수의 증가 특성</strong>만 신뢰한다. 측정값과
 * 최적화 patch는 {@code docs/private/performance/results/}에 보관한다.</p>
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class EvaluationQueryCountBaselineTest {

    /** 기록 조회 측정에 사용하는 page 크기. 운영 앱의 첫 페이지와 같은 조건이다. */
    private static final int PAGE_SIZE = 20;

    /** 한 평가 기록에 저장하는 음절 수. 2단어 × 5음절로 단어 경로까지 함께 태운다. */
    private static final int SYLLABLES_PER_LOG = 10;

    /**
     * 음절 문자 pool.
     *
     * <p>기록마다 서로 다른 문자를 쓰기 위해 pool에서 겹치지 않게 잘라 쓴다. 같은 문자가
     * 반복되면 Hibernate가 session 단위로 {@code Syllable}을 한 번만 적재해 N+1이 과소
     * 측정되므로, 측정에서는 문자 재사용을 의도적으로 피한다.</p>
     */
    private static final String SYLLABLE_POOL =
            "가각간갇갈감갑강개객거건걸검겁것게겨격견결겸경계고곡곤골곰곱공과관광교구국군굴굼굽궁귀규균그극근글금급긍기긴길김깁깅나낙난날남납낭내냉너널넘넣네녀년념녕노녹논놀놈농뇌뇨누눈눌느늑는늘능니닉닌달담답당대댁더덕던덜덤덥데도독돈돌동되된두둑둘둥드득든들등디딘딜딤딥";

    @Autowired
    private EvaluationHistoryService historyService;
    @Autowired
    private EvaluationPersistenceService persistenceService;
    @Autowired
    private EvaluationLogRepository evaluationLogRepository;
    @Autowired
    private SyllableRepository syllableRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanDatabase() {
        evaluationLogRepository.deleteAll();
        syllableRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("기록 첫 페이지의 page 크기별 query 수를 측정한다")
    void measuresHistoryPageStatementCount() {
        // Given
        User user = createUser("history-query-count-user");
        seedHistory(user, PAGE_SIZE);

        // When
        long smallPageStatements = measureStatements(
                () -> historyService.findHistory(user.getUserIdx(), 0, 5));
        long fullPageStatements = measureStatements(
                () -> historyService.findHistory(user.getUserIdx(), 0, PAGE_SIZE));
        report("history size=5", smallPageStatements);
        report("history size=" + PAGE_SIZE, fullPageStatements);

        // Then
        // 최적화 적용 후 단정으로 바꿀 invariant:
        //   assertThat(fullPageStatements).isLessThan(smallPageStatements * 2);
        //   assertThat(fullPageStatements).isLessThanOrEqualTo(15);
        // 지금은 측정만 하므로 query가 실제로 실행됐는지만 확인한다.
        assertThat(smallPageStatements).isPositive();
        assertThat(fullPageStatements).isGreaterThanOrEqualTo(smallPageStatements);
    }

    @Test
    @DisplayName("기록 조회는 응답 항목과 음절을 모두 채운 상태로 측정된다")
    void historyPageReturnsFullyHydratedItems() {
        // Given
        User user = createUser("history-contract-user");
        seedHistory(user, 3);

        // When
        PracticeHistoryResponse response = historyService.findHistory(user.getUserIdx(), 0, PAGE_SIZE);

        // Then: query 수를 줄이는 변경이 응답 축소로 이어지지 않았는지 같은 test class에서 함께 지킨다.
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems())
                .allSatisfy(item -> {
                    assertThat(item.getCharacters()).hasSize(SYLLABLES_PER_LOG);
                    assertThat(item.getWords()).hasSize(2);
                    assertThat(item.getCharacters())
                            .allSatisfy(character -> assertThat(character.getText()).isNotBlank());
                });
    }

    @Test
    @DisplayName("평가 저장의 문자 수별 query 수를 측정한다")
    void measuresSaveStatementCount() {
        // Given
        User user = createUser("save-query-count-user");
        // 음절 metadata가 이미 등록된 정상 운영 상태를 측정한다. 새 문자의 최초 insert는
        // 문자 집합이 한 번 수렴하면 사라지는 비용이므로 조회 비용과 섞지 않는다.
        warmUpSyllables(0, 30);

        // When
        long tenCharacterStatements = measureStatements(
                () -> persistenceService.saveResult(command(user, 0, 10)));
        long twentyCharacterStatements = measureStatements(
                () -> persistenceService.saveResult(command(user, 10, 20)));
        report("save 10 characters", tenCharacterStatements);
        report("save 20 characters", twentyCharacterStatements);

        // Then
        // 최적화 적용 후 단정으로 바꿀 invariant: 문자가 10개 늘면 evaluation_syllable 행
        // insert 10건만 늘어야 한다. 문자마다 조회가 따로 나가면 증가폭이 두 배로 벌어진다.
        //   assertThat(twentyCharacterStatements - tenCharacterStatements).isLessThanOrEqualTo(14);
        assertThat(tenCharacterStatements).isPositive();
        assertThat(twentyCharacterStatements).isGreaterThan(tenCharacterStatements);
    }

    @Test
    @DisplayName("처음 등장한 음절 문자는 저장되고 가이드 URL을 보존한다")
    void saveCreatesMissingSyllableMetadata() {
        // Given: 음절 metadata가 하나도 등록되지 않은 상태
        User user = createUser("save-new-syllable-user");

        // When
        persistenceService.saveResult(command(user, 0, 10));

        // Then: 조회를 batch로 묶는 변경이 "없는 문자는 새로 만든다"는 기존 계약을 깨지 않았는지 확인한다.
        assertThat(syllableRepository.findAllById(expectedSyllables(0, 10)))
                .as("저장 결과에 등장한 음절 문자 10개가 모두 등록되어야 한다")
                .hasSize(10);
    }

    /**
     * 측정값을 test 출력에 남긴다.
     *
     * <p>통과한 실행에서도 숫자가 보여야 개선 전후 비교와 결과 보관이 가능하므로, 단정 메시지에만
     * 의존하지 않고 항상 기록한다.</p>
     */
    private void report(String label, long statements) {
        System.out.printf("[query-count] %s = %d statements%n", label, statements);
    }

    /**
     * 주어진 작업을 실행하며 발생한 prepared statement 수를 센다.
     *
     * <p>호출자가 transaction을 열지 않으므로 서비스의 {@code @Transactional}이 운영과 같은
     * 단위로 session을 만든다. test transaction 안에서 측정하면 1차 cache가 남아 N+1이
     * 가려지므로 일부러 밖에서 호출한다.</p>
     */
    private long measureStatements(Runnable action) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    private User createUser(String socialId) {
        return userRepository.save(User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build());
    }

    /** 기록마다 겹치지 않는 문자를 사용해 연관 적재 횟수를 과소 측정하지 않는다. */
    private void seedHistory(User user, int logCount) {
        for (int index = 0; index < logCount; index++) {
            persistenceService.saveResult(command(
                    user,
                    index * SYLLABLES_PER_LOG,
                    SYLLABLES_PER_LOG
            ));
        }
    }

    /**
     * 단어 점수가 신뢰 가능한 평가 결과를 만든다.
     *
     * @param poolOffset      음절 pool 시작 위치. 호출마다 다른 문자를 쓰기 위한 값이다
     * @param characterCount  저장할 음절 수. 2단어로 균등 분할한다
     */
    private EvaluationPersistenceService.SaveEvaluationResultCommand command(
            User user,
            int poolOffset,
            int characterCount
    ) {
        int wordCount = 2;
        int perWord = characterCount / wordCount;
        List<PracticeWordResultResponse> words = new ArrayList<>();
        List<GuideCharacterResponse> allCharacters = new ArrayList<>();
        StringBuilder standardPronunciation = new StringBuilder();

        for (int wordPosition = 0; wordPosition < wordCount; wordPosition++) {
            List<GuideCharacterResponse> syllables = new ArrayList<>();
            StringBuilder wordText = new StringBuilder();
            for (int offset = 0; offset < perWord; offset++) {
                int position = wordPosition * perWord + offset;
                String text = syllableAt(poolOffset + position);
                GuideCharacterResponse character = GuideCharacterResponse.builder()
                        .position(position)
                        .text(text)
                        .score(80)
                        .scoreStatus(ScoreStatus.AVAILABLE)
                        .build();
                syllables.add(character);
                allCharacters.add(character);
                wordText.append(text);
            }
            if (wordPosition > 0) {
                standardPronunciation.append(' ');
            }
            standardPronunciation.append(wordText);
            words.add(PracticeWordResultResponse.builder()
                    .position(wordPosition)
                    .text(wordText.toString())
                    .score(80)
                    .scoreStatus(ScoreStatus.AVAILABLE)
                    .syllables(syllables)
                    .build());
        }

        return EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                .user(user)
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .originalText(standardPronunciation.toString())
                .standardPronunciation(standardPronunciation.toString())
                .result(PracticeResultResponse.builder()
                        .overallScore(85)
                        .characterScoreStatus(ScoreStatus.AVAILABLE)
                        .wordScoreStatus(ScoreStatus.AVAILABLE)
                        .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                                .accuracy(85)
                                .fluency(85)
                                .completeness(85)
                                .build())
                        .characters(allCharacters)
                        .weakCharacters(List.of())
                        .words(words)
                        .build())
                .build();
    }

    /** pool을 순환 참조해 필요한 만큼 음절 문자를 만든다. */
    private String syllableAt(int index) {
        return String.valueOf(SYLLABLE_POOL.charAt(index % SYLLABLE_POOL.length()));
    }

    /** 측정 구간이 음절 조회 비용만 포함하도록 사용할 문자를 미리 등록한다. */
    private void warmUpSyllables(int poolOffset, int count) {
        syllableRepository.saveAll(expectedSyllables(poolOffset, count).stream()
                .map(text -> Syllable.builder().syllableChar(text).build())
                .toList());
    }

    private List<String> expectedSyllables(int poolOffset, int count) {
        List<String> texts = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            texts.add(syllableAt(poolOffset + index));
        }
        return texts;
    }
}
