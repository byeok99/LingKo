package com.lingko.e2e.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
     * 고정 사용자의 남은 평가 기회를 {@code remaining}(0~5)회로 맞추고, 서버 응답 JSON 원문을 돌려준다.
     * 응답은 앱의 {@code GET /api/quota/today}와 같은 형식이라 화면 값과 비교할 기준으로 쓴다.
     */
    public static String setRemainingPractices(int remaining) {
        return send(HttpRequest.newBuilder(API_URL.resolve("/test-support/quota?remaining=" + remaining))
                .PUT(HttpRequest.BodyPublishers.noBody()));
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
