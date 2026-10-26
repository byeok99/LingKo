package com.lingko.lingko.core.domain.evaluation.service;

/** 평가 Worker의 생존 여부와 bounded outcome 지표를 관측 계층에 전달한다. */
public interface EvaluationJobTelemetry {

    EvaluationJobTelemetry NOOP = new EvaluationJobTelemetry() {
        @Override
        public void heartbeat() {
        }

        @Override
        public void record(EvaluationJobExecutor.ExecutionResult result, long durationNanos) {
        }
    };

    /** DB polling loop가 예외 없이 한 번 순환했음을 기록한다. */
    void heartbeat();

    /** 한 실행 결과와 처리 시간을 고유 식별자 없이 기록한다. */
    void record(EvaluationJobExecutor.ExecutionResult result, long durationNanos);
}
