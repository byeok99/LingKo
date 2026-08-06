package com.lingko.lingko.core.domain.evaluation.dto;

/**
 * 어절 하나에 대한 누적 연습 성적이다.
 *
 * 조회 계층이 평균을 다시 계산하지 않도록 집계 query 결과를 그대로 담는다.
 * {@code averageScore}는 JPA 집계 함수 결과라 Double이며, 표시용 반올림은 상위 계층이 맡는다.
 */
public record WeakWordAggregate(String wordText, Double averageScore, Long attemptCount) {
}
