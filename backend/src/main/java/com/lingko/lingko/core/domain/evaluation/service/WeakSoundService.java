package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.SoundDetailResponse;
import com.lingko.lingko.api.evaluation.dto.WeakSoundListResponse;
import com.lingko.lingko.api.evaluation.dto.WeakSoundResponse;
import com.lingko.lingko.core.domain.evaluation.dto.WordScoreAggregate;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 사용자가 반복해서 틀리는 음절을 골라 다음 연습 대상을 제안한다.
 *
 * <p><b>점수의 출처</b>: 음절 자체의 측정 점수는 존재하지 않는다. 공급자가 한국어 음절
 * 점수를 신뢰할 수 있게 제공하지 않아 {@code evaluation_syllable.score}는 항상 NULL이다.
 * 그래서 이 service는 측정된 최소 단위인 <b>어절 점수를 그 어절에 속한 음절들에 귀속</b>시켜
 * 음절별 평균을 만든다. 어절 하나만 보면 어느 음절이 문제인지 알 수 없지만, 같은 음절이
 * 서로 다른 여러 어절에서 반복해 낮게 나오면 그 소리가 원인일 가능성이 높다. 통계적 추정이며
 * 측정값이 아니라는 점을 응답 DTO와 화면 문구가 함께 지켜야 한다.
 *
 * <p>한 번만 틀린 음절까지 올리면 우연한 실수가 학습 목표로 승격되므로 최소 시도 횟수를 요구한다.
 */
@Service
@RequiredArgsConstructor
public class WeakSoundService {

    /**
     * 이 횟수 이상 연습한 음절만 취약 목록에 올린다.
     *
     * 2회를 고른 이유는 한 번의 실수와 반복되는 문제를 가르는 최소 경계이면서,
     * 이제 막 시작한 사용자에게도 목록이 비어 보이지 않는 값이기 때문이다.
     */
    private static final long MINIMUM_ATTEMPTS = 2;

    /**
     * 음절 집계의 입력으로 읽어올 서로 다른 어절 수의 상한이다.
     *
     * 점수가 낮은 어절부터 정렬해 가져오므로, 상한에 걸려 잘리는 것은 이미 잘하는 어절이다.
     * 취약 음절 산출에는 영향이 거의 없으면서 이력이 긴 사용자의 메모리 사용을 고정한다.
     */
    private static final int WORD_AGGREGATE_LIMIT = 500;

    /** Practiced·Suggested 목록의 기본 노출 개수다. 화면이 스크롤 없이 훑을 수 있는 범위로 잡았다. */
    private static final int DETAIL_LIST_SIZE = 10;

    private final EvaluationWordRepository evaluationWordRepository;
    private final RecommendedSentenceRepository recommendedSentenceRepository;

    /**
     * 취약 음절을 평균 점수가 낮은 순으로 반환한다.
     *
     * 집계를 DB가 아니라 Java에서 하는 이유는, 문자열을 글자 단위로 쪼개 group by 하는 일이
     * JPQL로 표현되지 않고 DB 함수에 기대면 벤더에 묶이기 때문이다. 입력이 사용자 한 명의
     * 서로 다른 어절 수(상한 {@value #WORD_AGGREGATE_LIMIT})라 메모리에서 처리해도 충분하다.
     */
    @Transactional(readOnly = true)
    public WeakSoundListResponse findWeakSounds(Long userId, int limit) {
        List<WordScoreAggregate> words = evaluationWordRepository.findScoredWordAggregates(
                userId,
                PageRequest.of(0, WORD_AGGREGATE_LIMIT)
        );

        Map<String, SoundAccumulator> byCharacter = new LinkedHashMap<>();
        for (WordScoreAggregate word : words) {
            if (word.averageScore() == null || word.attemptCount() == null) {
                continue;
            }
            for (String character : distinctHangulSyllables(word.wordText())) {
                byCharacter
                        .computeIfAbsent(character, key -> new SoundAccumulator())
                        .add(word.averageScore(), word.attemptCount());
            }
        }

        return new WeakSoundListResponse(
                byCharacter.entrySet().stream()
                        .filter(entry -> entry.getValue().attemptCount >= MINIMUM_ATTEMPTS)
                        .sorted(weakestFirst())
                        .limit(limit)
                        .map(entry -> new WeakSoundResponse(
                                entry.getKey(),
                                KoreanRomanizationUtil.romanizeSegment(entry.getKey()),
                                toDisplayScore(entry.getValue().averageScore()),
                                entry.getValue().attemptCount
                        ))
                        .toList()
        );
    }

    /**
     * 가장 약한 음절이 먼저 오도록 정렬한다.
     *
     * 평균이 같으면 더 많이 틀려본 음절을 앞에 둔다. 표본이 많을수록 추정이 덜 흔들리기 때문이다.
     * 그마저 같으면 글자 순으로 고정해, 같은 자료에서 매번 같은 순서가 나오게 한다.
     */
    private Comparator<Map.Entry<String, SoundAccumulator>> weakestFirst() {
        return Comparator
                .<Map.Entry<String, SoundAccumulator>>comparingDouble(
                        entry -> entry.getValue().averageScore()
                )
                .thenComparing(entry -> entry.getValue().attemptCount, Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey);
    }

