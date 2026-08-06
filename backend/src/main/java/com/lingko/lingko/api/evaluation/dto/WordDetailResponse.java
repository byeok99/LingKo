package com.lingko.lingko.api.evaluation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 어절 하나를 파고드는 화면(Word detail)이 한 번에 필요로 하는 자료를 묶는다.
 *
 * 화면이 누적 성적·과거 시도·다음 후보를 각각 조회하면 세 응답의 시점이 어긋나
 * "평균 62 / 8회"와 아래 목록의 개수가 맞지 않는 상태가 보일 수 있어 한 응답으로 제공한다.
 */
public record WordDetailResponse(
        String text,
        String romanization,
        int averageScore,
        long attemptCount,
        List<PracticedAttempt> practiced,
        List<SuggestedSentence> suggested
) {

    /** 이 어절을 연습했던 과거 시도 한 건이다. */
    public record PracticedAttempt(
            Long evaluationLogId,
            String originalText,
            String standardPronunciation,
            String romanization,
            Integer score,
            LocalDateTime createdAt
    ) {
    }

    /** 이 어절이 들어있지만 아직 연습하지 않은 추천 문장이다. */
    public record SuggestedSentence(
            Long sentenceId,
            String originalText,
            String standardPronunciation,
            String romanization,
            String translation
    ) {
    }
}
