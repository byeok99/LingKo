package com.lingko.lingko.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTokenResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long expiresInSeconds;
    private AuthUserResponse user;
}
