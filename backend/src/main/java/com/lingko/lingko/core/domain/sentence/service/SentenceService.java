package com.lingko.lingko.core.domain.sentence.service;

import com.lingko.lingko.api.sentence.dto.PracticeSentenceResponse;
import com.lingko.lingko.api.sentence.dto.RecommendedSentencesResponse;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Sentence 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
@RequiredArgsConstructor
public class SentenceService {

    private final RecommendedSentenceRepository repository;
    private final EvaluationService evaluationService;

    public RecommendedSentencesResponse findRecommendedSentences(int limit, String category) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<RecommendedSentence> sentences = isBlank(category)
                ? repository.findByActiveTrueOrderBySortOrderAscSentenceIdAsc(pageRequest)
                : repository.findByActiveTrueAndCategoryCodeOrderBySortOrderAscSentenceIdAsc(
                normalizeCategory(category),
                pageRequest
        );

        return new RecommendedSentencesResponse(sentences.stream()
                .map(this::toResponse)
                .toList());
    }

    public PracticeSentenceResponse getSentence(Long sentenceId) {
        RecommendedSentence sentence = repository.findBySentenceIdAndActiveTrue(sentenceId)
                .orElseThrow(() -> new SentenceNotFoundException(sentenceId));

        return toResponse(sentence);
    }

    private PracticeSentenceResponse toResponse(RecommendedSentence sentence) {
        return PracticeSentenceResponse.builder()
                .sentenceId(sentence.getSentenceId())
                .source("RECOMMENDED")
                .originalText(sentence.getOriginalText())
                .standardPronunciation(sentence.getStandardPronunciation())
                .translation(sentence.getTranslation())
                .categoryLabel(sentence.getCategoryLabel())
                .learningPoint(sentence.getLearningPoint())
                .initialScore(0)
                .characters(evaluationService.buildGuideCharacters(sentence.getStandardPronunciation()))
                .build();
    }

    private String normalizeCategory(String category) {
        return category.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
