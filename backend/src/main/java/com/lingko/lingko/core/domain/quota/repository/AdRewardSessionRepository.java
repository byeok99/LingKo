package com.lingko.lingko.core.domain.quota.repository;

import com.lingko.lingko.core.domain.quota.entity.AdRewardSession;
import com.lingko.lingko.core.domain.quota.entity.AdRewardSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** 광고 보상 session을 사용자 조회와 SSV 처리 lock 경계로 관리한다. */
public interface AdRewardSessionRepository extends JpaRepository<AdRewardSession, Long> {

    Optional<AdRewardSession> findByUserIdAndSessionTokenHash(Long userId, String sessionTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AdRewardSession session where session.sessionTokenHash = :tokenHash")
    Optional<AdRewardSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from AdRewardSession session where session.userId = :userId and session.status = :status")
    int deleteAllByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") AdRewardSessionStatus status
    );

    @Modifying
    @Query("delete from AdRewardSession session where session.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
