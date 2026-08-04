package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryItemResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryWordResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluation History 업무 규칙을 조율한다.
 *
 * 컨트롤러와 외부 어댑터가 정책을 소유하지 않도록 도메인 서비스에 조율을 집중했다.
 */
@Service
@RequiredArgsConstructor
public class EvaluationHistoryService {

    private final EvaluationLogRepository evaluationLogRepository;

    @Transactional(readOnly = true)
    public PracticeHistoryResponse findHistory(Long userIdx, int page, int size) {
        if (userIdx == null || userIdx < 1) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        // 응답 크기와 child 엔티티 hydration 비용을 제한하기 위해 page size 상한을 둔다.
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size must be between 1 and 50");
        }

        Page<EvaluationLog> resultPage = evaluationLogRepository.findByUser_UserIdxOrderByCreatedAtDesc(
                userIdx,
                PageRequest.of(page, size)
        );

        return PracticeHistoryResponse.builder()
                .items(resultPage.getContent().stream()
                        .map(this::toItem)
                        .toList())
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalItems(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .hasNext(resultPage.hasNext())
                .bestScore(evaluationLogRepository.findBestScoreByUserIdx(userIdx))
                .build();
    }

    private PracticeHistoryItemResponse toItem(EvaluationLog log) {
        return PracticeHistoryItemResponse.builder()
                .evaluationLogId(log.getEvaluationLogIdx())
                .sentenceId(log.getSentenceId())
                .source(log.getSource().name())
                .originalText(log.getOriginalWord())
                .standardPronunciation(log.getStandardPronunciation())
                .recognizedText(log.getRecognizedText())
                .overallScore(log.getScore())
                .gradeLabel(resolveGradeLabel(log.getScore()))
                .summary(resolveSummary(log.getScore()))
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(toInt(log.getAccuracyScore()))
                        .fluency(toInt(log.getFluencyScore()))
                        .completeness(toInt(log.getCompletenessScore()))
                        .build())
                // 영속 순서는 표시 순서를 보장하지 않으므로 기록된 문자 위치로 다시 정렬한다.
                .characters(log.getSyllableList().stream()
                        .sorted(Comparator.comparing(EvaluationSyllable::getPositionNo))
                        .map(this::toCharacter)
                        .toList())
                .words(toWords(log))
                .createdAt(log.getCreatedAt())
                .build();
    }

    private List<PracticeHistoryWordResponse> toWords(EvaluationLog log) {
        List<EvaluationSyllable> syllables = log.getSyllableList().stream()
                .sorted(Comparator.comparing(EvaluationSyllable::getPositionNo))
                .toList();
        List<EvaluationWord> storedWords = log.getWordList().stream()
                .sorted(Comparator.comparing(EvaluationWord::getPositionNo))
                .toList();

        if (storedWords.isEmpty()) {
            return fallbackWords(log.getStandardPronunciation(), syllables);
        }

        return storedWords.stream()
                .map(word -> PracticeHistoryWordResponse.builder()
                        .position(word.getPositionNo())
                        .text(word.getWordText())
                        .score(word.getScore())
                        .scoreStatus(ScoreStatus.ofNullableScore(word.getScore()))
                        .syllables(syllables.stream()
                                .filter(syllable -> word.getPositionNo().equals(syllable.getWordPosition()))
                                .map(this::toGuideCharacter)
                                .toList())
                        .build())
                .toList();
    }

    /** 저장된 단어 snapshot이 없는 과거 기록도 공백·음절 순서로 가이드를 복원한다. */
    private List<PracticeHistoryWordResponse> fallbackWords(
            String standardPronunciation,
            List<EvaluationSyllable> syllables
    ) {
        if (standardPronunciation == null || standardPronunciation.isBlank()) {
            return List.of();
        }
        List<String> words = List.of(standardPronunciation.trim().split("\\s+"));
        int expectedSyllables = words.stream()
                .mapToInt(word -> (int) word.codePoints()
                        .mapToObj(Character::toString)
                        .filter(value -> !value.isBlank())
                        .count())
                .sum();
        if (expectedSyllables != syllables.size()) {
            return List.of(PracticeHistoryWordResponse.builder()
                    .position(0)
                    .text(standardPronunciation.trim())
                    .score(null)
                    .scoreStatus(ScoreStatus.UNAVAILABLE)
                    .syllables(syllables.stream().map(this::toGuideCharacter).toList())
                    .build());
        }

        java.util.ArrayList<PracticeHistoryWordResponse> result = new java.util.ArrayList<>();
        int offset = 0;
        for (int position = 0; position < words.size(); position++) {
            String word = words.get(position);
            int count = word.codePointCount(0, word.length());
            result.add(PracticeHistoryWordResponse.builder()
                    .position(position)
                    .text(word)
                    .score(null)
                    .scoreStatus(ScoreStatus.UNAVAILABLE)
                    .syllables(syllables.subList(offset, offset + count).stream()
                            .map(this::toGuideCharacter)
                            .toList())
                    .build());
            offset += count;
        }
        return List.copyOf(result);
    }

    private PracticeHistoryCharacterResponse toCharacter(EvaluationSyllable syllable) {
        return PracticeHistoryCharacterResponse.builder()
                .position(syllable.getPositionNo())
                .text(syllable.getSyllable().getSyllableChar())
                .score(syllable.getScore())
                .feedback(syllable.getFeedback())
                .mouthGuideUrl(syllable.getMouthGuideUrl())
                .tongueGuideUrl(syllable.getTongueGuideUrl())
                .build();
    }

    /** 단어 하위 음절에서는 과거에 저장된 숫자도 점수로 노출하지 않고 guide-only로 반환한다. */
    private PracticeHistoryCharacterResponse toGuideCharacter(EvaluationSyllable syllable) {
        return PracticeHistoryCharacterResponse.builder()
                .position(syllable.getPositionNo())
                .text(syllable.getSyllable().getSyllableChar())
                .score(null)
                .feedback(syllable.getFeedback())
                .mouthGuideUrl(syllable.getMouthGuideUrl())
                .tongueGuideUrl(syllable.getTongueGuideUrl())
                .build();
    }

    private int toInt(BigDecimal value) {
        return value == null ? 0 : value.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private String resolveGradeLabel(int score) {
        if (score >= 90) {
            return "Excellent";
        }
        if (score >= 75) {
            return "Good";
        }
        return "Needs work";
    }

    private String resolveSummary(int score) {
        if (score >= 90) {
            return "Clear pronunciation. Keep the same rhythm and articulation.";
        }
        if (score >= 75) {
            return "Good pronunciation. Review the highlighted sounds and try once more.";
        }
        return "Pronunciation needs more practice. Focus on the guide items before retrying.";
    }
}
