package com.lingko.lingko.infra.auth;

import com.lingko.lingko.core.config.GoogleOAuthSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.OAuthProviderUnavailableException;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentityVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/**
 * Google 공개 JWK로 ID token의 서명과 보안 claim을 로컬에서 검증한다.
 *
 * <p>과거에는 로그인마다 Google tokeninfo endpoint를 timeout 없이 호출해, Google 지연이 요청 thread를
 * 무기한 점유했다. tokeninfo는 Google이 디버깅용으로 안내하는 endpoint이기도 하다. 공개 JWK는 cache되고
 * 알 수 없는 kid에서만 갱신되므로 로그인 경로의 원격 호출과 지연 위험이 크게 줄어든다.</p>
 */
@Component
@Slf4j
public class GoogleOAuthIdentityVerifier implements OAuthIdentityVerifier {

    private static final String GOOGLE_JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs";
    /** Google 문서상 ID token issuer는 scheme 유무가 다른 두 값 중 하나다. */
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final GoogleOAuthSettings settings;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Clock clock;

    /**
     * 운영 환경에서는 Google 원격 JWK를 cache·갱신하는 processor와 UTC clock을 사용한다.
     *
     * @param settings 허용할 Google OAuth client ID(audience) 설정
     */
    @Autowired
    public GoogleOAuthIdentityVerifier(GoogleOAuthSettings settings) {
        this(settings, createProcessor(), Clock.systemUTC());
    }

    GoogleOAuthIdentityVerifier(
            GoogleOAuthSettings settings,
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor,
            Clock clock
    ) {
        this.settings = settings;
        this.jwtProcessor = jwtProcessor;
        this.clock = clock;
    }

    @Override
    public String provider() {
        return "GOOGLE";
    }

    /**
     * Google은 nonce를 요구하지 않으므로 {@code rawNonce}는 사용하지 않는다.
     *
     * @throws OAuthProviderUnavailableException 공개 JWK를 가져오지 못한 일시 장애. 사용자 token 문제와
     *         구분해야 앱이 재로그인 대신 재시도를 안내할 수 있다.
     */
    @Override
    public OAuthIdentity verify(String idToken, String rawNonce) {
        validateSettings();
        try {
            JWTClaimsSet claims = jwtProcessor.process(idToken, null);
            validateClaims(claims);
            return new OAuthIdentity(
                    claims.getSubject(),
                    normalizedNullable(claims.getStringClaim("email")),
                    normalizedNullable(claims.getStringClaim("name")),
                    normalizedNullable(claims.getStringClaim("picture"))
            );
        } catch (KeySourceException exception) {
            log.warn("Google public keys are unavailable: {}", exception.getClass().getSimpleName());
            throw new OAuthProviderUnavailableException("Google public keys are unavailable", exception);
        } catch (ParseException | JOSEException | BadJOSEException | RuntimeException exception) {
            // 예외 본문에는 token/claim이 섞일 수 있어 유형만 남긴다.
            log.warn("Google identity verification rejected: {}", exception.getClass().getSimpleName());
            throw new AuthException("Invalid Google id token");
        }
    }

    private void validateClaims(JWTClaimsSet claims) throws ParseException {
        Instant expiration = claims.getExpirationTime() == null
                ? null
                : claims.getExpirationTime().toInstant();
        Object emailVerified = claims.getClaim("email_verified");

        // 다른 Google OAuth client용 token이 인증에 사용되지 않도록 audience를 결합한다.
        // email_verified=false는 거부하고 claim이 없으면 기존 tokeninfo 검증과 같이 허용한다.
        if (!GOOGLE_ISSUERS.contains(claims.getIssuer())
                || claims.getAudience() == null
                || !claims.getAudience().contains(settings.getClientId())
                || isBlank(claims.getSubject())
                || claims.getSubject().length() > 255
                || expiration == null
                || !expiration.isAfter(clock.instant())
                || isFalse(emailVerified)) {
            throw new AuthException("Invalid Google id token");
        }
    }

    private void validateSettings() {
        if (isBlank(settings.getClientId())) {
            throw new IllegalStateException("Google client id must be configured");
        }
    }

    private boolean isFalse(Object value) {
        return Boolean.FALSE.equals(value) || "false".equalsIgnoreCase(String.valueOf(value));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizedNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    @SneakyThrows
    private static ConfigurableJWTProcessor<SecurityContext> createProcessor() {
        // 원격 JWK source는 key를 cache하고 알 수 없는 kid에서 갱신해 Google key rotation을 따른다.
        // Nimbus 기본 HTTP connect/read timeout이 적용되어 Google 지연이 요청 thread를 오래 묶지 않는다.
        JWKSource<SecurityContext> keySource = JWKSourceBuilder
                .create(URI.create(GOOGLE_JWK_SET_URL).toURL())
                .retrying(true)
                .build();
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
        return processor;
    }
}
