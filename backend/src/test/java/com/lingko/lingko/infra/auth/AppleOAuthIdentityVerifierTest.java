package com.lingko.lingko.infra.auth;

import com.lingko.lingko.core.config.AppleOAuthSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Apple identity token의 서명과 보안 claim 검증 계약을 고정한다.
 */
class AppleOAuthIdentityVerifierTest {

    private static final String CLIENT_ID = "com.byeok.lingko";
    private static final String RAW_NONCE = "raw-nonce-012345678901234567890123";
    private static final Instant NOW = Instant.now();

    private RSAKey signingKey;
    private AppleOAuthIdentityVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("apple-key-1").generate();
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256,
                new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()))
        ));
        AppleOAuthSettings settings = new AppleOAuthSettings();
        settings.setClientId(CLIENT_ID);
        verifier = new AppleOAuthIdentityVerifier(
                settings,
                processor,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("유효한 Apple token은 nonce와 검증된 이메일을 포함한 신원으로 변환한다")
    void verifiesValidAppleIdentityToken() throws Exception {
        OAuthIdentity identity = verifier.verify(token(builder()), RAW_NONCE);

        assertThat(identity.socialId()).isEqualTo("apple-sub-123");
        assertThat(identity.email()).isEqualTo("relay@privaterelay.appleid.com");
        assertThat(identity.name()).isNull();
        assertThat(identity.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("다른 요청의 raw nonce로 Apple token을 재사용할 수 없다")
    void rejectsNonceMismatch() throws Exception {
        assertThatThrownBy(() -> verifier.verify(
                token(builder()),
                "different-nonce-012345678901234567890"
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("다른 앱 audience로 발급된 Apple token을 거부한다")
    void rejectsWrongAudience() throws Exception {
        assertThatThrownBy(() -> verifier.verify(
                token(builder().audience("com.example.other")),
                RAW_NONCE
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 이외 issuer로 발급된 token을 거부한다")
    void rejectsWrongIssuer() throws Exception {
        assertThatThrownBy(() -> verifier.verify(
                token(builder().issuer("https://example.com")),
                RAW_NONCE
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 공개 key와 다른 key로 서명된 token을 거부한다")
    void rejectsUnknownSigningKey() throws Exception {
        RSAKey originalKey = signingKey;
        signingKey = new RSAKeyGenerator(2048).keyID("attacker-key").generate();
        String forgedToken = token(builder());
        signingKey = originalKey;

        assertThatThrownBy(() -> verifier.verify(forgedToken, RAW_NONCE))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("만료된 Apple token을 거부한다")
    void rejectsExpiredToken() throws Exception {
        assertThatThrownBy(() -> verifier.verify(
                token(builder().expirationTime(Date.from(NOW.minusSeconds(1)))),
                RAW_NONCE
        )).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("검증되지 않은 Apple 이메일 claim을 계정에 저장하지 않고 로그인을 거부한다")
    void rejectsUnverifiedEmail() throws Exception {
        assertThatThrownBy(() -> verifier.verify(
                token(builder().claim("email_verified", false)),
                RAW_NONCE
        )).isInstanceOf(AuthException.class);
    }

    private JWTClaimsSet.Builder builder() throws Exception {
        return new JWTClaimsSet.Builder()
                .issuer("https://appleid.apple.com")
                .audience(CLIENT_ID)
                .subject("apple-sub-123")
                .issueTime(Date.from(NOW.minusSeconds(10)))
                .expirationTime(Date.from(NOW.plusSeconds(300)))
                .claim("nonce", sha256(RAW_NONCE))
                .claim("email", "relay@privaterelay.appleid.com")
                .claim("email_verified", true);
    }

    private String token(JWTClaimsSet.Builder claimsBuilder) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claimsBuilder.build()
        );
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
