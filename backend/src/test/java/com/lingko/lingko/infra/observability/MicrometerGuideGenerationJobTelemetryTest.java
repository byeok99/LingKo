package com.lingko.lingko.infra.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 가이드 생성 admission과 완료 결과가 비용 모니터링 지표로 남는 계약을 검증한다. */
class MicrometerGuideGenerationJobTelemetryTest {

    @Test
    void recordsBoundedRequestAndCompletionMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerGuideGenerationJobTelemetry telemetry = new MicrometerGuideGenerationJobTelemetry(registry);

        telemetry.request("accepted");
        telemetry.jobStarted();

        assertThat(registry.counter("lingko.guide.jobs.requests", "outcome", "accepted").count())
                .isEqualTo(1);
        assertThat(registry.get("lingko.guide.jobs.active").gauge().value()).isEqualTo(1);

        telemetry.jobFinished("completed");

        assertThat(registry.get("lingko.guide.jobs.active").gauge().value()).isZero();
        assertThat(registry.counter("lingko.guide.jobs.completed", "outcome", "completed").count())
                .isEqualTo(1);
    }
}
