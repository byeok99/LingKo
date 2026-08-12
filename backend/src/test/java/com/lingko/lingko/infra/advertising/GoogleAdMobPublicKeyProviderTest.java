package com.lingko.lingko.infra.advertising;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** AdMob rotating key cache가 정상 키와 unknown key 요청을 안전하게 처리하는지 검증한다. */
class GoogleAdMobPublicKeyProviderTest {

    @Test
    @DisplayName("Spring은 운영용 생성자를 선택해 AdMob 공개키 provider bean을 생성한다")
    void createsProviderThroughSpringConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(WebClient.Builder.class, WebClient::builder);
            context.register(GoogleAdMobPublicKeyProvider.class);

            context.refresh();

            assertThat(context.getBean(GoogleAdMobPublicKeyProvider.class)).isNotNull();
        }
    }

    @Test
    @DisplayName("fresh cache의 unknown key_id는 Google key server를 반복 호출하지 않는다")
    void cachesUnknownKeyMissesUntilRefresh() throws Exception {
        byte[] encodedKey = KeyPairGenerator.getInstance("EC").generateKeyPair()
                .getPublic()
                .getEncoded();
        String body = "{\"keys\":[{\"keyId\":1234,\"base64\":\""
                + Base64.getEncoder().encodeToString(encodedKey)
                + "\"}]}";
        AtomicInteger calls = new AtomicInteger();
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    calls.incrementAndGet();
                    return reactor.core.publisher.Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(body)
                            .build());
                })
                .build();
        GoogleAdMobPublicKeyProvider provider = new GoogleAdMobPublicKeyProvider(
                client,
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(provider.getKey(1234)).isNotNull();
        assertThat(provider.getKey(9999)).isNull();
        assertThat(provider.getKey(9999)).isNull();
        assertThat(calls).hasValue(1);
    }
}
