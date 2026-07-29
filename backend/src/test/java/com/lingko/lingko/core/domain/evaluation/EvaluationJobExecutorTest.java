package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobExecutor;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Worker 종류와 무관하게 평가 성공·재시도·최종 실패의 S3 및 DB 정리 계약을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobExecutorTest {

    @Mock
    private EvaluationJobProcessingService processingService;
    @Mock
    private EvaluationAudioStorage audioStorage;
    @Mock
    private EvaluationService evaluationService;

    private EvaluationJobExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new EvaluationJobExecutor(processingService, audioStorage, evaluationService);
    }

    @Test
    @DisplayName("평가 성공 시 결과를 완료 처리하고 S3 원본과 로컬 임시 파일을 삭제한다")
    void completesAndDeletesAudio() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        Path audioPath = Path.of("/tmp/audio.wav");
        PracticeResultResponse result = PracticeResultResponse.builder().overallScore(91).build();
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(job.getStandardPronunciation()).thenReturn("안녕하세여.");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenReturn(audioPath);
        when(evaluationService.evaluatePronunciation(audioPath, "안녕하세여.")).thenReturn(result);

        EvaluationJobExecutor.ExecutionResult outcome = executor.execute(job);

        assertThat(outcome).isEqualTo(EvaluationJobExecutor.ExecutionResult.COMPLETED);
        verify(processingService).complete(job, result);
        verify(audioStorage).delete("evaluation-audio/7/audio.wav");
        verify(audioStorage).deleteLocal(audioPath);
    }

    @Test
    @DisplayName("재시도 실패는 S3 원본을 유지하고 로컬 임시 파일만 삭제한다")
    void keepsSourceForRetry() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        Path audioPath = Path.of("/tmp/audio.wav");
        RuntimeException failure = new IllegalStateException("Azure unavailable");
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(job.getStandardPronunciation()).thenReturn("안녕하세여.");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenReturn(audioPath);
        when(evaluationService.evaluatePronunciation(audioPath, "안녕하세여.")).thenThrow(failure);
        when(processingService.fail(job, failure)).thenReturn(false);

        EvaluationJobExecutor.ExecutionResult outcome = executor.execute(job);

        assertThat(outcome).isEqualTo(EvaluationJobExecutor.ExecutionResult.RETRY_SCHEDULED);
        verify(audioStorage, never()).delete("evaluation-audio/7/audio.wav");
        verify(audioStorage).deleteLocal(audioPath);
    }

    @Test
    @DisplayName("최종 실패는 쿼터 보상 후 S3 원본을 삭제한다")
    void deletesSourceAfterTerminalFailure() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        RuntimeException failure = new IllegalStateException("Azure unavailable");
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenThrow(failure);
        when(processingService.fail(job, failure)).thenReturn(true);

        EvaluationJobExecutor.ExecutionResult outcome = executor.execute(job);

        assertThat(outcome).isEqualTo(EvaluationJobExecutor.ExecutionResult.TERMINAL_FAILURE);
        verify(audioStorage).delete("evaluation-audio/7/audio.wav");
    }
}
