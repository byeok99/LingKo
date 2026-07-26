package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.DailyPracticeQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Daily Practice 할당량 영속성 연산을 추상화한다.
 *
 * 도메인 서비스가 query와 저장 기술 세부사항에 의존하지 않도록 저장소 경계를 선택했다.
 */
public interface DailyPracticeQuotaRepository extends JpaRepository<DailyPracticeQuota, Long> {

    Optional<DailyPracticeQuota> findByUserUserIdxAndQuotaDate(Long userId, LocalDate quotaDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select quota
            from DailyPracticeQuota quota
            where quota.user.userIdx = :userId
              and quota.quotaDate = :quotaDate
            """)
    Optional<DailyPracticeQuota> findByUserAndDateForUpdate(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    /**
     * 잔여 무료 횟수 조건과 예약 증가를 하나의 UPDATE로 처리해 조회-수정 경쟁을 제거한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET free_reserved = free_reserved + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND free_used + free_reserved < free_limit
            """, nativeQuery = true)
    int reserveFreePractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET rewarded_reserved = rewarded_reserved + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND rewarded_reserved < rewarded_available
            """, nativeQuery = true)
    int reserveRewardedPractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET free_reserved = free_reserved - 1,
                free_used = free_used + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND free_reserved > 0
            """, nativeQuery = true)
    int confirmFreePractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET rewarded_reserved = rewarded_reserved - 1,
                rewarded_available = rewarded_available - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND rewarded_reserved > 0
            """, nativeQuery = true)
    int confirmRewardedPractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET free_reserved = free_reserved - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND free_reserved > 0
            """, nativeQuery = true)
    int releaseFreePractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE daily_practice_quota
            SET rewarded_reserved = rewarded_reserved - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE user_idx = :userId
              AND quota_date = :quotaDate
              AND rewarded_reserved > 0
            """, nativeQuery = true)
    int releaseRewardedPractice(
            @Param("userId") Long userId,
            @Param("quotaDate") LocalDate quotaDate
    );
}
