package com.lingko.lingko.core.domain.auth.repository;

import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSession session where session.sessionId = :sessionId")
    Optional<RefreshTokenSession> findBySessionIdForUpdate(@Param("sessionId") String sessionId);
}
