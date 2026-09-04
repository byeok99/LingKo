package com.lingko.lingko.core.domain.evaluation.exception;

/**
 * 인증 사용자가 소유하지 않거나 존재하지 않는 평가 작업 조회를 동일하게 숨긴다.
 */
public class EvaluationJobNotFoundException extends RuntimeException {
    public EvaluationJobNotFoundException() {
        super("Evaluation job not found");
    }
}
