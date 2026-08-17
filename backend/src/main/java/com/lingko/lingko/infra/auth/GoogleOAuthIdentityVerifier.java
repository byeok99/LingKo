package com.lingko.lingko.infra.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lingko.lingko.core.config.GoogleOAuthSettings;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentity;
import com.lingko.lingko.core.domain.auth.service.OAuthIdentityVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Google 신원 검증을 LingKo의 공급자 중립 인증 경계에 연결한다.
 *
 * 도메인이 검증된 신원 정보에만 의존하도록 Google 전용 세부사항을 infrastructure에 격리했다.
 */
@Component
@RequiredArgsConstructor
public class GoogleOAuthIdentityVerifier implements OAuthIdentityVerifier {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final GoogleOAuthSettings googleOAuthSettings;
    private final WebClient.Builder webClientBuilder;

    @Override
    public String provider() {
        return "GOOGLE";
    }

    @Override
    public OAuthIdentity verify(String idToken, String rawNonce) {
        validateSettings();

        try {
            // tokeninfo를 검증 경계로 사용하며 정규화된 신원 정보만 어댑터 밖으로 전달한다.
            GoogleTokenInfoResponse response = webClientBuilder.build()
                    .get()
                    .uri(GOOGLE_TOKENINFO_URL, builder -> builder.queryParam("id_token", idToken).build())
                    .retrieve()
                    .bodyToMono(GoogleTokenInfoResponse.class)
                    .block();

            return toIdentity(response);
        } catch (WebClientResponseException exception) {
            throw new AuthException("Invalid Google id token");
        }
    }

    private OAuthIdentity toIdentity(GoogleTokenInfoResponse response) {
        // 다른 Google OAuth client용 토큰이 인증에 사용되지 않도록 audience를 결합한다.
        if (response == null
                || isBlank(response.sub())
                || !googleOAuthSettings.getClientId().equals(response.aud())
                || Boolean.FALSE.equals(response.emailVerified())) {
            throw new AuthException("Invalid Google id token");
        }

        return new OAuthIdentity(
                response.sub(),
                response.email(),
                response.name(),
                response.picture()
        );
    }

    private void validateSettings() {
        if (isBlank(googleOAuthSettings.getClientId())) {
            throw new IllegalStateException("Google client id must be configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record GoogleTokenInfoResponse(
            String sub,
            String aud,
            String email,
            String name,
            String picture,
            @JsonProperty("email_verified")
            Boolean emailVerified
    ) {
    }
}
