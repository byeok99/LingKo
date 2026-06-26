package com.lingko.lingko.core.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.JwtSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";

    private final JwtSettings jwtSettings;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(JwtSettings jwtSettings, ObjectMapper objectMapper) {
        this(jwtSettings, objectMapper, Clock.systemUTC());
    }

    JwtTokenProvider(JwtSettings jwtSettings, ObjectMapper objectMapper, Clock clock) {
        this.jwtSettings = jwtSettings;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public TokenPair issueTokens(Long userId) {
        validateSettings();

        long issuedAt = Instant.now(clock).getEpochSecond();
        long accessExpiresAt = issuedAt + accessTokenExpiresInSeconds();
        long refreshExpiresAt = issuedAt + refreshTokenExpiresInSeconds();

        return new TokenPair(
                createToken(userId, "access", issuedAt, accessExpiresAt),
                createToken(userId, "refresh", issuedAt, refreshExpiresAt),
                accessTokenExpiresInSeconds()
        );
    }

    public long accessTokenExpiresInSeconds() {
        return jwtSettings.getAccessTokenExpireMinutes() * 60L;
    }

    private String createToken(Long userId, String type, long issuedAt, long expiresAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("typ", type);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);

        String encodedHeader = base64UrlJson(header);
        String encodedPayload = base64UrlJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;

        return signingInput + "." + sign(signingInput);
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to create JWT", e);
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtSettings.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private long refreshTokenExpiresInSeconds() {
        return jwtSettings.getRefreshTokenExpireDays() * 24L * 60L * 60L;
    }

    private void validateSettings() {
        if (!JWT_ALGORITHM.equals(jwtSettings.getAlgorithm())) {
            throw new IllegalStateException("Unsupported JWT algorithm");
        }
        if (jwtSettings.getSecretKey() == null || jwtSettings.getSecretKey().length() < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 characters");
        }
        if (jwtSettings.getAccessTokenExpireMinutes() == null || jwtSettings.getAccessTokenExpireMinutes() < 1) {
            throw new IllegalStateException("JWT access token expiry must be positive");
        }
        if (jwtSettings.getRefreshTokenExpireDays() == null || jwtSettings.getRefreshTokenExpireDays() < 1) {
            throw new IllegalStateException("JWT refresh token expiry must be positive");
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long expiresInSeconds
    ) {
    }
}
