package com.lingko.lingko.infra.auth;

import com.lingko.lingko.core.config.AppleOAuthSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentityVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Apple 공개 JWK로 identity token의 서명과 로그인 보안 claim을 검증한다.
 *
 * <p>외부 입력인 token 본문은 신뢰하지 않으며 RS256, issuer, audience, 만료와 요청별 nonce를
 * 모두 확인한 뒤에만 공급자 중립 신원으로 변환한다.</p>
 */
@Component
public class AppleOAuthIdentityVerifier implements OAuthIdentityVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWK_SET_URL = "https://appleid.apple.com/auth/keys";

    private final AppleOAuthSettings settings;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Clock clock;

    @Autowired
    public AppleOAuthIdentityVerifier(AppleOAuthSettings settings) {
        this(settings, createProcessor(), Clock.systemUTC());
    }

    AppleOAuthIdentityVerifier(
            AppleOAuthSettings settings,
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor,
            Clock clock
    ) {
        this.settings = settings;
        this.jwtProcessor = jwtProcessor;
        this.clock = clock;
    }

    @Override
    public String provider() {
        return "APPLE";
    }

    @Override
    public OAuthIdentity verify(String idToken, String rawNonce) {
        validateSettings();
        try {
            JWTClaimsSet claims = jwtProcessor.process(idToken, null);
            validateClaims(claims, rawNonce);
            return new OAuthIdentity(
                    claims.getSubject(),
                    normalizedNullable(claims.getStringClaim("email")),
                    null,
                    null
            );
        } catch (ParseException | JOSEException | BadJOSEException | RuntimeException exception) {
            throw new AuthException("Invalid Apple identity token");
        }
    }

    private void validateClaims(JWTClaimsSet claims, String rawNonce) throws ParseException {
        Instant expiration = claims.getExpirationTime() == null
                ? null
                : claims.getExpirationTime().toInstant();
        String email = claims.getStringClaim("email");
        Object emailVerified = claims.getClaim("email_verified");

        if (!APPLE_ISSUER.equals(claims.getIssuer())
                || !claims.getAudience().contains(settings.getClientId())
                || isBlank(claims.getSubject())
                || claims.getSubject().length() > 255
                || expiration == null
                || !expiration.isAfter(clock.instant())
                || !constantTimeEquals(claims.getStringClaim("nonce"), sha256(rawNonce))
                || (!isBlank(email) && (email.length() > 255 || !isTrue(emailVerified)))) {
            throw new AuthException("Invalid Apple identity token");
        }
    }

    private void validateSettings() {
        if (isBlank(settings.getClientId())) {
            throw new IllegalStateException("Apple client id must be configured");
        }
    }

    private boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    @SneakyThrows
    private String sha256(String value) {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private boolean constantTimeEquals(String actual, String expected) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizedNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @SneakyThrows
    private static ConfigurableJWTProcessor<SecurityContext> createProcessor() {
        // 원격 JWK source는 key를 cache하고 알 수 없는 kid에서 갱신해 Apple key rotation을 따른다.
        JWKSource<SecurityContext> keySource = JWKSourceBuilder
                .create(URI.create(APPLE_JWK_SET_URL).toURL())
                .retrying(true)
                .build();
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
        return processor;
    }
}
