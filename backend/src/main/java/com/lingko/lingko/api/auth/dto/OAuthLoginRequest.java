package com.lingko.lingko.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String provider,
        @NotBlank String idToken
) {
    public String normalizedProvider() {
        return provider == null ? "" : provider.trim().toUpperCase();
    }

    public String trimmedIdToken() {
        return idToken == null ? "" : idToken.trim();
    }
}
