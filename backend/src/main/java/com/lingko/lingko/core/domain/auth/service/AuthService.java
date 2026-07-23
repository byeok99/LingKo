package com.lingko.lingko.core.domain.auth.service;

import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.AuthUserResponse;
import com.lingko.lingko.api.auth.dto.OAuthLoginRequest;
import com.lingko.lingko.api.auth.dto.RefreshTokenRequest;
import com.lingko.lingko.core.domain.auth.entity.RefreshTokenSession;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.RefreshTokenReuseException;
import com.lingko.lingko.core.domain.auth.repository.RefreshTokenSessionRepository;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GOOGLE_PROVIDER = "GOOGLE";

    private final UserRepository userRepository;
    private final OAuthIdentityVerifier googleIdentityVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Transactional
    public AuthTokenResponse loginWithOAuth(OAuthLoginRequest request) {
        if (!GOOGLE_PROVIDER.equals(request.normalizedProvider())) {
            throw new IllegalArgumentException("Unsupported OAuth provider");
        }

        OAuthIdentity identity = googleIdentityVerifier.verify(request.trimmedIdToken());
        User user = userRepository.findBySocialIdAndSocialType(identity.socialId(), User.SocialType.GOOGLE)
                .map(existing -> updateUser(existing, identity))
                .orElseGet(() -> createUser(identity));
        JwtTokenProvider.TokenPair tokens = jwtTokenProvider.issueTokens(user.getUserIdx());
        refreshTokenSessionRepository.save(RefreshTokenSession.create(
                tokens.refreshSessionId(),
                user,
                refreshTokenHasher.hash(tokens.refreshToken()),
                tokens.refreshExpiresAt()
        ));

        return toTokenResponse(user, tokens);
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.trimmedRefreshToken();
        JwtTokenProvider.RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        RefreshTokenSession session = findSessionForUpdate(claims.sessionId());
        validateSessionOwner(session, claims.userId());

        Instant now = Instant.now();
        if (session.isRevoked() || session.isExpired(now)) {
            throw new AuthException("Refresh session unavailable");
        }
        if (!refreshTokenHasher.matches(refreshToken, session.getCurrentTokenHash())) {
            session.revoke(now);
            throw new RefreshTokenReuseException();
        }

        JwtTokenProvider.TokenPair tokens = jwtTokenProvider.issueTokens(
                claims.userId(),
                claims.sessionId(),
                session.getExpiresAt()
        );
        session.rotate(refreshTokenHasher.hash(tokens.refreshToken()));

        return toTokenResponse(session.getUser(), tokens);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.trimmedRefreshToken();
        JwtTokenProvider.RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        RefreshTokenSession session = findSessionForUpdate(claims.sessionId());
        validateSessionOwner(session, claims.userId());
        session.revoke(Instant.now());
    }

    private RefreshTokenSession findSessionForUpdate(String sessionId) {
        return refreshTokenSessionRepository.findBySessionIdForUpdate(sessionId)
                .orElseThrow(() -> new AuthException("Refresh session not found"));
    }

    private void validateSessionOwner(RefreshTokenSession session, Long userId) {
        if (!session.getUser().getUserIdx().equals(userId)) {
            throw new AuthException("Refresh session owner mismatch");
        }
    }

    private AuthTokenResponse toTokenResponse(User user, JwtTokenProvider.TokenPair tokens) {
        return AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresInSeconds(tokens.expiresInSeconds())
                .user(toUserResponse(user))
                .build();
    }

    private User updateUser(User user, OAuthIdentity identity) {
        user.updateOAuthProfile(identity.email(), identity.name(), identity.profileImageUrl());

        return user;
    }

    private User createUser(OAuthIdentity identity) {
        User user = User.builder()
                .socialId(identity.socialId())
                .socialType(User.SocialType.GOOGLE)
                .email(identity.email())
                .name(identity.name())
                .profileImageUrl(identity.profileImageUrl())
                .build();

        return userRepository.save(user);
    }

    private AuthUserResponse toUserResponse(User user) {
        return AuthUserResponse.builder()
                .userId(user.getUserIdx())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
