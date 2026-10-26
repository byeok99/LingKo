package com.lingko.lingko.core.domain.evaluation.service;

/** 외부 AI 공급자 호출 결과와 처리시간을 bounded provider·outcome으로 기록한다. */
public interface ExternalCallTelemetry {

    ExternalCallTelemetry NOOP = (provider, outcome, durationNanos) -> { };

    void record(String provider, String outcome, long durationNanos);
}
