package com.lingko.lingko.core.domain.evaluation.repository;

import com.lingko.lingko.core.domain.evaluation.entity.EvaluationJob;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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

    long countByStatus(EvaluationJob.Status status);

    long countByStatusAndLeaseExpiresAtBefore(EvaluationJob.Status status, Instant now);

    /** 대기시간 gauge 계산에 필요한 가장 오래된 생성 시각만 조회한다. */
    @Query("select min(job.createdAt) from EvaluationJob job where job.status = :status")
    Optional<Instant> findOldestCreatedAtByStatus(@Param("status") EvaluationJob.Status status);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from EvaluationJob job where job.jobId = :jobId")
    Optional<EvaluationJob> findByIdForUpdate(@Param("jobId") String jobId);

    @Modifying
    @Query("delete from EvaluationJob job where job.user.userIdx = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 실행 가능한 작업을 잠그되 다른 Worker가 이미 잠근 행은 건너뛴다.
     *
     * <p>lock timeout -2는 Hibernate의 SKIP LOCKED 지정이다(MySQL 8 {@code FOR UPDATE SKIP LOCKED}).
     * 일반 FOR UPDATE는 Worker를 늘렸을 때 모두 같은 첫 행에서 대기해 사실상 직렬화된다.
     * SKIP LOCKED를 지원하지 않는 DB(H2 테스트 등)에서는 Hibernate가 일반 잠금으로 대체한다.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
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
