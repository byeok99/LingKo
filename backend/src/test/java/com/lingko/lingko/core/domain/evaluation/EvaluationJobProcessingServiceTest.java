package com.lingko.lingko.core.domain.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.EvaluationJobSettings;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.evaluation.repository.EvaluationJobRepository;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationJobProcessingService;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationPersistenceService;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 평가 작업의 최종 실패가 쿼터 이상 상태에서도 terminal 상태로 수렴하는 계약을 검증한다.
 */
class EvaluationJobProcessingServiceTest {

    @Test
    @DisplayName("쿼터 예약이 이미 사라졌어도 최종 실패 작업은 FAILED로 종결한다")
    void terminatesJobWhenQuotaReservationIsAlreadyMissing() {
        EvaluationJobRepository jobRepository = mock(EvaluationJobRepository.class);
        EvaluationPersistenceService persistenceService = mock(EvaluationPersistenceService.class);
        PracticeQuotaService quotaService = mock(PracticeQuotaService.class);
        EvaluationJobSettings settings = new EvaluationJobSettings();
        settings.getWorker().setMaxAttempts(3);
        Instant now = Instant.parse("2026-07-30T12:00:00Z");
        EvaluationJob job = processingJob(now);
        when(jobRepository.findByIdForUpdate(job.getJobId())).thenReturn(Optional.of(job));
        when(quotaService.releasePracticeIfReserved(any())).thenReturn(false);
        EvaluationJobProcessingService service = new EvaluationJobProcessingService(
                jobRepository,
                persistenceService,
                quotaService,
                settings,
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC)
        );

        boolean terminalFailure = service.fail(job, new IllegalStateException("missing S3 object"));

        assertThat(terminalFailure).isTrue();
        assertThat(job.getStatus()).isEqualTo(EvaluationJob.Status.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("EVALUATION_FAILED");
        assertThat(job.getCompletedAt()).isEqualTo(now);
        verify(quotaService).releasePracticeIfReserved(job.reservation());
    }

    private EvaluationJob processingJob(Instant now) {
        EvaluationJob job = EvaluationJob.create(
                "job-id",
                User.builder()
                        .userIdx(1L)
                        .socialId("google-sub")
                        .socialType(User.SocialType.GOOGLE)
                        .build(),
                "idempotency-key",
                "request-hash",
                "evaluation-audio/1/missing.wav",
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                "김",
                "김",
                LocalDate.of(2026, 7, 27),
                PracticeQuotaService.QuotaSource.FREE,
                now.minusSeconds(60)
        );
        job.claim(now.minusSeconds(30), now.minusSeconds(20));
        job.claim(now.minusSeconds(20), now.minusSeconds(10));
        job.claim(now.minusSeconds(10), now);
        return job;
    }
}
