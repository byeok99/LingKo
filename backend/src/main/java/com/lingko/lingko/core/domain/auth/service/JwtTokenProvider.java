package com.lingko.lingko.core.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingko.lingko.core.config.JwtSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
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
import java.util.Objects;

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

    public Long parseAccessTokenUserId(String token) {
        Map<String, Object> payload = parseAndValidate(token);

        if (!"access".equals(payload.get("typ"))) {
            throw new AuthException("Invalid token type");
        }

        Object subject = payload.get("sub");
        if (!(subject instanceof String subjectText)) {
            throw new AuthException("Invalid token subject");
        }

        try {
            return Long.parseLong(subjectText);
        } catch (NumberFormatException exception) {
            throw new AuthException("Invalid token subject");
        }
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

    private Map<String, Object> parseAndValidate(String token) {
        validateSettings();

        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new AuthException("Invalid JWT");
        }

        String signingInput = parts[0] + "." + parts[1];
        if (!Objects.equals(sign(signingInput), parts[2])) {
            throw new AuthException("Invalid JWT signature");
        }

        Map<String, Object> payload = decodePayload(parts[1]);
        long expiresAt = longValue(payload.get("exp"));
        if (expiresAt <= Instant.now(clock).getEpochSecond()) {
            throw new AuthException("Expired JWT");
        }

        return payload;
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);

            return objectMapper.readValue(decoded, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new AuthException("Invalid JWT payload");
        }
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                throw new AuthException("Invalid JWT expiry");
            }
        }

        throw new AuthException("Invalid JWT expiry");
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
