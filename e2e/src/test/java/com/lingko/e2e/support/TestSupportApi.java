package com.lingko.e2e.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 앱을 띄우기 전에 backend의 {@code e2e} 프로필 전용 test-support API로 서버 상태를 맞춘다.
 *
 * <p>화면 조작으로 사전조건(평가 기회 3회 등)을 만들면 느리고 이전 실행 결과에 흔들리므로 서버에서 직접 맞춘다.
 * 실패하면 상태 코드와 본문을 담아 바로 예외를 던진다. 사전조건 실패를 삼키면 뒤의 화면 assert가
 * 엉뚱한 이유로 실패해 원인을 찾기 어렵기 때문이다.</p>
 */
public final class TestSupportApi {

    // 앱 E2E 빌드의 LINGKO_API_BASE_URL과 같은 backend를 가리켜야 한다
    private static final URI API_URL = URI.create(System.getProperty("api.url", "http://127.0.0.1:8080"));
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private TestSupportApi() {
    }

    /** 고정 테스트 사용자를 만든다(이미 있으면 재사용). 이 호출 없이 quota를 설정하면 처음 실행할 때 409다. */
    public static void ensureE2eUser() {
        send(HttpRequest.newBuilder(API_URL.resolve("/test-support/sessions"))
                .POST(HttpRequest.BodyPublishers.noBody()));
    }

    /**
     * 고정 사용자의 남은 평가 기회를 {@code remaining}(0~5)회로 맞추고 서버 기준값을 돌려준다.
     * 응답은 앱의 {@code GET /api/quota/today}와 같은 형식이라 화면 값과 비교할 기준으로 쓴다.
     */
    public static ServerQuota setRemainingPractices(int remaining) {
        String body = send(HttpRequest.newBuilder(API_URL.resolve("/test-support/quota?remaining=" + remaining))
                .PUT(HttpRequest.BodyPublishers.noBody()));
        return ServerQuota.parse(body);
    }

    /**
     * 화면 비교에 쓰는 서버 평가 기회 값.
     *
     * <p>{@code nextRefillAt}이 null이면 5회 보유라 충전 대기가 없다는 뜻이다. 남은 시간은 기기·호스트 시계 대신
     * 두 서버 시각의 차이로 계산한다. 앱도 같은 방식으로 카운트다운을 만들기 때문이다.</p>
     */
    public record ServerQuota(int remaining, int limit, OffsetDateTime nextRefillAt, OffsetDateTime serverTime) {

        // shortcut: 필드 4개만 정규식으로 읽는다. 응답 구조를 더 많이 다루게 되면 JSON 라이브러리로 바꾼다
        static ServerQuota parse(String json) {
            String nextRefillAt = optionalString(json, "nextRefillAt");
            return new ServerQuota(
                    Integer.parseInt(required(json, "\"remainingPractices\":(\\d+)")),
                    Integer.parseInt(required(json, "\"freeLimit\":(\\d+)")),
                    nextRefillAt == null ? null : OffsetDateTime.parse(nextRefillAt),
                    OffsetDateTime.parse(required(json, "\"serverTime\":\"([^\"]+)\"")));
        }

        /** 서버가 이 값을 돌려준 순간 기준의 다음 충전까지 남은 시간. 충전 대기가 없으면 null이다. */
        public Duration timeUntilRefill() {
            return nextRefillAt == null ? null : Duration.between(serverTime, nextRefillAt);
        }

        private static String required(String json, String regex) {
            Matcher matcher = Pattern.compile(regex).matcher(json);
            if (!matcher.find()) {
                throw new IllegalStateException("quota 응답에서 " + regex + "를 찾지 못했다: " + json);
            }
            return matcher.group(1);
        }

        private static String optionalString(String json, String field) {
            Matcher matcher = Pattern.compile("\"" + field + "\":\"([^\"]+)\"").matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        }
    }

    private static String send(HttpRequest.Builder builder) {
        HttpRequest request = builder.timeout(Duration.ofSeconds(10)).build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(request.method() + " " + request.uri()
                        + " -> " + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "backend에 연결하지 못했다. e2e 프로필로 실행 중인지 확인: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("backend 호출이 중단됐다: " + request.uri(), e);
        }
    }
}
