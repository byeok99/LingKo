package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 평가 작업의 사용자 격리 조회와 Worker claim용 locking read를 제공한다.
 */
public interface EvaluationJobRepository extends JpaRepository<EvaluationJob, String> {

    Optional<EvaluationJob> findByUserUserIdxAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<EvaluationJob> findByJobIdAndUserUserIdx(String jobId, Long userId);

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
