package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.WeakSoundListResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.service.WeakSoundService;
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
 * 취약 음절 집계가 보장해야 하는 귀속 규칙·가중치·표본 조건·소유자 격리를 검증한다.
 *
 * 이 목록은 Home에서 다음 연습 대상을 제안하는 근거다. 음절 점수는 측정값이 아니라
 * 어절 점수를 귀속시켜 만든 추정이므로, 귀속과 가중이 틀리면 사용자는 멀쩡한 소리를
 * 약점으로 알고 연습하게 된다. 우연한 실수가 학습 목표로 올라오거나 다른 사용자의
 * 기록이 섞이는 경우도 같은 종류의 조용한 실패다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:weak_sounds;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import(WeakSoundService.class)
class WeakSoundServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EvaluationLogRepository evaluationLogRepository;

    @Autowired
    private WeakSoundService weakSoundService;

    @Test
    @DisplayName("평균 점수가 낮은 음절부터 반환하고 로마자를 함께 제공한다")
    void returnsWeakestSoundsFirst() {
        User user = persistUser("google-weak-1");
        persistWords(user, entry("나비", 58), entry("나비", 62));
        persistWords(user, entry("오늘", 90), entry("오늘", 94));
        entityManager.flush();
        entityManager.clear();

        WeakSoundListResponse response = weakSoundService.findWeakSounds(user.getUserIdx(), 10);

        // 어절 점수가 그 어절의 모든 음절에 귀속된다. 평균이 같으면 글자 순으로 고정한다.
        assertThat(response.items()).extracting("text")
                .containsExactly("나", "비", "늘", "오");
        assertThat(response.items().get(0).averageScore()).isEqualTo(60);
        assertThat(response.items().get(0).attemptCount()).isEqualTo(2);
        // 한글을 못 읽는 학습자가 대상이라 목록에도 로마자가 있어야 한다.
        assertThat(response.items().get(0).romanization()).isEqualTo("na");
    }

    @Test
    @DisplayName("여러 어절에 걸친 같은 음절을 시도 횟수로 가중해 하나로 모은다")
    void aggregatesOneSoundAcrossWordsWeightedByAttempts() {
        User user = persistUser("google-weak-6");
        // 시: 3회 평균 50인 어절과 1회 90인 어절에 걸쳐 있다.
        persistWords(user, entry("시장", 40), entry("시장", 50), entry("시장", 60));
        persistWords(user, entry("시간", 90));
        entityManager.flush();
        entityManager.clear();

        WeakSoundListResponse response = weakSoundService.findWeakSounds(user.getUserIdx(), 10);

        // 어절 평균을 단순 평균하면 (50+90)/2 = 70이 된다. 표본이 적은 어절이 평균을
        // 끌고 가지 않도록 시도 횟수로 가중해 (50*3 + 90*1)/4 = 60이어야 한다.
        assertThat(response.items())
                .filteredOn(item -> item.text().equals("시"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.averageScore()).isEqualTo(60);
                    assertThat(item.attemptCount()).isEqualTo(4);
                });
    }

    @Test
    @DisplayName("한 어절 안에서 같은 음절이 반복돼도 시도는 한 번으로 센다")
    void countsRepeatedSoundInOneWordOnce() {
        User user = persistUser("google-weak-7");
        persistWords(user, entry("가가", 50), entry("가가", 54));
        entityManager.flush();
        entityManager.clear();

        WeakSoundListResponse response = weakSoundService.findWeakSounds(user.getUserIdx(), 10);

        // 글자가 두 번 나왔다고 두 배로 틀린 것이 아니다. 연습 2회는 시도 2회다.
        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.attemptCount()).isEqualTo(2));
    }

    @Test
    @DisplayName("한글 음절이 아닌 문자는 학습 단위가 아니라 집계에서 뺀다")
    void ignoresNonHangulCharacters() {
        User user = persistUser("google-weak-8");
        persistWords(user, entry("A나?", 50), entry("A나?", 54));
        entityManager.flush();
        entityManager.clear();

        assertThat(weakSoundService.findWeakSounds(user.getUserIdx(), 10).items())
                .extracting("text")
                .containsExactly("나");
    }

    @Test
    @DisplayName("한 번만 연습한 음절은 표본이 부족해 제외한다")
    void excludesSoundsPracticedOnce() {
        User user = persistUser("google-weak-2");
        // 우연한 한 번의 실수가 학습 목표로 승격되면 안 된다.
        persistWords(user, entry("처음", 10));
        persistWords(user, entry("반복", 70), entry("반복", 74));
        entityManager.flush();
        entityManager.clear();

        WeakSoundListResponse response = weakSoundService.findWeakSounds(user.getUserIdx(), 10);

        assertThat(response.items()).extracting("text").containsExactly("반", "복");
    }

    @Test
    @DisplayName("점수를 신뢰할 수 없어 저장하지 않은 어절은 집계에 넣지 않는다")
    void ignoresWordsWithoutScore() {
        User user = persistUser("google-weak-3");
        persistWords(user, entry("무점수", null), entry("무점수", null));
        entityManager.flush();
        entityManager.clear();

        assertThat(weakSoundService.findWeakSounds(user.getUserIdx(), 10).items()).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 기록은 섞이지 않는다")
    void isolatesByUser() {
        User owner = persistUser("google-weak-4");
        User other = persistUser("google-weak-5");
        persistWords(owner, entry("내것", 50), entry("내것", 54));
        persistWords(other, entry("남의", 20), entry("남의", 24));
        entityManager.flush();
        entityManager.clear();

        assertThat(weakSoundService.findWeakSounds(owner.getUserIdx(), 10).items())
                .extracting("text")
                .containsExactly("것", "내");
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
