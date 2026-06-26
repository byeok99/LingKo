package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.PracticeHistoryCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryItemResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeHistoryResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationSyllable;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;

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
                .characters(log.getSyllableList().stream()
                        .sorted(Comparator.comparing(EvaluationSyllable::getPositionNo))
                        .map(this::toCharacter)
                        .toList())
                .createdAt(log.getCreatedAt())
                .build();
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
