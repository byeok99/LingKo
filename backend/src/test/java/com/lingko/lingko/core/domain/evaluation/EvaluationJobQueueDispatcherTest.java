package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueue;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobQueueDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DB에 저장된 PENDING 작업을 SQS에 전달하고 전송 성공 시각을 기록하는 복구 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobQueueDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-07-29T04:00:00Z");

    @Mock
    private EvaluationJobRepository jobRepository;
    @Mock
    private EvaluationJobQueue queue;

    private EvaluationJobQueueDispatcher dispatcher;
    private EvaluationJobSettings settings;

    @BeforeEach
    void setUp() {
        settings = new EvaluationJobSettings();
        dispatcher = new EvaluationJobQueueDispatcher(
                jobRepository,
                queue,
                settings,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("미전송 또는 오래된 PENDING 작업을 batch로 SQS에 발행한다")
    void publishesDispatchCandidates() {
        Instant redispatchCutoff = NOW.minusSeconds(
                settings.getQueue().getRedispatchSeconds()
        );
        when(jobRepository.findQueueDispatchCandidates(
                EvaluationJob.Status.PENDING,
                NOW,
                redispatchCutoff,
                PageRequest.of(0, settings.getQueue().getDispatchBatchSize())
        )).thenReturn(List.of("job-1", "job-2"));
        when(jobRepository.markEnqueuedIfPending("job-1", NOW, NOW)).thenReturn(1);
        when(jobRepository.markEnqueuedIfPending("job-2", NOW, NOW)).thenReturn(1);

        int dispatched = dispatcher.dispatchAvailable();

        assertThat(dispatched).isEqualTo(2);
        verify(queue).publish("job-1");
        verify(queue).publish("job-2");
        verify(jobRepository).markEnqueuedIfPending("job-1", NOW, NOW);
        verify(jobRepository).markEnqueuedIfPending("job-2", NOW, NOW);
    }

    @Test
    @DisplayName("SQS 발행 실패 시 전송 시각을 기록하지 않아 다음 주기에 복구할 수 있다")
    void doesNotMarkJobWhenPublishFails() {
        Instant redispatchCutoff = NOW.minusSeconds(
                settings.getQueue().getRedispatchSeconds()
        );
        when(jobRepository.findQueueDispatchCandidates(
                EvaluationJob.Status.PENDING,
                NOW,
                redispatchCutoff,
                PageRequest.of(0, settings.getQueue().getDispatchBatchSize())
        )).thenReturn(List.of("job-1"));
        doThrow(new IllegalStateException("queue unavailable"))
                .when(queue)
                .publish("job-1");

        assertThatThrownBy(dispatcher::dispatchAvailable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("queue unavailable");

        verify(jobRepository, never())
                .markEnqueuedIfPending("job-1", NOW, NOW);
    }
}
