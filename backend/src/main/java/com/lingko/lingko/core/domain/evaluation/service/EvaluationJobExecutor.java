package com.lingko.lingko.core.domain.evaluation.service;

import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobLeaseLostException;
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
    private final com.lingko.lingko.core.domain.legal.service.AiProcessingConsentService aiConsentService;

    public ExecutionResult execute(EvaluationJob job) {
        Path localAudio = null;
        try {
            aiConsentService.requireJobConsent(job.getUser().getUserIdx(), job.getAiConsentId());
            localAudio = audioStorage.download(job.getAudioObjectKey());
            processingService.advancePhase(job, EvaluationJob.Phase.ANALYZING_SPEECH);
            // 다운로드 중 철회도 반영한다. 이 검사 뒤 시작한 전송은 이미 진행 중인 요청으로 취급한다.
            aiConsentService.requireJobConsent(job.getUser().getUserIdx(), job.getAiConsentId());
            PracticeResultResponse result = evaluationService.evaluatePronunciation(
                    localAudio,
                    job.getStandardPronunciation(),
                    () -> processingService.advancePhase(job, EvaluationJob.Phase.PREPARING_GUIDES)
            );
            processingService.advancePhase(job, EvaluationJob.Phase.FINALIZING);
            processingService.complete(job, result);
            deleteSourceBestEffort(job);
            return ExecutionResult.COMPLETED;
        } catch (EvaluationJobLeaseLostException leaseLost) {
            return abandonLostLease(job);
        } catch (RuntimeException failure) {
            log.warn("Evaluation job failed: jobId={}, attempt={}",
                    job.getJobId(), job.getAttemptCount(), failure);
            try {
                boolean terminalFailure = processingService.fail(job, failure);
                if (terminalFailure) {
                    deleteSourceBestEffort(job);
                    return ExecutionResult.TERMINAL_FAILURE;
                }
                return ExecutionResult.RETRY_SCHEDULED;
            } catch (EvaluationJobLeaseLostException leaseLost) {
                return abandonLostLease(job);
            }
        } finally {
            if (localAudio != null) {
                audioStorage.deleteLocal(localAudio);
            }
        }
    }

    /**
     * 다른 시도가 lease를 가져간 작업은 새 소유자가 처리하므로 DB 상태와 S3 원본을 건드리지 않는다.
     * 원본을 지우면 새 소유자의 다운로드가 실패해 정상 작업이 실패로 끝난다.
     */
    private ExecutionResult abandonLostLease(EvaluationJob job) {
        log.warn("Evaluation job lease lost; abandoning stale attempt: jobId={}, attempt={}",
                job.getJobId(), job.getAttemptCount());
        return ExecutionResult.LEASE_LOST;
    }

    private void deleteSourceBestEffort(EvaluationJob job) {
        try {
            audioStorage.delete(job.getAudioObjectKey());
        } catch (RuntimeException deleteFailure) {
            // DB 완료 상태를 되돌리지 않고 S3 Lifecycle이 고아 object를 최종 정리하도록 남긴다.
            log.warn("Evaluation source cleanup failed: jobId={}", job.getJobId(), deleteFailure);
        }
    }

    /** Worker가 다음 polling에서 완료·재시도·영구 실패를 구분하는 실행 결과다. */
    public enum ExecutionResult {
        COMPLETED,
        RETRY_SCHEDULED,
        TERMINAL_FAILURE,
        /** lease 만료 후 다른 시도가 작업을 가져가 이 실행의 결과를 버린 경우 */
        LEASE_LOST
    }
}
