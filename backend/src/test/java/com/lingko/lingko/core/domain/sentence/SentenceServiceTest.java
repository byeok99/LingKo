package com.lingko.lingko.core.domain.sentence;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.sentence.dto.RecommendedSentencesResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.sentence.service.SentenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sentence 서비스 Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
class SentenceServiceTest {

    private final RecommendedSentenceRepository repository = mock(RecommendedSentenceRepository.class);
    private final EvaluationService evaluationService = mock(EvaluationService.class);
    private final SentenceService service = new SentenceService(repository, evaluationService);

    @Test
    @DisplayName("active 추천 문장을 category와 limit으로 조회해 응답 DTO로 변환한다")
    void findRecommendedSentences() {
        RecommendedSentence sentence = recommendedSentence("FOOD");
        when(repository.findByActiveTrueAndCategoryCodeOrderBySortOrderAscSentenceIdAsc(
                "FOOD",
                PageRequest.of(0, 10)
        )).thenReturn(List.of(sentence));
        when(evaluationService.convertToStandardPronunciation("맛있겠다")).thenReturn("마싣껟따");
        when(evaluationService.buildGuideCharacters("마싣껟따")).thenReturn(List.of(
                GuideCharacterResponse.builder().position(0).text("마").pronunciationText("마").build()
        ));

        RecommendedSentencesResponse response = service.findRecommendedSentences(10, "food");

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).getSentenceId()).isEqualTo(1L);
        assertThat(response.items().get(0).getSource()).isEqualTo("RECOMMENDED");
        assertThat(response.items().get(0).getOriginalText()).isEqualTo("맛있겠다");
        assertThat(response.items().get(0).getStandardPronunciation()).isEqualTo("마싣껟따");
        assertThat(response.items().get(0).getRomanizedPronunciation()).isEqualTo("ma-sit-kket-tta");
        assertThat(response.items().get(0).getCategoryLabel()).isEqualTo("Food");
        assertThat(response.items().get(0).getCharacters()).hasSize(1);
    }

    @Test
    @DisplayName("category가 없으면 전체 active 추천 문장을 조회한다")
    void findRecommendedSentencesWithoutCategory() {
        when(repository.findByActiveTrueOrderBySortOrderAscSentenceIdAsc(PageRequest.of(0, 20)))
                .thenReturn(List.of());

        service.findRecommendedSentences(20, null);

        verify(repository).findByActiveTrueOrderBySortOrderAscSentenceIdAsc(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("단건 조회는 active 문장만 반환한다")
    void getSentence() {
        RecommendedSentence sentence = recommendedSentence("FOOD");
        when(repository.findBySentenceIdAndActiveTrue(1L)).thenReturn(Optional.of(sentence));
        when(evaluationService.convertToStandardPronunciation("맛있겠다")).thenReturn("마싣껟따");
        when(evaluationService.buildGuideCharacters("마싣껟따")).thenReturn(List.of());

        assertThat(service.getSentence(1L).getOriginalText()).isEqualTo("맛있겠다");
    }

    private RecommendedSentence recommendedSentence(String categoryCode) {
        return RecommendedSentence.builder()
                .sentenceId(1L)
                .originalText("맛있겠다.")
                .translation("It looks delicious.")
                .levelLabel("Beginner 2")
                .categoryCode(categoryCode)
                .categoryLabel("Food")
                .learningPoint("Final consonant linking and tense sound")
                .active(true)
                .sortOrder(1)
                .build();
    }
}
