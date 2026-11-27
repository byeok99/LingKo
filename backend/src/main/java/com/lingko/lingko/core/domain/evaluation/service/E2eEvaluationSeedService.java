package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.api.evaluation.dto.PracticeWordResultResponse;
import com.lingko.lingko.api.evaluation.dto.ScoreStatus;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationLogRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationSyllableRepository;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationWordRepository;
import com.lingko.lingko.core.domain.sentence.entity.RecommendedSentence;
import com.lingko.lingko.core.domain.sentence.repository.RecommendedSentenceRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import com.lingko.lingko.core.util.PracticeSentenceNormalizer;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E2E 사전조건을 위해 "평가가 이미 끝난" 연습 기록을 만들고 지운다.
 *
 * <p>로컬 E2E는 S3·Azure 자격증명이 없어 실제 평가가 끝까지 돌지 않으므로, 기록 탭·상세·취약 음절·그래프처럼
 * 과거 기록을 읽는 화면을 검증하려면 결과 행을 직접 만들어야 한다. 저장은 운영 평가 완료와 같은
 * {@link EvaluationPersistenceService#saveResult}에 맡긴다. 그래야 "단어 점수를 신뢰할 수 없으면 단어 행을 남기지 않는다"
 * 같은 저장 규칙이 시드에도 그대로 적용되고, 시드 데이터가 실제 데이터와 어긋나지 않는다. 표준 발음과 음절 가이드도
 * 연습 화면과 같은 {@link EvaluationService} 변환을 쓴다.</p>
 *
 * <p>업로드·평가 작업·평가 기회는 건드리지 않는다. 기록 조회는 {@code evaluation_log}와 하위 행만 읽기 때문이다.
 * 운영 흐름에는 필요 없는 조작이라 {@code e2e} 프로필에서만 bean으로 등록한다.</p>
 */
@Profile("e2e")
@Service
@RequiredArgsConstructor
public class E2eEvaluationSeedService {

    private final UserRepository userRepository;
    private final RecommendedSentenceRepository recommendedSentenceRepository;
    private final EvaluationService evaluationService;
    private final EvaluationPersistenceService persistenceService;
    private final EvaluationLogRepository evaluationLogRepository;
    private final EvaluationWordRepository evaluationWordRepository;
    private final EvaluationSyllableRepository evaluationSyllableRepository;
    private final EntityManager entityManager;

    /**
     * 사용자의 연습 기록(log와 단어·음절 행)을 모두 지운다. 계정 탈퇴와 같은 child → parent 순서로 지운다.
     * 평가 작업과 평가 기회는 그대로 둔다.
     *
     * @return 지운 기록(log) 수
     */
    @Transactional
    public int clear(Long userId) {
        evaluationSyllableRepository.deleteAllByUserId(userId);
        evaluationWordRepository.deleteAllByUserId(userId);
        return evaluationLogRepository.deleteAllByUserId(userId);
    }

    /**
     * records를 오래된 것부터 최신 순으로 받아 기록을 만든다. 생성 시각은 마지막 기록을 지금으로 두고
     * 앞 기록일수록 1분씩 이르게 고친다. 같은 순간에 저장되면 최신순 정렬이 흔들리므로 순서를 시각으로 못 박는다.
     *
     * @return 만든 기록 id. records와 같은 순서다.
     */
    @Transactional
    public List<Long> seed(Long userId, List<SeedRecord> records) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("user not found: " + userId));
        List<Long> ids = new ArrayList<>();
        for (SeedRecord record : records) {
            ids.add(save(user, record).getEvaluationLogIdx());
        }
        // @CreationTimestamp와 같은 JVM 시계 기준으로 "지금"을 잡아야 앱이 보는 다른 기록과 시각 기준이 맞는다
        LocalDateTime newest = LocalDateTime.now();
        for (int i = 0; i < ids.size(); i++) {
            // created_at은 엔티티에서 updatable=false라 JPA 갱신으로는 바뀌지 않아 native update로 고친다
            entityManager.createNativeQuery(
                            "update evaluation_log set created_at = :createdAt where evaluation_log_idx = :id")
                    .setParameter("createdAt", newest.minusMinutes(ids.size() - 1 - i))
                    .setParameter("id", ids.get(i))
                    .executeUpdate();
        }
        // 같은 transaction에서 다시 읽는 호출자가 고친 시각을 보도록 1차 캐시를 비운다
        entityManager.clear();
        return ids;
    }

    private EvaluationLog save(User user, SeedRecord record) {
        boolean recommended = record.sentenceId() != null;
        String originalText = recommended
                ? recommendedSentenceRepository.findBySentenceIdAndActiveTrue(record.sentenceId())
                        .map(RecommendedSentence::getOriginalText)
                        .orElseThrow(() -> new IllegalArgumentException("active sentence not found: " + record.sentenceId()))
                : record.text();
        String normalized = PracticeSentenceNormalizer.normalize(originalText);
        // null이면 현재 규칙의 표준 발음이다. 값을 주면 "과거 규칙으로 저장된 snapshot" 같은 기록을 만들 수 있다
        String standardPronunciation = record.standardPronunciation() == null
                ? evaluationService.convertToStandardPronunciation(normalized)
                : record.standardPronunciation().trim();

        return persistenceService.saveResult(EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                .user(user)
                .source(recommended ? EvaluationLog.PracticeSource.RECOMMENDED : EvaluationLog.PracticeSource.CUSTOM)
                .sentenceId(record.sentenceId())
                .originalText(normalized)
                .standardPronunciation(standardPronunciation)
                .result(toResult(record, standardPronunciation))
                .build());
    }

    /**
     * 평가기 응답 대신 넘길 결과를 만든다. 단어는 표준 발음을 공백으로 나눈 어절이고, wordScores와 순서·개수가 같아야 한다.
     * 하나라도 null이면 단어 점수 전체를 신뢰할 수 없는 결과(UNAVAILABLE)로 보내, 저장 규칙에 따라 단어 행이 남지 않는다.
     */
    private PracticeResultResponse toResult(SeedRecord record, String standardPronunciation) {
        List<GuideCharacterResponse> characters = evaluationService.buildGuideCharacters(standardPronunciation);
        String[] wordTexts = standardPronunciation.split("\\s+");
        List<Integer> scores = record.wordScores();
        if (scores.size() != wordTexts.length) {
            throw new IllegalArgumentException("wordScores size " + scores.size()
                    + " must equal word count " + wordTexts.length + " of '" + standardPronunciation + "'");
        }
        boolean reliable = scores.stream().allMatch(score -> score != null);
        int wordCharacters = 0;
        for (String word : wordTexts) {
            wordCharacters += word.codePointCount(0, word.length());
        }
        if (wordCharacters != characters.size()) {
            // 문장부호처럼 가이드 변환이 지우는 글자가 섞이면 어절과 음절을 나눌 수 없다
            throw new IllegalArgumentException("standardPronunciation must contain only syllables and spaces: "
                    + standardPronunciation);
        }

        List<PracticeWordResultResponse> words = new ArrayList<>();
        int characterOffset = 0;
        for (int position = 0; position < wordTexts.length; position++) {
            int length = wordTexts[position].codePointCount(0, wordTexts[position].length());
            words.add(PracticeWordResultResponse.builder()
                    .position(position)
                    .text(wordTexts[position])
                    .score(scores.get(position))
                    .scoreStatus(ScoreStatus.ofNullableScore(scores.get(position)))
                    .syllables(characters.subList(characterOffset, characterOffset + length))
                    .build());
            characterOffset += length;
        }
        return PracticeResultResponse.builder()
                .overallScore(record.score())
                .wordScoreStatus(reliable ? ScoreStatus.AVAILABLE : ScoreStatus.UNAVAILABLE)
                .scoreBreakdown(PracticeResultResponse.ScoreBreakdownResponse.builder()
                        .accuracy(record.accuracy())
                        .fluency(record.fluency())
                        .completeness(record.completeness())
                        .build())
                .characters(characters)
                .words(words)
                .build();
    }

    /**
     * 시드할 기록 하나. sentenceId가 있으면 추천 문장 기록, 없으면 text로 만든 자유 문장 기록이다.
     *
     * @param standardPronunciation null이면 현재 규칙으로 계산한다. 값을 주면 그 snapshot을 그대로 저장한다.
     * @param wordScores 표준 발음 어절마다의 점수(0~100). null 원소는 "신뢰할 수 없는 점수"다.
     */
    public record SeedRecord(
            Long sentenceId,
            String text,
            String standardPronunciation,
            int score,
            int accuracy,
            int fluency,
            int completeness,
            List<Integer> wordScores
    ) {
    }
}
