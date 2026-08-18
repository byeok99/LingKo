package com.lingko.lingko.core.domain.evaluation.entity;

import com.lingko.lingko.core.domain.quota.service.PracticeQuotaService;
import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * S3 음성 평가의 영속 작업 상태와 재시작 가능한 lease·재시도 전이를 소유한다.
 */
@Entity
@Table(
        name = "evaluation_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_evaluation_jobs_user_idempotency",
                        columnNames = {"user_idx", "idempotency_key"}
                ),
                @UniqueConstraint(
                        name = "uk_evaluation_jobs_audio_object",
                        columnNames = "audio_object_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationJob {

    @Id
    @Column(name = "job_id", nullable = false, length = 36)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "audio_object_key", nullable = false, length = 500)
    private String audioObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private EvaluationLog.PracticeSource source;

    @Column(name = "sentence_id")
    private Long sentenceId;

    @Column(name = "original_text", nullable = false, length = 300)
    private String originalText;

    @Column(name = "standard_pronunciation", nullable = false, length = 300)
    private String standardPronunciation;

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "quota_source", nullable = false, length = 20)
    private PracticeQuotaService.QuotaSource quotaSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "result_payload", columnDefinition = "LONGTEXT")
    private String resultPayload;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private EvaluationJob(
            String jobId,
            User user,
            String idempotencyKey,
            String requestHash,
            String audioObjectKey,
            EvaluationLog.PracticeSource source,
            Long sentenceId,
            String originalText,
            String standardPronunciation,
            LocalDate quotaDate,
            PracticeQuotaService.QuotaSource quotaSource,
            Instant now
    ) {
        this.jobId = jobId;
        this.user = user;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.audioObjectKey = audioObjectKey;
        this.source = source;
        this.sentenceId = sentenceId;
        this.originalText = originalText;
        this.standardPronunciation = standardPronunciation;
        this.quotaDate = quotaDate;
        this.quotaSource = quotaSource;
        this.status = Status.PENDING;
        this.nextAttemptAt = now;
    }

    public static EvaluationJob create(
            String jobId,
            User user,
            String idempotencyKey,
            String requestHash,
            String audioObjectKey,
            EvaluationLog.PracticeSource source,
            Long sentenceId,
            String originalText,
            String standardPronunciation,
            LocalDate quotaDate,
            PracticeQuotaService.QuotaSource quotaSource,
            Instant now
    ) {
        return new EvaluationJob(
                jobId,
                user,
                idempotencyKey,
                requestHash,
                audioObjectKey,
                source,
                sentenceId,
                originalText,
                standardPronunciation,
                quotaDate,
                quotaSource,
                now
        );
    }

    /**
     * Worker 한 개가 작업을 소유하도록 처리 상태와 만료 가능한 lease를 함께 기록한다.
     */
    public void claim(Instant now, Instant leaseExpiresAt) {
        status = Status.PROCESSING;
        attemptCount++;
        nextAttemptAt = now;
        this.leaseExpiresAt = leaseExpiresAt;
        errorCode = null;
    }

    public void scheduleRetry(Instant nextAttemptAt, String errorCode) {
        status = Status.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        this.errorCode = errorCode;
        leaseExpiresAt = null;
    }

    public void succeed(String resultPayload, Instant completedAt) {
        status = Status.SUCCEEDED;
        this.resultPayload = resultPayload;
        this.completedAt = completedAt;
        errorCode = null;
        leaseExpiresAt = null;
    }

    public void fail(String errorCode, Instant completedAt) {
        status = Status.FAILED;
        this.errorCode = errorCode;
        this.completedAt = completedAt;
        leaseExpiresAt = null;
    }

    public boolean hasSameRequestHash(String candidate) {
        return requestHash.equals(candidate);
    }

    public PracticeQuotaService.PracticeQuotaReservation reservation() {
        return new PracticeQuotaService.PracticeQuotaReservation(
                user.getUserIdx(),
                quotaDate,
                quotaSource
        );
    }

    /** 비동기 평가 작업의 영속 상태이며 Worker의 claim·완료·실패 전이를 제한한다. */
    public enum Status {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED
    }
}
