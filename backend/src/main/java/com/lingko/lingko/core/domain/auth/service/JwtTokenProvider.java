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
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LingKo HS256 Access·갱신 JWT를 발급하고 검증한다.
 *
 * <p>두 토큰이 동일한 session ID를 공유해 서버 폐기가 일관되게 적용되며,
 * 갱신 토큰은 회전마다 고유 ID도 함께 가진다.</p>
 */
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

    /**
     * 무작위 기기 세션 ID로 새로운 토큰 계열을 시작한다.
     */
    public TokenPair issueTokens(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        Instant refreshExpiresAt = Instant.now(clock).plusSeconds(refreshTokenExpiresInSeconds());
        return issueTokens(userId, sessionId, refreshExpiresAt);
    }

    /**
     * 갱신 세션의 절대 만료를 연장하지 않고 토큰 계열을 회전한다.
     */
    public TokenPair issueTokens(Long userId, String sessionId, Instant refreshExpiresAt) {
        validateSettings();

        long issuedAt = Instant.now(clock).getEpochSecond();
        long accessExpiresAt = issuedAt + accessTokenExpiresInSeconds();
        if (refreshExpiresAt.getEpochSecond() <= issuedAt) {
            throw new AuthException("Expired refresh session");
        }

        return new TokenPair(
                createToken(
                        userId,
                        "access",
                        issuedAt,
                        accessExpiresAt,
                        Map.of(
                                "jti", UUID.randomUUID().toString(),
                                "sid", sessionId
                        )
                ),
                createToken(
                        userId,
                        "refresh",
                        issuedAt,
                        refreshExpiresAt.getEpochSecond(),
                        Map.of(
                                "jti", UUID.randomUUID().toString(),
                                "sid", sessionId
                        )
                ),
                accessTokenExpiresInSeconds(),
                sessionId,
                refreshExpiresAt
        );
    }

    public long accessTokenExpiresInSeconds() {
        return jwtSettings.getAccessTokenExpireMinutes() * 60L;
    }

    /**
     * Access 토큰 subject만 필요한 호출자를 위해 사용자 ID를 추출한다.
     */
    public Long parseAccessTokenUserId(String token) {
        return parseAccessToken(token).userId();
    }

    /**
     * Access 토큰을 검증하고 활성 세션 조회에 필요한 claim을 반환한다.
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Map<String, Object> payload = parseAndValidate(token);

        if (!"access".equals(payload.get("typ"))) {
            throw new AuthException("Invalid token type");
        }

        return new AccessTokenClaims(
                parseSubject(payload.get("sub")),
                requiredText(payload.get("sid"), "Invalid access session")
        );
    }

    /**
     * 갱신 토큰을 검증하고 토큰 계열·회전 metadata를 반환한다.
     */
    public RefreshTokenClaims parseRefreshToken(String token) {
        Map<String, Object> payload = parseAndValidate(token);

        if (!"refresh".equals(payload.get("typ"))) {
            throw new AuthException("Invalid token type");
        }

        return new RefreshTokenClaims(
                parseSubject(payload.get("sub")),
                requiredText(payload.get("sid"), "Invalid refresh session"),
                requiredText(payload.get("jti"), "Invalid refresh token id"),
                Instant.ofEpochSecond(longValue(payload.get("exp")))
        );
    }

    private String createToken(
            Long userId,
            String type,
            long issuedAt,
            long expiresAt,
            Map<String, Object> additionalClaims
    ) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", JWT_ALGORITHM);
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("typ", type);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);
        payload.putAll(additionalClaims);

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

        // 일치하는 signature prefix가 노출되지 않도록 constant-time으로 비교한다.
        String signingInput = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(
                sign(signingInput).getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII)
        )) {
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

    private Long parseSubject(Object value) {
        if (!(value instanceof String subjectText)) {
            throw new AuthException("Invalid token subject");
        }

        try {
            return Long.parseLong(subjectText);
        } catch (NumberFormatException exception) {
            throw new AuthException("Invalid token subject");
        }
    }

    private String requiredText(Object value, String errorMessage) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new AuthException(errorMessage);
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

    /**
     * 클라이언트에 전달할 토큰 쌍과 서버 세션 metadata를 함께 보관한다.
     */
    public record TokenPair(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            String refreshSessionId,
            Instant refreshExpiresAt
    ) {
    }

    /**
     * 갱신 Session 하나를 조회하고 회전하는 데 필요한 claim이다.
     */
    public record RefreshTokenClaims(
            Long userId,
            String sessionId,
            String tokenId,
            Instant expiresAt
    ) {
    }

    /**
     * Access 토큰을 활성 서버 세션에 연결하는 데 필요한 claim이다.
     */
    public record AccessTokenClaims(
            Long userId,
            String sessionId
    ) {
    }
}
