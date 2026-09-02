package com.lingko.lingko.core.domain.evaluation;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import com.lingko.lingko.core.domain.evaluation.entity.EvaluationLog;
import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DB Worker가 재시작과 재시도에서도 복원할 수 있도록 평가 작업 상태 전이 계약을 검증한다.
 */
class EvaluationJobTest {

    @Test
    @DisplayName("대기 작업을 claim하면 처리 상태와 lease 및 시도 횟수를 기록한다")
    void claimsPendingJob() {
        Instant now = Instant.parse("2026-07-27T01:00:00Z");
        EvaluationJob job = pendingJob(now);

        assertThat(job.getPhase()).isEqualTo(EvaluationJob.Phase.QUEUED);

        job.claim(now, now.plusSeconds(60));

        assertThat(job.getStatus()).isEqualTo(EvaluationJob.Status.PROCESSING);
        assertThat(job.getPhase()).isEqualTo(EvaluationJob.Phase.DOWNLOADING_AUDIO);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getLeaseExpiresAt()).isEqualTo(now.plusSeconds(60));
    }

    @Test
    @DisplayName("재시도 가능한 실패는 예약을 유지하고 다음 실행 시각으로 되돌린다")
    void schedulesRetry() {
        Instant now = Instant.parse("2026-07-27T01:00:00Z");
        EvaluationJob job = pendingJob(now);
        job.claim(now, now.plusSeconds(60));

        job.scheduleRetry(now.plusSeconds(5), "EVALUATION_FAILED");

        assertThat(job.getStatus()).isEqualTo(EvaluationJob.Status.PENDING);
        assertThat(job.getPhase()).isEqualTo(EvaluationJob.Phase.QUEUED);
        assertThat(job.getNextAttemptAt()).isEqualTo(now.plusSeconds(5));
        assertThat(job.getErrorCode()).isEqualTo("EVALUATION_FAILED");
        assertThat(job.getLeaseExpiresAt()).isNull();
    }

    @Test
    @DisplayName("처리 phase는 같은 시도 안에서 앞으로만 이동한다")
    void advancesPhaseMonotonically() {
        Instant now = Instant.parse("2026-07-27T01:00:00Z");
        EvaluationJob job = pendingJob(now);
        job.claim(now, now.plusSeconds(60));

        job.advancePhase(EvaluationJob.Phase.ANALYZING_SPEECH);
        job.advancePhase(EvaluationJob.Phase.PREPARING_GUIDES);

        assertThat(job.getPhase()).isEqualTo(EvaluationJob.Phase.PREPARING_GUIDES);
        assertThatThrownBy(() -> job.advancePhase(EvaluationJob.Phase.ANALYZING_SPEECH))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("성공 작업은 결과 payload를 보존하고 lease를 제거한다")
    void succeedsWithResult() {
        Instant now = Instant.parse("2026-07-27T01:00:00Z");
        EvaluationJob job = pendingJob(now);
        job.claim(now, now.plusSeconds(60));

        job.succeed("{\"overallScore\":91}", now.plusSeconds(3));

        assertThat(job.getStatus()).isEqualTo(EvaluationJob.Status.SUCCEEDED);
        assertThat(job.getResultPayload()).contains("\"overallScore\":91");
        assertThat(job.getCompletedAt()).isEqualTo(now.plusSeconds(3));
        assertThat(job.getLeaseExpiresAt()).isNull();
    }

    private EvaluationJob pendingJob(Instant now) {
        return EvaluationJob.create(
                "job-id",
                User.builder().socialId("social-id").socialType(User.SocialType.GOOGLE).build(),
                "idempotency-key",
                "request-hash",
                "evaluation-audio/7/audio-id.wav",
                EvaluationLog.PracticeSource.CUSTOM,
                null,
                "안녕하세요.",
                "안녕하세여.",
                LocalDate.of(2026, 7, 27),
                PracticeQuotaService.QuotaSource.FREE,
                now
        );
    }
}
