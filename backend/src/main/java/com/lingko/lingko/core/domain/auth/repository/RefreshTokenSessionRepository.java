package com.lingko.lingko.core.domain.auth.repository;

import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 갱신 토큰 회전과 폐기에 필요한 서버 상태를 영속화한다.
 */
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, String> {

    /**
     * 한 세션의 갱신 시도를 직렬화해 하나의 토큰만 회전에 성공하게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSession session where session.sessionId = :sessionId")
    Optional<RefreshTokenSession> findBySessionIdForUpdate(@Param("sessionId") String sessionId);

    @Modifying
    @Query("delete from RefreshTokenSession session where session.user.userIdx = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
