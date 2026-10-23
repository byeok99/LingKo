package com.lingko.lingko.infra.auth;

import com.lingko.lingko.core.config.GoogleOAuthSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.OAuthProviderUnavailableException;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Google ID token을 원격 tokeninfo 없이 공개 JWK로 로컬 검증하는 계약과,
 * 공개 키 장애를 사용자 token 오류(401)와 구분하는 실패 분류를 고정한다.
 */
class GoogleOAuthIdentityVerifierTest {

    private static final String CLIENT_ID = "lingko-web-client.apps.googleusercontent.com";
    private static final Instant NOW = Instant.now();

    private RSAKey signingKey;
    private GoogleOAuthIdentityVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("google-key-1").generate();
        verifier = verifierWith(new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK())));
    }

    @Test
    @DisplayName("유효한 Google token은 프로필 claim을 포함한 신원으로 변환한다")
    void verifiesValidGoogleIdToken() throws Exception {
        OAuthIdentity identity = verifier.verify(token(builder()), null);

        assertThat(identity.socialId()).isEqualTo("google-sub-123");
        assertThat(identity.email()).isEqualTo("learner@example.com");
        assertThat(identity.name()).isEqualTo("Learner");
        assertThat(identity.profileImageUrl()).isEqualTo("https://example.com/p.png");
    }

    @Test
    @DisplayName("scheme 없는 Google issuer도 허용한다")
    void acceptsIssuerWithoutScheme() throws Exception {
        OAuthIdentity identity = verifier.verify(token(builder().issuer("accounts.google.com")), null);

        assertThat(identity.socialId()).isEqualTo("google-sub-123");
    }

    @Test
    @DisplayName("다른 OAuth client audience로 발급된 token을 거부한다")
    void rejectsWrongAudience() {
        assertThatThrownBy(() -> verifier.verify(token(builder().audience("other-client")), null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Google 이외 issuer를 거부한다")
    void rejectsWrongIssuer() {
        assertThatThrownBy(() -> verifier.verify(token(builder().issuer("https://example.com")), null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("만료된 token을 거부한다")
    void rejectsExpiredToken() {
        assertThatThrownBy(() -> verifier.verify(
                token(builder().expirationTime(Date.from(NOW.minusSeconds(1)))),
                null
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("email_verified=false token을 거부한다")
    void rejectsUnverifiedEmail() {
        assertThatThrownBy(() -> verifier.verify(token(builder().claim("email_verified", false)), null))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("공개 key와 다른 key로 서명된 token을 거부한다")
    void rejectsForgedSignature() throws Exception {
        RSAKey original = signingKey;
        signingKey = new RSAKeyGenerator(2048).keyID("google-key-1").generate();
        String forged = token(builder());
        signingKey = original;

        assertThatThrownBy(() -> verifier.verify(forged, null)).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("공개 JWK를 가져오지 못하면 401이 아닌 공급자 일시 장애로 분류한다")
    void classifiesKeySourceOutageAsProviderUnavailable() throws Exception {
        JWKSource<SecurityContext> unavailable = (selector, context) -> {
            throw new KeySourceException("connect timed out");
        };
        GoogleOAuthIdentityVerifier outageVerifier = verifierWith(unavailable);

        assertThatThrownBy(() -> outageVerifier.verify(token(builder()), null))
                .isInstanceOf(OAuthProviderUnavailableException.class);
    }

    private GoogleOAuthIdentityVerifier verifierWith(JWKSource<SecurityContext> keySource) {
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource));
        GoogleOAuthSettings settings = new GoogleOAuthSettings();
        settings.setClientId(CLIENT_ID);
        return new GoogleOAuthIdentityVerifier(settings, processor, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private JWTClaimsSet.Builder builder() {
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("google-sub-123")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .claim("email", "learner@example.com")
                .claim("email_verified", true)
                .claim("name", "Learner")
                .claim("picture", "https://example.com/p.png");
    }

    private String token(JWTClaimsSet.Builder claimsBuilder) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claimsBuilder.build()
        );
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }
}
