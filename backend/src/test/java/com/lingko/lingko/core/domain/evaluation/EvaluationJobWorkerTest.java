package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationAudioStorage;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobWorker;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Worker가 성공·재시도·최종 실패에서 S3와 DB 상태를 올바르게 정리하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationJobWorkerTest {

    @Mock
    private EvaluationJobProcessingService processingService;
    @Mock
    private EvaluationAudioStorage audioStorage;
    @Mock
    private EvaluationService evaluationService;

    private EvaluationJobWorker worker;

    @BeforeEach
    void setUp() {
        worker = new EvaluationJobWorker(processingService, audioStorage, evaluationService);
    }

    @Test
    @DisplayName("평가 성공 시 결과를 완료 처리하고 S3 원본과 로컬 임시 파일을 삭제한다")
    void completesAndDeletesAudio() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        Path audioPath = Path.of("/tmp/audio.wav");
        PracticeResultResponse result = PracticeResultResponse.builder().overallScore(91).build();
        when(processingService.claimNext()).thenReturn(Optional.of(job));
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(job.getStandardPronunciation()).thenReturn("안녕하세여.");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenReturn(audioPath);
        when(evaluationService.evaluatePronunciation(audioPath, "안녕하세여.")).thenReturn(result);

        worker.processNext();

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
        when(processingService.claimNext()).thenReturn(Optional.of(job));
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(job.getStandardPronunciation()).thenReturn("안녕하세여.");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenReturn(audioPath);
        when(evaluationService.evaluatePronunciation(audioPath, "안녕하세여.")).thenThrow(failure);
        when(processingService.fail(job, failure)).thenReturn(false);

        worker.processNext();

        verify(audioStorage, never()).delete("evaluation-audio/7/audio.wav");
        verify(audioStorage).deleteLocal(audioPath);
    }

    @Test
    @DisplayName("최종 실패는 쿼터 보상 후 S3 원본을 삭제한다")
    void deletesSourceAfterTerminalFailure() {
        EvaluationJob job = org.mockito.Mockito.mock(EvaluationJob.class);
        RuntimeException failure = new IllegalStateException("Azure unavailable");
        when(processingService.claimNext()).thenReturn(Optional.of(job));
        when(job.getAudioObjectKey()).thenReturn("evaluation-audio/7/audio.wav");
        when(audioStorage.download("evaluation-audio/7/audio.wav")).thenThrow(failure);
        when(processingService.fail(job, failure)).thenReturn(true);

        worker.processNext();

        verify(audioStorage).delete("evaluation-audio/7/audio.wav");
    }
}
