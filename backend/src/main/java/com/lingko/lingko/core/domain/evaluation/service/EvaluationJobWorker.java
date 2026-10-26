package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * HTTP 요청과 분리된 단일 제한 Worker로 DB 작업을 순차 처리한다.
 *
 * DB가 작업 원본이므로 프로세스가 종료되어도 lease 만료 후 다시 claim할 수 있다.
 */
@Component
@ConditionalOnProperty(
        name = "evaluation.worker.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EvaluationJobWorker {

    private final EvaluationJobProcessingService processingService;
    private final EvaluationJobExecutor executor;
    private final EvaluationJobTelemetry telemetry;

    /** 운영 Worker에는 실제 telemetry를 주입해 처리와 관측의 생명주기를 함께 구성한다. */
    @Autowired
    public EvaluationJobWorker(
            EvaluationJobProcessingService processingService,
            EvaluationJobExecutor executor,
            EvaluationJobTelemetry telemetry
    ) {
        this.processingService = processingService;
        this.executor = executor;
        this.telemetry = telemetry;
    }

    /** 관측 bean을 구성하지 않는 격리 통합 환경에서도 기존 Worker 계약을 재사용한다. */
    public EvaluationJobWorker(EvaluationJobProcessingService processingService, EvaluationJobExecutor executor) {
        this(processingService, executor, EvaluationJobTelemetry.NOOP);
    }

    @Scheduled(fixedDelayString = "${evaluation.worker.poll-delay-ms:1000}")
    public boolean processNext() {
        Optional<EvaluationJob> claimed = processingService.claimNext();
        if (claimed.isEmpty()) {
            telemetry.heartbeat();
            return false;
        }
        long startedAt = System.nanoTime();
        EvaluationJobExecutor.ExecutionResult result = executor.execute(claimed.get());
        telemetry.record(result, System.nanoTime() - startedAt);
        telemetry.heartbeat();
        return true;
    }
}
