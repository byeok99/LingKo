package com.lingko.lingko.api.common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HTTP 요청 ID의 생성·검증·응답·MDC 정리 계약을 독립적으로 검증한다. */
class RequestIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("요청 ID가 없으면 서버 ID를 생성해 응답과 완료 로그에 같은 값으로 남긴다")
    void generatesRequestIdForResponseAndLog() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(() -> "generated-request-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        ListAppender<ILoggingEvent> appender = attach(logger);

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) ->
                    assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo("generated-request-id"));

            assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("generated-request-id");
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getMDCPropertyMap())
                        .containsEntry(RequestIdContext.MDC_KEY, "generated-request-id");
                assertThat(event.getFormattedMessage())
                        .contains("method=GET", "status=200", "durationMs=");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("기본 생성기는 충돌 가능성이 낮은 UUID v4를 사용한다")
    void defaultGeneratorUsesRandomUuid() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                new MockHttpServletRequest("GET", "/health"),
                response,
                (request, servletResponse) -> { }
        );

        UUID requestId = UUID.fromString(response.getHeader(RequestIdFilter.HEADER_NAME));
        assertThat(requestId.version()).isEqualTo(4);
    }

    @Test
    @DisplayName("안전한 외부 요청 ID는 그대로 사용한다")
    void reusesValidatedExternalRequestId() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(() -> "generated-request-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.addHeader(RequestIdFilter.HEADER_NAME, "client_01.trace-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo("client_01.trace-2"));

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("client_01.trace-2");
    }

    @Test
    @DisplayName("제어하기 어려운 문자나 최대 길이를 넘긴 외부 ID는 서버 ID로 교체한다")
    void replacesUnsafeExternalRequestIds() throws Exception {
        Iterator<String> generatedIds = java.util.List.of("safe-one", "safe-two", "safe-three").iterator();
        RequestIdFilter filter = new RequestIdFilter(generatedIds::next);

        assertGeneratedId(filter, "unsafe/value", "safe-one");
        assertGeneratedId(filter, "a".repeat(65), "safe-two");

        MockHttpServletRequest duplicateHeaderRequest = new MockHttpServletRequest("GET", "/health");
        duplicateHeaderRequest.addHeader(RequestIdFilter.HEADER_NAME, "first-id");
        duplicateHeaderRequest.addHeader(RequestIdFilter.HEADER_NAME, "second-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(duplicateHeaderRequest, response, (request, servletResponse) ->
                assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo("safe-three"));
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("safe-three");
    }

    @Test
    @DisplayName("연속 요청과 예외 경로 모두 이전 요청의 MDC 값을 남기지 않는다")
    void clearsRequestIdAfterSuccessAndFailure() throws Exception {
        Iterator<String> generatedIds = java.util.List.of("first-request", "second-request").iterator();
        RequestIdFilter filter = new RequestIdFilter(generatedIds::next);

        filter.doFilter(
                new MockHttpServletRequest("GET", "/first"),
                new MockHttpServletResponse(),
                (request, response) -> assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo("first-request")
        );
        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest("GET", "/second"),
                new MockHttpServletResponse(),
                (request, response) -> {
                    assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo("second-request");
                    throw new ServletException("test failure");
                }
        )).isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();
    }

    private void assertGeneratedId(RequestIdFilter filter, String suppliedId, String expectedId)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        request.addHeader(RequestIdFilter.HEADER_NAME, suppliedId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(RequestIdContext.MDC_KEY)).isEqualTo(expectedId));

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(expectedId);
    }

    private ListAppender<ILoggingEvent> attach(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
