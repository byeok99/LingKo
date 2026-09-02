package com.lingko.lingko.core.domain.evaluation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Worker claim과 성공·재시도·최종 실패의 DB 변경을 각각 짧은 transaction으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationJobProcessingService {

    private final EvaluationJobRepository jobRepository;
    private final EvaluationPersistenceService persistenceService;
    private final PracticeQuotaService quotaService;
    private final EvaluationJobSettings settings;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public Optional<EvaluationJob> claimNext() {
        Instant now = clock.instant();
        return jobRepository.findClaimable(
                        EvaluationJob.Status.PENDING,
                        EvaluationJob.Status.PROCESSING,
                        now,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(job -> {
                    job.claim(
                            now,
                            now.plusSeconds(settings.getWorker().getLeaseSeconds())
                    );
                    return job;
                });
    }

    @Transactional
    public void complete(EvaluationJob claimedJob, PracticeResultResponse result) {
        EvaluationJob job = requireProcessingJob(claimedJob.getJobId());
        persistenceService.saveResult(EvaluationPersistenceService.SaveEvaluationResultCommand.builder()
                .user(job.getUser())
                .source(job.getSource())
                .sentenceId(job.getSentenceId())
                .originalText(job.getOriginalText())
                .standardPronunciation(job.getStandardPronunciation())
                .result(result)
                .build());
        job.succeed(writeResult(result), clock.instant());
        // quota native UPDATE가 EntityManager를 clear하므로 작업 상태를 먼저 변경해 flush 대상에 포함한다.
        quotaService.confirmPractice(job.reservation());
    }

    /**
     * 외부 호출 사이의 실제 작업 경계를 짧은 transaction으로 기록해 polling 응답이 현재 phase를 제공한다.
     */
    @Transactional
    public void advancePhase(EvaluationJob claimedJob, EvaluationJob.Phase phase) {
        EvaluationJob job = requireProcessingJob(claimedJob.getJobId());
        job.advancePhase(phase);
    }

    /**
     * @return 원본 음성을 삭제해도 되는 최종 실패이면 true
     */
    @Transactional
    public boolean fail(EvaluationJob claimedJob, RuntimeException failure) {
        EvaluationJob job = requireProcessingJob(claimedJob.getJobId());
        String errorCode = "EVALUATION_FAILED";
        if (job.getAttemptCount() >= settings.getWorker().getMaxAttempts()) {
            job.fail(errorCode, clock.instant());
            // 예약 복구 native UPDATE 전에 FAILED를 기록해 clear 이후 상태 변경 유실을 막는다.
            boolean quotaReleased = quotaService.releasePracticeIfReserved(job.reservation());
            if (!quotaReleased) {
                // 이미 사라진 예약 때문에 terminal 상태 transaction을 롤백하면 같은 실패가 영구 재실행된다.
                log.error(
                        "Evaluation job reached terminal failure without quota reservation: jobId={}, quotaDate={}",
                        job.getJobId(),
                        job.getQuotaDate()
                );
            }
            return true;
        }

        job.scheduleRetry(
                clock.instant().plusSeconds(settings.getWorker().getRetryDelaySeconds()),
                errorCode
        );
        return false;
    }

    private EvaluationJob requireProcessingJob(String jobId) {
        EvaluationJob job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new IllegalStateException("evaluation job does not exist"));
        if (job.getStatus() != EvaluationJob.Status.PROCESSING) {
            throw new IllegalStateException("evaluation job is not processing");
        }
        return job;
    }

    private String writeResult(PracticeResultResponse result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Evaluation result serialization failed", exception);
        }
    }
}
