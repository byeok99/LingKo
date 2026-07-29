package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * 독립 DB polling Worker의 평가 실행·보상 절차를 소유한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationJobExecutor {

    private final EvaluationJobProcessingService processingService;
    private final EvaluationAudioStorage audioStorage;
    private final EvaluationService evaluationService;

    public ExecutionResult execute(EvaluationJob job) {
        Path localAudio = null;
        try {
            localAudio = audioStorage.download(job.getAudioObjectKey());
            PracticeResultResponse result = evaluationService.evaluatePronunciation(
                    localAudio,
                    job.getStandardPronunciation()
            );
            processingService.complete(job, result);
            deleteSourceBestEffort(job);
            return ExecutionResult.COMPLETED;
        } catch (RuntimeException failure) {
            log.warn("Evaluation job failed: jobId={}, attempt={}",
                    job.getJobId(), job.getAttemptCount(), failure);
            boolean terminalFailure = processingService.fail(job, failure);
            if (terminalFailure) {
                deleteSourceBestEffort(job);
                return ExecutionResult.TERMINAL_FAILURE;
            }
            return ExecutionResult.RETRY_SCHEDULED;
        } finally {
            if (localAudio != null) {
                audioStorage.deleteLocal(localAudio);
            }
        }
    }

    private void deleteSourceBestEffort(EvaluationJob job) {
        try {
            audioStorage.delete(job.getAudioObjectKey());
        } catch (RuntimeException deleteFailure) {
            // DB 완료 상태를 되돌리지 않고 S3 Lifecycle이 고아 object를 최종 정리하도록 남긴다.
            log.warn("Evaluation source cleanup failed: jobId={}", job.getJobId(), deleteFailure);
        }
    }

    public enum ExecutionResult {
        COMPLETED,
        RETRY_SCHEDULED,
        TERMINAL_FAILURE
    }
}
