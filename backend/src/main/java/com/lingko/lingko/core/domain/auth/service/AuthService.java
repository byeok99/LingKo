package com.lingko.lingko.core.domain.auth.service;

import com.lingko.lingko.api.auth.dto.AuthTokenResponse;
import com.lingko.lingko.api.auth.dto.AuthUserResponse;
import com.lingko.lingko.api.auth.dto.OAuthLoginRequest;
import com.lingko.lingko.core.domain.user.entity.User;
import com.lingko.lingko.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GOOGLE_PROVIDER = "GOOGLE";

    private final UserRepository userRepository;
    private final OAuthIdentityVerifier googleIdentityVerifier;
    private final JwtTokenProvider jwtTokenProvider;

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
