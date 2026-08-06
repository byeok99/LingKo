package com.lingko.lingko.core.domain.evaluation.dto;

/**
 * 어절 하나에 대한 누적 연습 성적이다.
 *
 * 취약 음절 집계의 입력으로 쓴다. 측정된 점수의 최소 단위는 어절이므로 DB에서는 어절까지만
 * 집계하고, 음절별 성적은 이 결과를 글자 단위로 다시 나눠 상위 계층에서 만든다.
 * {@code averageScore}는 JPA 집계 함수 결과라 Double이며, 표시용 반올림은 상위 계층이 맡는다.
 */
public record WordScoreAggregate(String wordText, Double averageScore, Long attemptCount) {
}
