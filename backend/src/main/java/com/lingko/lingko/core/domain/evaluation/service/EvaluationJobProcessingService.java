package com.lingko.lingko.core.domain.evaluation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.api.evaluation.dto.PracticeResultResponse;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobLeaseLostException;
import com.lingko.lingko.core.domain.evaluation.exception.InvalidEvaluationAudioException;
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
        EvaluationJob job = requireProcessingJob(claimedJob);
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
        EvaluationJob job = requireProcessingJob(claimedJob);
        job.advancePhase(phase);
    }

    /**
     * 실패를 재시도 또는 최종 실패로 기록한다.
     *
     * <p>작업 {@code errorCode} 값:
     * <ul>
     *   <li>{@code AI_CONSENT_REQUIRED}: AI 처리 동의 철회. 즉시 최종 실패</li>
     *   <li>{@code INVALID_AUDIO}: 업로드 음성 형식 오류. 재시도해도 같으므로 즉시 최종 실패</li>
     *   <li>{@code EVALUATION_FAILED}: 그 밖의 일시 장애. 최대 시도 횟수까지 재시도</li>
     * </ul></p>
     *
     * @return 원본 음성을 삭제해도 되는 최종 실패이면 true
     */
    @Transactional
    public boolean fail(EvaluationJob claimedJob, RuntimeException failure) {
        EvaluationJob job = requireProcessingJob(claimedJob);
        boolean consentDenied = failure instanceof com.lingko.lingko.core.domain.legal.service.AiConsentRequiredException;
        boolean invalidAudio = failure instanceof InvalidEvaluationAudioException;
        String errorCode = consentDenied
                ? "AI_CONSENT_REQUIRED"
                : invalidAudio ? "INVALID_AUDIO" : "EVALUATION_FAILED";
        // 철회와 형식 오류는 일시적 외부 장애가 아니므로 재시도하지 않고 즉시 종료해 예약을 복구한다.
        if (consentDenied || invalidAudio || job.getAttemptCount() >= settings.getWorker().getMaxAttempts()) {
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

    /**
     * 호출 Worker가 아직 이 작업의 현재 lease 소유자인지 확인하고 잠긴 최신 행을 반환한다.
     *
     * <p>claim마다 attemptCount가 증가하므로 claim 시점의 값이 현재 행과 같아야 같은 시도다.
     * 상태만 확인하면 lease 만료 후 다른 Worker가 재claim해 PROCESSING인 작업을 이전 Worker가
     * 완료·실패 처리해 결과 중복 저장이나 상태 역전이 생긴다.</p>
     *
     * @throws EvaluationJobLeaseLostException 다른 시도가 lease를 가져갔거나 작업이 더 이상 PROCESSING이 아님
     */
    private EvaluationJob requireProcessingJob(EvaluationJob claimedJob) {
        String jobId = claimedJob.getJobId();
        EvaluationJob job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new EvaluationJobLeaseLostException(jobId));
        if (job.getStatus() != EvaluationJob.Status.PROCESSING
                || job.getAttemptCount() != claimedJob.getAttemptCount()) {
            throw new EvaluationJobLeaseLostException(jobId);
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
