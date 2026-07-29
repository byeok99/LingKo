package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 평가 작업의 사용자 격리 조회와 Worker claim용 locking read를 제공한다.
 */
public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, String> {

    Optional<EvaluationJob> findByUserUserIdxAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<EvaluationJob> findByJobIdAndUserUserIdx(String jobId, Long userId);

    @Query("""
            select job.jobId
            from EvaluationJob job
            where job.status in :terminalStatuses
              and job.completedAt < :cutoff
            order by job.completedAt asc
            """)
    List<String> findExpiredTerminalJobIds(
            @Param("terminalStatuses") List<EvaluationJob.Status> terminalStatuses,
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Query("""
            select job.jobId
            from EvaluationJob job
            where job.status = :pendingStatus
              and job.nextAttemptAt <= :now
              and (job.enqueuedAt is null or job.enqueuedAt <= :redispatchCutoff)
            order by job.nextAttemptAt asc, job.createdAt asc
            """)
    List<String> findQueueDispatchCandidates(
            @Param("pendingStatus") EvaluationJob.Status pendingStatus,
            @Param("now") Instant now,
            @Param("redispatchCutoff") Instant redispatchCutoff,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
            update EvaluationJob job
            set job.enqueuedAt = :enqueuedAt
            where job.jobId = :jobId
              and job.status = com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob.Status.PENDING
              and job.nextAttemptAt <= :now
            """)
    int markEnqueuedIfPending(
            @Param("jobId") String jobId,
            @Param("enqueuedAt") Instant enqueuedAt,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from EvaluationJob job where job.jobId = :jobId")
    Optional<EvaluationJob> findByIdForUpdate(@Param("jobId") String jobId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from EvaluationJob job
            where (job.status = :pendingStatus and job.nextAttemptAt <= :now)
               or (job.status = :processingStatus and job.leaseExpiresAt <= :now)
            order by job.createdAt asc
            """)
    List<EvaluationJob> findClaimable(
            @Param("pendingStatus") EvaluationJob.Status pendingStatus,
            @Param("processingStatus") EvaluationJob.Status processingStatus,
            @Param("now") Instant now,
            Pageable pageable
    );
}
