package com.lingko.lingko.core.domain.evaluation.dto;

/**
 * 도메인 서비스가 사용하는 공급자 독립적인 Guide Generation Job Status 값을 전달한다.
 *
 * 업무 의미를 외부 API의 응답 형식과 분리하기 위해 내부 모델을 둔다.
 */
public enum GuideGenerationJobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
