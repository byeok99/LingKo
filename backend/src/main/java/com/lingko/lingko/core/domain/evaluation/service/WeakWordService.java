package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.WeakWordListResponse;
import com.lingko.lingko.api.evaluation.dto.WeakWordResponse;
import com.lingko.lingko.api.evaluation.dto.WordDetailResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationWord;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationWordRepository;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.util.KoreanPhonemeUtil;
import com.lingko.lingko.core.util.KoreanRomanizationUtil;
import com.lingko.lingko.core.util.PracticeSentenceNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 사용자가 반복해서 틀리는 어절을 골라 다음 연습 대상을 제안한다.
 *
 * 한 번만 틀린 어절까지 올리면 우연한 실수가 학습 목표로 승격되므로 최소 시도 횟수를 요구한다.
 */
@Service
@RequiredArgsConstructor
public class WeakWordService {

    /**
     * 이 횟수 이상 연습한 어절만 취약 목록에 올린다.
     *
     * 2회를 고른 이유는 한 번의 실수와 반복되는 문제를 가르는 최소 경계이면서,
     * 이제 막 시작한 사용자에게도 목록이 비어 보이지 않는 값이기 때문이다.
     */
    private static final long MINIMUM_ATTEMPTS = 2;

    /** Practiced·Suggested 목록의 기본 노출 개수다. 화면이 스크롤 없이 훑을 수 있는 범위로 잡았다. */
    private static final int DETAIL_LIST_SIZE = 10;

    private final EvaluationWordRepository evaluationWordRepository;
    private final RecommendedSentenceRepository recommendedSentenceRepository;

    @Transactional(readOnly = true)
    public WeakWordListResponse findWeakWords(Long userId, int limit) {
        return new WeakWordListResponse(
                evaluationWordRepository
                        .findWeakWords(userId, MINIMUM_ATTEMPTS, PageRequest.of(0, limit))
                        .stream()
                        .map(aggregate -> new WeakWordResponse(
                                aggregate.wordText(),
                                KoreanRomanizationUtil.romanizeSegment(aggregate.wordText()),
                                toDisplayScore(aggregate.averageScore()),
                                aggregate.attemptCount()
                        ))
                        .toList()
        );
    }

    /** 화면이 평균을 다시 반올림하지 않도록 표시용 정수로 고정한다. */
    private int toDisplayScore(Double averageScore) {
        return averageScore == null
                ? 0
                : BigDecimal.valueOf(averageScore).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 어절 하나의 누적 성적과 과거 시도·다음 후보를 한 번에 반환한다.
     *
     * 아직 이 어절을 연습한 적이 없으면 평균과 횟수는 0이고 Practiced는 비어 있다.
     * 이때도 Suggested는 채워야 사용자가 진입 즉시 다음 행동을 고를 수 있다.
     */
    @Transactional(readOnly = true)
    public WordDetailResponse findWordDetail(Long userId, String wordText) {
        String normalized = wordText == null ? "" : wordText.trim();
        List<EvaluationWord> practiced = normalized.isEmpty()
                ? List.of()
                : evaluationWordRepository.findPracticedByWord(
                        userId,
                        normalized,
                        PageRequest.of(0, DETAIL_LIST_SIZE)
                );

        List<Integer> scores = practiced.stream()
                .map(EvaluationWord::getScore)
                .filter(Objects::nonNull)
                .toList();

        return new WordDetailResponse(
                normalized,
                KoreanRomanizationUtil.romanizeSegment(normalized),
                scores.isEmpty()
                        ? 0
                        : toDisplayScore(scores.stream().mapToInt(Integer::intValue).average().orElse(0)),
                scores.size(),
                practiced.stream().map(this::toPracticedAttempt).toList(),
                normalized.isEmpty() ? List.of() : findSuggested(userId, normalized)
        );
    }

    private List<WordDetailResponse.SuggestedSentence> findSuggested(Long userId, String wordText) {
        return recommendedSentenceRepository
                .findUnpracticedByWord(userId, wordText, PageRequest.of(0, DETAIL_LIST_SIZE))
                .stream()
                .map(sentence -> {
                    // 추천 문장은 표준 발음을 저장하지 않아(V12) 조회 시점에 같은 규칙으로 변환한다.
                    // 평가 orchestrator를 통째로 끌어오지 않도록 변환 유틸을 직접 쓴다.
                    String standardPronunciation = KoreanPhonemeUtil.toPronunciation(
                            PracticeSentenceNormalizer.normalize(sentence.getOriginalText())
                    );
                    return new WordDetailResponse.SuggestedSentence(
                            sentence.getSentenceId(),
                            sentence.getOriginalText(),
                            standardPronunciation,
                            KoreanRomanizationUtil.romanize(standardPronunciation),
                            sentence.getTranslation()
                    );
                })
                .toList();
    }

    private WordDetailResponse.PracticedAttempt toPracticedAttempt(EvaluationWord word) {
        var log = word.getEvaluationLog();
        return new WordDetailResponse.PracticedAttempt(
                log.getEvaluationLogIdx(),
                log.getOriginalWord(),
                log.getStandardPronunciation(),
                KoreanRomanizationUtil.romanize(log.getStandardPronunciation()),
                word.getScore(),
                log.getCreatedAt()
        );
    }
}
