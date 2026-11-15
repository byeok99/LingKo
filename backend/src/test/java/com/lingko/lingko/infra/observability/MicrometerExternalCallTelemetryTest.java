package com.lingko.lingko.infra.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 외부 호출 metric이 bounded provider·outcome만 사용하는지 검증한다. */
class MicrometerExternalCallTelemetryTest {

    @Test
    void recordsProviderOutcomeAndDuration() {
        // Given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerExternalCallTelemetry telemetry = new MicrometerExternalCallTelemetry(registry);

        // When
        telemetry.record("azure", "success", 2_000_000L);

        // Then
        assertThat(registry.get("lingko.external.requests")
                .tags("provider", "azure", "outcome", "success").counter().count()).isEqualTo(1);
        assertThat(registry.get("lingko.external.duration")
                .tags("provider", "azure", "outcome", "success").timer().count()).isEqualTo(1);
    }

    @Test
    void deniesMetricsOutsideTheCloudWatchAllowlist() {
        // Given
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(new ObservabilityConfig().cloudWatchMetricAllowlist());

        // When
        registry.counter("unbounded.user.metric", "userId", "7").increment();
        registry.counter("lingko.evaluation.executions", "outcome", "completed").increment();

        // Then
        assertThat(registry.find("unbounded.user.metric").counter()).isNull();
        assertThat(registry.get("lingko.evaluation.executions").counter().count()).isEqualTo(1);
    }
}
