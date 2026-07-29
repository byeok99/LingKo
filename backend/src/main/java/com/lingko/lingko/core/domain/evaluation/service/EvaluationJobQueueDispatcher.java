package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * DB transaction 이후 유실될 수 있는 Queue 발행을 PENDING 작업 재조회로 복구한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("""
        ${evaluation.queue.dispatcher-enabled:true}
        and '${evaluation.worker.mode:database}'.equalsIgnoreCase('sqs')
        """)
public class EvaluationJobQueueDispatcher {

    private final EvaluationJobRepository jobRepository;
    private final EvaluationJobQueue queue;
    private final EvaluationJobSettings settings;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${evaluation.queue.dispatch-delay-ms:500}")
    public int dispatchAvailable() {
        Instant now = clock.instant();
        EvaluationJobSettings.Queue queueSettings = settings.getQueue();
        List<String> jobIds = jobRepository.findQueueDispatchCandidates(
                EvaluationJob.Status.PENDING,
                now,
                now.minusSeconds(queueSettings.getRedispatchSeconds()),
                PageRequest.of(0, queueSettings.getDispatchBatchSize())
        );

        int dispatched = 0;
        for (String jobId : jobIds) {
            queue.publish(jobId);
            dispatched += jobRepository.markEnqueuedIfPending(jobId, now, now);
        }
        if (dispatched > 0) {
            log.debug("Evaluation jobs dispatched: count={}", dispatched);
        }
        return dispatched;
    }
}
