package com.lingko.lingko.core.domain.sentence;

import com.lingko.lingko.api.sentence.dto.SavedSentenceResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.sentence.repository.SavedSentenceRepository;
import com.lingko.lingko.core.domain.sentence.service.SavedSentenceService;
import com.lingko.lingko.core.domain.sentence.service.SentenceService;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.util.SyllableMappingUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 문장 저장 토글이 보장해야 하는 멱등성·소유자 격리·존재 검증을 확인한다.
 *
 * 저장 상태는 여러 화면(Home·Result·Saved)에서 같은 값을 보여줘야 하므로, 토글 결과가
 * 호출 순서나 클라이언트가 보낸 상태에 따라 달라지면 화면마다 다른 아이콘이 보이게 된다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saved_sentence;MODE=MySQL;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Import({SavedSentenceService.class, SentenceService.class, SavedSentenceServiceTest.TestBeans.class})
class SavedSentenceServiceTest {

    @TestConfiguration
    static class TestBeans {
        /** 표준 발음 변환은 이 테스트의 관심사가 아니라 실제 구현을 그대로 쓴다. */
        @Bean
        EvaluationService evaluationService() {
            return new EvaluationService(mock(SyllableMappingUtil.class));
        }
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecommendedSentenceRepository recommendedSentenceRepository;

    @Autowired
    private SavedSentenceRepository savedSentenceRepository;

    @Autowired
    private SavedSentenceService savedSentenceService;

    @Test
    @DisplayName("저장 토글은 서버의 실제 상태를 뒤집는다")
    void togglesFromServerState() {
        User user = persistUser("google-saved-1");
        Long sentenceId = persistSentence("내일 날씨가 어때요");

        assertThat(savedSentenceService.toggle(user.getUserIdx(), sentenceId).saved()).isTrue();
        assertThat(savedSentenceService.toggle(user.getUserIdx(), sentenceId).saved()).isFalse();
        assertThat(savedSentenceService.toggle(user.getUserIdx(), sentenceId).saved()).isTrue();
    }

    @Test
    @DisplayName("저장 목록은 최근 저장한 문장부터 반환하고 개수가 목록 길이와 일치한다")
    void listsRecentFirstWithMatchingCount() {
        User user = persistUser("google-saved-2");
        Long first = persistSentence("첫 문장이에요");
        Long second = persistSentence("두 번째 문장이에요");

        savedSentenceService.toggle(user.getUserIdx(), first);
        savedSentenceService.toggle(user.getUserIdx(), second);
        entityManager.flush();

        var response = savedSentenceService.findSaved(user.getUserIdx());

        // 화면 머리말의 개수가 목록과 어긋나면 사용자가 사라진 항목을 찾게 된다.
        assertThat(response.totalCount()).isEqualTo(response.items().size());
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).getSentenceId()).isEqualTo(second);
    }

    @Test
    @DisplayName("존재하지 않거나 비활성인 문장은 저장할 수 없다")
    void rejectsUnknownSentence() {
        User user = persistUser("google-saved-3");

        assertThatThrownBy(() -> savedSentenceService.toggle(user.getUserIdx(), 9999L))
                .isInstanceOf(SentenceNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 저장 목록은 보이지 않는다")
    void isolatesByUser() {
        User owner = persistUser("google-saved-4");
        User other = persistUser("google-saved-5");
        Long sentenceId = persistSentence("공유되지 않아요");

        savedSentenceService.toggle(owner.getUserIdx(), sentenceId);
        entityManager.flush();

        assertThat(savedSentenceService.findSaved(other.getUserIdx()).items()).isEmpty();
        assertThat(savedSentenceService.findSavedSentenceIds(other.getUserIdx())).isEmpty();
    }

    private Long persistSentence(String originalText) {
        RecommendedSentence sentence = recommendedSentenceRepository.save(RecommendedSentence.builder()
                .originalText(originalText)
                .translation("translation")
                .levelLabel("Beginner")
                .categoryCode("daily")
                .categoryLabel("Daily")
                .learningPoint("point")
                .sortOrder(1)
                .active(true)
                .build());
        return sentence.getSentenceId();
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
