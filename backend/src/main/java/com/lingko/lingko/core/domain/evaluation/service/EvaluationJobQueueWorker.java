package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SQS 메시지를 소비하되 DB lease를 획득한 작업만 실행하는 독립 확장 Worker다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("""
        ${evaluation.worker.enabled:true}
        and '${evaluation.worker.mode:database}'.equalsIgnoreCase('sqs')
        """)
public class EvaluationJobQueueWorker {

    private final EvaluationJobQueue queue;
    private final EvaluationJobProcessingService processingService;
    private final EvaluationJobExecutor executor;
    private final EvaluationJobSettings settings;

    @Scheduled(fixedDelayString = "${evaluation.worker.poll-delay-ms:1000}")
    public boolean processNext() {
        Optional<EvaluationJobQueue.Message> received = queue.receive();
        if (received.isEmpty()) {
            return false;
        }

        EvaluationJobQueue.Message message = received.get();
        EvaluationJobProcessingService.QueueClaim claim =
                processingService.claimQueued(message.jobId());
        switch (claim.disposition()) {
            case DISCARD -> queue.acknowledge(message);
            case RETRY_LATER -> queue.release(message, claim.retryAfterSeconds());
            case CLAIMED -> handleClaimed(message, claim.job());
        }
        return true;
    }

    private void handleClaimed(
            EvaluationJobQueue.Message message,
            com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob job
    ) {
        EvaluationJobExecutor.ExecutionResult result = executor.execute(job);
        if (result == EvaluationJobExecutor.ExecutionResult.RETRY_SCHEDULED) {
            queue.release(message, settings.getWorker().getRetryDelaySeconds());
            return;
        }
        queue.acknowledge(message);
    }
}
