package com.lingko.lingko.core.domain.auth.service;

import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Bearer Access 토큰을 JWT claim과 서버 세션 상태 모두에 대해 인증한다.
 *
 * <p>DB 세션 조회를 통해 로그아웃과 갱신 토큰 재사용 폐기를
 * Access 토큰 만료까지 기다리지 않고 즉시 반영한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ActiveSessionAuthenticator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    /**
     * 토큰의 기기 세션이 활성 상태일 때만 인증된 사용자 ID를 반환한다.
     */
    @Transactional(readOnly = true)
    public Long authenticateBearer(String authorization) {
        String token = extractBearerToken(authorization);
        JwtTokenProvider.AccessTokenClaims claims = jwtTokenProvider.parseAccessToken(token);
        RefreshTokenSession session = refreshTokenSessionRepository.findById(claims.sessionId())
                .orElseThrow(() -> new AuthException("Invalid access session"));

        if (!session.getUser().getUserIdx().equals(claims.userId())
                || session.isRevoked()
                || session.isExpired(Instant.now())) {
            throw new AuthException("Invalid access session");
        }

        return claims.userId();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException("Missing bearer token");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException("Missing bearer token");
        }
        return token;
    }
}
