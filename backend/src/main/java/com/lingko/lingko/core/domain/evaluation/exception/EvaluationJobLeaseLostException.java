package com.lingko.lingko.core.domain.evaluation.exception;

/**
 * Worker가 처리하던 평가 작업의 lease를 잃었음을 나타낸다.
 *
 * <p>lease가 만료되면 다른 Worker가 같은 작업을 다시 claim해 시도 번호(attemptCount)가 증가한다.
 * 이전 Worker가 뒤늦게 결과·실패·phase를 기록하면 새 소유자의 상태를 덮어쓰거나 결과를 두 번 저장하므로,
 * 이 예외를 받은 Worker는 DB 상태와 S3 원본을 건드리지 않고 즉시 처리를 중단한다.</p>
 */
public class EvaluationJobLeaseLostException extends RuntimeException {

    public EvaluationJobLeaseLostException(String jobId) {
        super("evaluation job lease was taken over: jobId=" + jobId);
    }
}
