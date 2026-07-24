package com.lingko.lingko.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 기기 세션 하나를 회전하거나 폐기하는 데 필요한 갱신 토큰을 전달한다.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {

    /**
     * JWT parsing과 해시 비교 전에 전송 과정에서 붙은 공백을 정규화한다.
     */
    public String trimmedRefreshToken() {
        return refreshToken == null ? "" : refreshToken.trim();
    }
}
