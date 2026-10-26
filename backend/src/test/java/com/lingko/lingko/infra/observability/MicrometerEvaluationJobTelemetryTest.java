package com.lingko.lingko.infra.observability;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 평가 운영 지표가 고유 ID 없이 backlog와 bounded outcome만 노출하는지 검증한다. */
class MicrometerEvaluationJobTelemetryTest {

    @Test
    void recordsBacklogHeartbeatAndOutcome() {
        EvaluationJobRepository repository = mock(EvaluationJobRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Instant now = Instant.parse("2026-10-23T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        when(repository.countByStatus(EvaluationJob.Status.PENDING)).thenReturn(3L);
        when(repository.findOldestCreatedAtByStatus(EvaluationJob.Status.PENDING))
                .thenReturn(Optional.of(now.minusSeconds(90)));
        when(repository.countByStatusAndLeaseExpiresAtBefore(EvaluationJob.Status.PROCESSING, now))
                .thenReturn(2L);

        MicrometerEvaluationJobTelemetry telemetry =
                new MicrometerEvaluationJobTelemetry(repository, registry, clock);
        telemetry.refreshBacklog();
        telemetry.heartbeat();
        telemetry.record(EvaluationJobExecutor.ExecutionResult.TERMINAL_FAILURE, 1_000_000L);

        assertThat(registry.get("lingko.evaluation.pending").gauge().value()).isEqualTo(3);
        assertThat(registry.get("lingko.evaluation.oldest.pending.age.seconds").gauge().value()).isEqualTo(90);
        assertThat(registry.get("lingko.evaluation.expired.leases").gauge().value()).isEqualTo(2);
        assertThat(registry.get("lingko.evaluation.worker.heartbeat.age.seconds").gauge().value()).isZero();
        assertThat(registry.get("lingko.evaluation.executions")
                .tag("outcome", "terminal_failure").counter().count()).isEqualTo(1);
        assertThat(registry.get("lingko.evaluation.duration")
                .tag("outcome", "terminal_failure").timer().count()).isEqualTo(1);
    }
}
