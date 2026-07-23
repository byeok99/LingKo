package com.lingko.lingko.core.domain.auth.service;

import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ActiveSessionAuthenticator {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

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
