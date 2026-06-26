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

@Component
@RequiredArgsConstructor
public class GoogleOAuthIdentityVerifier implements OAuthIdentityVerifier {

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final GoogleOAuthSettings googleOAuthSettings;
    private final WebClient.Builder webClientBuilder;

    @Override
    public OAuthIdentity verify(String idToken) {
        validateSettings();

        try {
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
