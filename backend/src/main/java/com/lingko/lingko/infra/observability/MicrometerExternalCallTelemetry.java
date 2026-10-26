package com.lingko.lingko.infra.observability;

import com.lingko.lingko.core.domain.evaluation.service.ExternalCallTelemetry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Azure·Replicate 호출을 URL·사용자·작업 식별자 없이 집계한다. */
@Component
@RequiredArgsConstructor
public class MicrometerExternalCallTelemetry implements ExternalCallTelemetry {

    private final MeterRegistry registry;

    @Override
    public void record(String provider, String outcome, long durationNanos) {
        registry.counter("lingko.external.requests", "provider", provider, "outcome", outcome).increment();
        Timer.builder("lingko.external.duration")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .publishPercentiles(0.5, 0.95)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
