package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobWorker;
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
 * DB polling fallback Worker가 claim된 작업만 공통 실행기에 전달하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobWorkerTest {

    @Mock
    private EvaluationJobProcessingService processingService;
    @Mock
    private EvaluationJobExecutor executor;

    @Test
    @DisplayName("claim 가능한 DB 작업이 있으면 공통 실행기로 전달한다")
    void executesClaimedJob() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        when(processingService.claimNext()).thenReturn(Optional.of(job));
        EvaluationJobWorker worker = new EvaluationJobWorker(processingService, executor);

        worker.processNext();

        verify(executor).execute(job);
    }

    @Test
    @DisplayName("claim 가능한 DB 작업이 없으면 실행기를 호출하지 않는다")
    void skipsWhenNoJobIsClaimable() {
        when(processingService.claimNext()).thenReturn(Optional.empty());
        EvaluationJobWorker worker = new EvaluationJobWorker(processingService, executor);

        worker.processNext();

        verify(executor, never()).execute(org.mockito.ArgumentMatchers.any());
    }
}
