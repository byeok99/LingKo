package com.lingko.lingko.core.domain.auth.service;

public record OAuthIdentity(
        String socialId,
        String email,
        String name,
        String profileImageUrl
) {
}
