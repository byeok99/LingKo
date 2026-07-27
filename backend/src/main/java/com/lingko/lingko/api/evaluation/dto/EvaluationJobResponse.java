package com.lingko.lingko.api.evaluation.dto;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;

import java.time.Instant;

/**
 * 앱 Polling에 필요한 작업 상태와 완료 결과 또는 안정적인 실패 코드를 반환한다.
 */
public record EvaluationJobResponse(
        String jobId,
        EvaluationJob.Status status,
        PracticeResultResponse result,
        String errorCode,
        Instant createdAt,
        Instant updatedAt
) {
}
