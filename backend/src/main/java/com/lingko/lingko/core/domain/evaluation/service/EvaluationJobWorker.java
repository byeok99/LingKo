package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * HTTP 요청과 분리된 단일 제한 Worker로 DB 작업을 순차 처리한다.
 *
 * DB가 작업 원본이므로 프로세스가 종료되어도 lease 만료 후 다시 claim할 수 있다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("""
        ${evaluation.worker.enabled:true}
        and '${evaluation.worker.mode:database}'.equalsIgnoreCase('database')
        """)
public class EvaluationJobWorker {

    private final EvaluationJobProcessingService processingService;
    private final EvaluationJobExecutor executor;

    @Scheduled(fixedDelayString = "${evaluation.worker.poll-delay-ms:1000}")
    public void processNext() {
        Optional<EvaluationJob> claimed = processingService.claimNext();
        if (claimed.isEmpty()) {
            return;
        }
        executor.execute(claimed.get());
    }
}
