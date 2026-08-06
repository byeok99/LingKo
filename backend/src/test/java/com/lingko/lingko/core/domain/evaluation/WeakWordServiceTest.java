package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.WeakWordListResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.WeakWordService;
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
 * 취약 어절 집계가 보장해야 하는 정렬·표본 조건·소유자 격리를 검증한다.
 *
 * 이 목록은 Home에서 다음 연습 대상을 제안하는 근거이므로, 우연한 실수가 학습 목표로
 * 올라오거나 다른 사용자의 기록이 섞이면 조용히 잘못된 학습을 유도하게 된다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:weak_words;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(WeakWordService.class)
class WeakWordServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Autowired
    private WeakWordService weakWordService;

    @Test
    @DisplayName("평균 점수가 낮은 어절부터 반환하고 로마자를 함께 제공한다")
    void returnsWeakestWordsFirst() {
        User user = persistUser("google-weak-1");
        persistWords(user, entry("날씨가", 58), entry("날씨가", 62));
        persistWords(user, entry("진짜요", 64), entry("진짜요", 70));
        persistWords(user, entry("오늘은", 90), entry("오늘은", 94));
        entityManager.flush();
        entityManager.clear();

        WeakWordListResponse response = weakWordService.findWeakWords(user.getUserIdx(), 3);

        assertThat(response.items()).extracting("text")
                .containsExactly("날씨가", "진짜요", "오늘은");
        assertThat(response.items().get(0).averageScore()).isEqualTo(60);
        assertThat(response.items().get(0).attemptCount()).isEqualTo(2);
        // 한글을 못 읽는 학습자가 대상이라 목록에도 로마자가 있어야 한다.
        assertThat(response.items().get(0).romanization()).isEqualTo("nal-ssi-ga");
    }

    @Test
    @DisplayName("한 번만 연습한 어절은 표본이 부족해 제외한다")
    void excludesWordsPracticedOnce() {
        User user = persistUser("google-weak-2");
        // 우연한 한 번의 실수가 학습 목표로 승격되면 안 된다.
        persistWords(user, entry("처음", 10));
        persistWords(user, entry("반복", 70), entry("반복", 74));
        entityManager.flush();
        entityManager.clear();

        WeakWordListResponse response = weakWordService.findWeakWords(user.getUserIdx(), 10);

        assertThat(response.items()).extracting("text").containsExactly("반복");
    }

    @Test
    @DisplayName("점수를 신뢰할 수 없어 저장하지 않은 어절은 집계에 넣지 않는다")
    void ignoresWordsWithoutScore() {
        User user = persistUser("google-weak-3");
        persistWords(user, entry("무점수", null), entry("무점수", null));
        entityManager.flush();
        entityManager.clear();

        assertThat(weakWordService.findWeakWords(user.getUserIdx(), 10).items()).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 기록은 섞이지 않는다")
    void isolatesByUser() {
        User owner = persistUser("google-weak-4");
        User other = persistUser("google-weak-5");
        persistWords(owner, entry("내것", 50), entry("내것", 54));
        persistWords(other, entry("남의것", 20), entry("남의것", 24));
        entityManager.flush();
        entityManager.clear();

        assertThat(weakWordService.findWeakWords(owner.getUserIdx(), 10).items())
                .extracting("text")
                .containsExactly("내것");
    }

    private record WordEntry(String text, Integer score) {
    }

    private WordEntry entry(String text, Integer score) {
        return new WordEntry(text, score);
    }

    private void persistWords(User user, WordEntry... entries) {
        EvaluationLog log = EvaluationLog.builder()
                .user(user)
                .originalWord("문장")
                .standardPronunciation("문장")
                .score(80)
                .pronunciationScore(BigDecimal.valueOf(80))
                .source(EvaluationLog.PracticeSource.CUSTOM)
                .build();
        for (int position = 0; position < entries.length; position++) {
            log.addWord(EvaluationWord.builder()
                    .positionNo(position)
                    .wordText(entries[position].text())
                    .score(entries[position].score())
                    .build());
        }
        evaluationLogRepository.save(log);
    }

    private User persistUser(String socialId) {
        User user = User.builder()
                .socialId(socialId)
                .socialType(User.SocialType.GOOGLE)
                .build();
        entityManager.persist(user);
        return user;
    }
}
