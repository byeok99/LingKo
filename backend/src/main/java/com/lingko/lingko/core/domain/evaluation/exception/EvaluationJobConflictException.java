package com.lingko.lingko.core.domain.evaluation.exception;

/**
 * 동일 Idempotency Key가 다른 평가 요청에 재사용됐음을 나타낸다.
 */
public class EvaluationJobConflictException extends RuntimeException {
    public EvaluationJobConflictException() {
        super("Idempotency key was already used for a different evaluation request");
    }
}
