package com.lingko.lingko.core.domain.evaluation.service;

/**
 * 가이드 생성 요청·실행 결과를 특정 metric 구현에 결합하지 않고 기록하는 계약이다.
 */
public interface GuideGenerationJobTelemetry {

    GuideGenerationJobTelemetry NOOP = new GuideGenerationJobTelemetry() { };

    default void request(String outcome) { }

    default void jobStarted() { }

    default void jobFinished(String outcome) { }
}
