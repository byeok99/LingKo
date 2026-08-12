package com.lingko.lingko.infra.advertising;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lingko.lingko.core.domain.quota.exception.AdRewardUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Google AdMob key server의 rotating public key를 최대 24시간 cache한다. */
@Component
public class GoogleAdMobPublicKeyProvider implements AdMobPublicKeyProvider {

    static final String KEY_SERVER_URL = "https://www.gstatic.com/admob/reward/verifier-keys.json";
    private static final Duration CACHE_TTL = Duration.ofHours(23);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final Clock clock;
    private volatile CachedKeys cachedKeys;

    /** 테스트용 생성자와 구분해 Spring이 운영 의존성 생성자를 선택하도록 명시한다. */
    @Autowired
    public GoogleAdMobPublicKeyProvider(
            WebClient.Builder webClientBuilder,
            ObjectProvider<Clock> clockProvider
    ) {
        this(webClientBuilder.build(), clockProvider.getIfAvailable(Clock::systemUTC));
    }

    GoogleAdMobPublicKeyProvider(WebClient webClient, Clock clock) {
        this.webClient = webClient;
        this.clock = clock;
    }

    @Override
    public PublicKey getKey(long keyId) {
        CachedKeys current = cachedKeys;
        if (current != null && current.isFreshAt(clock.instant())) {
            // 공격자가 임의 key_id로 key server fetch를 증폭하지 못하게 fresh cache의 miss도 cache한다.
            return current.keys().get(keyId);
        }
        return refreshAndGet(keyId);
    }

    private synchronized PublicKey refreshAndGet(long keyId) {
        CachedKeys current = cachedKeys;
        if (current != null && current.isFreshAt(clock.instant()) && current.keys().containsKey(keyId)) {
            return current.keys().get(keyId);
        }

        try {
            KeySetResponse response = webClient.get()
                    .uri(KEY_SERVER_URL)
                    .retrieve()
                    .bodyToMono(KeySetResponse.class)
                    .block(REQUEST_TIMEOUT);
            if (response == null || response.keys() == null || response.keys().isEmpty()) {
                throw new AdRewardUnavailableException("AdMob public keys are unavailable");
            }
            Map<Long, PublicKey> parsed = response.keys().stream()
                    .collect(Collectors.toUnmodifiableMap(KeyResponse::keyId, this::parseKey));
            cachedKeys = new CachedKeys(parsed, clock.instant().plus(CACHE_TTL));
            PublicKey key = parsed.get(keyId);
            if (key == null) {
                throw new AdRewardUnavailableException("AdMob public key is unavailable");
            }
            return key;
        } catch (AdRewardUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AdRewardUnavailableException("Unable to refresh AdMob public keys", exception);
        }
    }

    private PublicKey parseKey(KeyResponse response) {
        try {
            byte[] encoded = Base64.getDecoder().decode(response.base64());
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception exception) {
            throw new AdRewardUnavailableException("Invalid AdMob public key", exception);
        }
    }

    private record CachedKeys(Map<Long, PublicKey> keys, Instant expiresAt) {
        boolean isFreshAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    private record KeySetResponse(List<KeyResponse> keys) {
    }

    private record KeyResponse(@JsonProperty("keyId") long keyId, String base64) {
    }
}
