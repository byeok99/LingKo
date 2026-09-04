package com.lingko.lingko.infra.observability;

import com.lingko.lingko.core.domain.evaluation.service.GuideGenerationJobTelemetry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 가이드 생성의 요청 결과와 실행 중 작업 수를 bounded tag의 Micrometer 지표로 기록한다.
 */
@Component
public class MicrometerGuideGenerationJobTelemetry implements GuideGenerationJobTelemetry {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeJobs = new AtomicInteger();

    public MicrometerGuideGenerationJobTelemetry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("lingko.guide.jobs.active", activeJobs, AtomicInteger::get)
                .description("Currently running guide generation jobs")
                .register(meterRegistry);
    }

    @Override
    public void request(String outcome) {
        meterRegistry.counter("lingko.guide.jobs.requests", "outcome", outcome).increment();
    }

    @Override
    public void jobStarted() {
        activeJobs.incrementAndGet();
    }

    @Override
    public void jobFinished(String outcome) {
        activeJobs.updateAndGet(current -> Math.max(0, current - 1));
        meterRegistry.counter("lingko.guide.jobs.completed", "outcome", outcome).increment();
    }
}
