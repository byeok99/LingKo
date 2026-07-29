package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueueWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQS의 at-least-once 전달에서 DB claim 결과에 따라 ACK 또는 재노출하는 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobQueueWorkerTest {

    @Mock
    private EvaluationJobQueue queue;
    @Mock
    private EvaluationJobProcessingService processingService;
    @Mock
    private EvaluationJobExecutor executor;

    private EvaluationJobQueueWorker worker;
    private EvaluationJobSettings settings;

    @BeforeEach
    void setUp() {
        settings = new EvaluationJobSettings();
        worker = new EvaluationJobQueueWorker(queue, processingService, executor, settings);
    }

    @Test
    @DisplayName("Queue 작업이 완료되면 메시지를 ACK한다")
    void acknowledgesCompletedJob() {
        EvaluationJobQueue.Message message =
                new EvaluationJobQueue.Message("job-id", "receipt");
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        when(queue.receive()).thenReturn(Optional.of(message));
        when(processingService.claimQueued("job-id"))
                .thenReturn(EvaluationJobProcessingService.QueueClaim.claimed(job));
        when(executor.execute(job))
                .thenReturn(EvaluationJobExecutor.ExecutionResult.COMPLETED);

        worker.processNext();

        verify(queue).acknowledge(message);
        verify(queue, never()).release(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    @DisplayName("재시도 가능한 실패는 DB retry 시각에 맞춰 메시지를 다시 노출한다")
    void releasesRetryableFailure() {
        EvaluationJobQueue.Message message =
                new EvaluationJobQueue.Message("job-id", "receipt");
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        when(queue.receive()).thenReturn(Optional.of(message));
        when(processingService.claimQueued("job-id"))
                .thenReturn(EvaluationJobProcessingService.QueueClaim.claimed(job));
        when(executor.execute(job))
                .thenReturn(EvaluationJobExecutor.ExecutionResult.RETRY_SCHEDULED);

        worker.processNext();

        verify(queue).release(message, settings.getWorker().getRetryDelaySeconds());
        verify(queue, never()).acknowledge(message);
    }

    @Test
    @DisplayName("다른 Worker lease가 유효하면 메시지를 ACK하지 않고 lease 이후로 미룬다")
    void releasesMessageWhileAnotherWorkerOwnsLease() {
        EvaluationJobQueue.Message message =
                new EvaluationJobQueue.Message("job-id", "receipt");
        when(queue.receive()).thenReturn(Optional.of(message));
        when(processingService.claimQueued("job-id"))
                .thenReturn(EvaluationJobProcessingService.QueueClaim.retryLater(37));

        worker.processNext();

        verify(queue).release(message, 37);
        verify(queue, never()).acknowledge(message);
        verify(executor, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("이미 완료됐거나 없는 작업 메시지는 안전하게 ACK한다")
    void acknowledgesDiscardedMessage() {
        EvaluationJobQueue.Message message =
                new EvaluationJobQueue.Message("job-id", "receipt");
        when(queue.receive()).thenReturn(Optional.of(message));
        when(processingService.claimQueued("job-id"))
                .thenReturn(EvaluationJobProcessingService.QueueClaim.discard());

        worker.processNext();

        verify(queue).acknowledge(message);
        verify(executor, never()).execute(org.mockito.ArgumentMatchers.any());
    }
}