    /**
     * 어절에서 서로 다른 한글 음절만 뽑는다.
     *
     * 같은 어절 안에 같은 글자가 두 번 나와도 한 번만 센다. 한 번의 연습은 그 음절에 대한
     * 시도 한 번이지, 글자가 반복된다고 두 배로 틀린 것이 아니기 때문이다.
     * 자모·구두점·공백은 발음 학습의 단위가 아니므로 제외한다.
     */
    private List<String> distinctHangulSyllables(String wordText) {
        if (wordText == null || wordText.isBlank()) {
            return List.of();
        }
        List<String> characters = new ArrayList<>();
        wordText.codePoints().forEach(codePoint -> {
            if (!KoreanRomanizationUtil.isHangulSyllable(codePoint)) {
                return;
            }
            String character = new String(Character.toChars(codePoint));
            if (!characters.contains(character)) {
                characters.add(character);
            }
        });
        return characters;
    }

    /** 화면이 평균을 다시 반올림하지 않도록 표시용 정수로 고정한다. */
    private int toDisplayScore(Double averageScore) {
        return averageScore == null
                ? 0
                : BigDecimal.valueOf(averageScore).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 음절 하나의 누적 성적과 과거 시도·다음 후보를 한 번에 반환한다.
     *
     * 아직 이 음절을 연습한 적이 없으면 평균과 횟수는 0이고 Practiced는 비어 있다.
     * 이때도 Suggested는 채워야 사용자가 진입 즉시 다음 행동을 고를 수 있다.
     */
    @Transactional(readOnly = true)
    public SoundDetailResponse findSoundDetail(Long userId, String character) {
        String normalized = character == null ? "" : character.trim();
        // 조회 대상이 한글 음절 한 글자가 아니면 아무것도 찾지 않는다.
        //
        // 이 값은 like 패턴 한가운데로 들어가므로, 거르지 않으면 '%'나 '_' 같은 wildcard가
        // 그대로 패턴이 되어 사용자의 모든 어절이 "이 음절의 연습 기록"으로 잡힌다.
        // 사용자 본인 자료로 범위가 제한돼 노출 문제는 아니지만 화면이 거짓을 말하게 된다.
        // 이스케이프 대신 형식 검증으로 막는 이유는, 음절이 아닌 입력에는 애초에
        // 보여줄 결과가 없어 빈 응답이 정확한 답이기 때문이다.
        List<EvaluationWord> practiced = isSingleHangulSyllable(normalized)
                ? evaluationWordRepository.findPracticedByCharacter(
                        userId,
                        normalized,
                        PageRequest.of(0, DETAIL_LIST_SIZE)
                )
                : List.of();

        List<Integer> scores = practiced.stream()
                .map(EvaluationWord::getScore)
                .filter(Objects::nonNull)
                .toList();

        return new SoundDetailResponse(
                normalized,
                KoreanRomanizationUtil.romanizeSegment(normalized),
                scores.isEmpty()
                        ? 0
                        : toDisplayScore(scores.stream().mapToInt(Integer::intValue).average().orElse(0)),
                scores.size(),
                practiced.stream().map(this::toPracticedAttempt).toList(),
                isSingleHangulSyllable(normalized) ? findSuggested(userId, normalized) : List.of()
        );
    }

    /**
     * 한글 완성형 음절 정확히 한 글자인지 판별한다.
     *
     * 자모(ㄱ), 여러 글자, 영문·기호는 이 화면의 조회 단위가 아니다.
     */
    private boolean isSingleHangulSyllable(String value) {
        return value.codePointCount(0, value.length()) == 1
                && KoreanRomanizationUtil.isHangulSyllable(value.codePointAt(0));
    }

    private List<SoundDetailResponse.SuggestedSentence> findSuggested(Long userId, String character) {
        return recommendedSentenceRepository
                .findUnpracticedByCharacter(userId, character, PageRequest.of(0, DETAIL_LIST_SIZE))
                .stream()
                .map(sentence -> {
                    // 추천 문장은 표준 발음을 저장하지 않아(V12) 조회 시점에 같은 규칙으로 변환한다.
                    // 평가 orchestrator를 통째로 끌어오지 않도록 변환 유틸을 직접 쓴다.
                    String standardPronunciation = KoreanPhonemeUtil.toPronunciation(
                            PracticeSentenceNormalizer.normalize(sentence.getOriginalText())
                    );
                    return new SoundDetailResponse.SuggestedSentence(
                            sentence.getSentenceId(),
                            sentence.getOriginalText(),
                            standardPronunciation,
                            KoreanRomanizationUtil.romanize(standardPronunciation),
                            sentence.getTranslation()
                    );
                })
                .toList();
    }

    private SoundDetailResponse.PracticedAttempt toPracticedAttempt(EvaluationWord word) {
        var log = word.getEvaluationLog();
        return new SoundDetailResponse.PracticedAttempt(
                log.getEvaluationLogIdx(),
                log.getOriginalWord(),
                log.getStandardPronunciation(),
                KoreanRomanizationUtil.romanize(log.getStandardPronunciation()),
                word.getScore(),
                log.getCreatedAt()
        );
    }

    /**
     * 음절 하나에 귀속된 어절 점수를 모으는 누산기다.
     *
     * 어절마다 시도 횟수가 다르므로 단순 평균이 아니라 횟수로 가중한다. 10번 연습한 어절과
     * 1번 연습한 어절을 같은 무게로 두면 표본이 적은 쪽이 평균을 끌고 간다.
     */
    private static final class SoundAccumulator {

        /** 어절 평균 × 시도 횟수의 합이다. 나눗셈을 마지막 한 번으로 미뤄 반올림 오차를 줄인다. */
        private double weightedScoreSum;

        /** 이 음절이 들어간 어절을 연습한 총 횟수다. */
        private long attemptCount;

        private void add(double wordAverageScore, long wordAttemptCount) {
            weightedScoreSum += wordAverageScore * wordAttemptCount;
            attemptCount += wordAttemptCount;
        }

        private double averageScore() {
            return attemptCount == 0 ? 0 : weightedScoreSum / attemptCount;
        }
    }
}
