package com.lingko.lingko.api.evaluation.dto;

/**
 * Home의 취약 어절 타일과 Word detail 화면 머리말에 쓰는 누적 성적이다.
 *
 * 음절이 아니라 어절 단위인 이유는 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.
 * {@code averageScore}는 반올림한 정수라 화면이 다시 계산하지 않는다.
 */
public record WeakWordResponse(
        String text,
        String romanization,
        int averageScore,
        long attemptCount
) {
}
