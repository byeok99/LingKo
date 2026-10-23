package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.AdRewardSession;
import com.lingko.lingko.core.domain.quota.entity.AdRewardSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/** 광고 보상 session을 사용자 조회와 SSV 처리 lock 경계로 관리한다. */
public interface AdRewardSessionRepository extends JpaRepository<AdRewardSession, Long> {

    Optional<AdRewardSession> findByUserIdAndSessionTokenHash(Long userId, String sessionTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AdRewardSession session where session.sessionTokenHash = :tokenHash")
    Optional<AdRewardSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * 만료 시각이 지난 PENDING 세션만 지운다.
     *
     * <p>아직 유효한 PENDING 세션은 SSV callback이 늦게 도착할 수 있으므로 남겨 둔다.
     * 새 세션 생성 시 이를 지우면 사용자가 이미 시청한 광고의 보상이 사라진다.</p>
     */
    @Modifying
    @Query("""
            delete from AdRewardSession session
            where session.userId = :userId
              and session.status = :status
              and session.expiresAt <= :now
            """)
    int deleteExpiredByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") AdRewardSessionStatus status,
            @Param("now") Instant now
    );

    /** 동시에 열린 유효 PENDING 세션 수로, 세션 생성 남용 상한 판단에 쓴다. */
    long countByUserIdAndStatusAndExpiresAtAfter(Long userId, AdRewardSessionStatus status, Instant now);

    @Modifying
    @Query("delete from AdRewardSession session where session.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
