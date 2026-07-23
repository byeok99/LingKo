package com.lingko.lingko.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {
    public String trimmedRefreshToken() {
        return refreshToken == null ? "" : refreshToken.trim();
    }
}
