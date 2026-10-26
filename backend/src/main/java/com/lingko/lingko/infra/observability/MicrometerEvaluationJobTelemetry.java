package com.lingko.lingko.infra.observability;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobTelemetry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Worker backlog·heartbeat·실행 결과를 개인정보 없는 저카디널리티 지표로 제공한다. */
@Component
@ConditionalOnProperty(name = "evaluation.worker.enabled", havingValue = "true", matchIfMissing = true)
public class MicrometerEvaluationJobTelemetry implements EvaluationJobTelemetry {

    private final EvaluationJobRepository repository;
    private final MeterRegistry registry;
    private final Clock clock;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong oldestPendingAgeSeconds = new AtomicLong();
    private final AtomicLong expiredLeases = new AtomicLong();
    private final AtomicLong lastHeartbeatEpochSeconds = new AtomicLong();

    /** repository snapshot과 MeterRegistry를 연결하고 고정된 gauge 집합을 한 번 등록한다. */
    public MicrometerEvaluationJobTelemetry(EvaluationJobRepository repository, MeterRegistry registry) {
        this(repository, registry, Clock.systemUTC());
    }

    MicrometerEvaluationJobTelemetry(EvaluationJobRepository repository, MeterRegistry registry, Clock clock) {
        this.repository = repository;
        this.registry = registry;
        this.clock = clock;
        registerGauge("lingko.evaluation.pending", pending, "Pending evaluation jobs");
        registerGauge("lingko.evaluation.oldest.pending.age.seconds", oldestPendingAgeSeconds,
                "Age of the oldest pending evaluation job");
        registerGauge("lingko.evaluation.expired.leases", expiredLeases, "Expired processing leases");
        Gauge.builder("lingko.evaluation.worker.heartbeat.age.seconds", this, telemetry -> telemetry.heartbeatAge())
                .description("Seconds since the worker polling loop last completed")
                .register(registry);
    }

    /** DB 부하를 제한하기 위해 backlog snapshot은 60초에 한 번만 갱신한다. */
    @Scheduled(fixedDelayString = "${observability.evaluation.snapshot-delay-ms:60000}")
    public void refreshBacklog() {
        Instant now = clock.instant();
        pending.set(repository.countByStatus(EvaluationJob.Status.PENDING));
        oldestPendingAgeSeconds.set(repository.findOldestCreatedAtByStatus(EvaluationJob.Status.PENDING)
                .map(createdAt -> Math.max(0, Duration.between(createdAt, now).toSeconds()))
                .orElse(0L));
        expiredLeases.set(repository.countByStatusAndLeaseExpiresAtBefore(EvaluationJob.Status.PROCESSING, now));
    }

    @Override
    public void heartbeat() {
        lastHeartbeatEpochSeconds.set(clock.instant().getEpochSecond());
    }

    @Override
    public void record(EvaluationJobExecutor.ExecutionResult result, long durationNanos) {
        String outcome = result.name().toLowerCase(Locale.ROOT);
        registry.counter("lingko.evaluation.executions", "outcome", outcome).increment();
        Timer.builder("lingko.evaluation.duration")
                .tag("outcome", outcome)
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private void registerGauge(String name, AtomicLong value, String description) {
        Gauge.builder(name, value, AtomicLong::get).description(description).register(registry);
    }

    private long heartbeatAge() {
        long heartbeat = lastHeartbeatEpochSeconds.get();
        return heartbeat == 0 ? -1 : Math.max(0, clock.instant().getEpochSecond() - heartbeat);
    }
}
