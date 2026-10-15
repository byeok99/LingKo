package com.lingko.lingko.api.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 각 HTTP 요청에 안전한 상관관계 ID를 부여해 응답, 오류 본문과 서버 로그를 연결한다.
 *
 * 외부 ID는 로그 주입을 막기 위해 제한된 문자와 길이를 통과할 때만 신뢰하며, 인증 정보나 사용자
 * 식별자는 MDC에 넣지 않는다. 가장 앞에서 실행해 Controller 이전 실패도 같은 ID로 추적한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]{0," + (MAX_REQUEST_ID_LENGTH - 1) + "}"
    );

    private final Supplier<String> requestIdGenerator;

    public RequestIdFilter() {
        this(() -> UUID.randomUUID().toString());
    }

    RequestIdFilter(Supplier<String> requestIdGenerator) {
        this.requestIdGenerator = Objects.requireNonNull(requestIdGenerator);
    }

    /**
     * 요청 처리 중에만 MDC 값을 유지하고 완료 로그를 기록한 뒤 반드시 제거한다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAtNanos = System.nanoTime();
        boolean completed = false;

        response.setHeader(HEADER_NAME, requestId);
        MDC.put(RequestIdContext.MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
            completed = true;
        } finally {
            long durationMillis = (System.nanoTime() - startedAtNanos) / 1_000_000;
            try {
                // URL·query·본문은 식별자나 민감정보를 포함할 수 있어 요청 로그에 기록하지 않는다.
                log.info(
                        "HTTP request finished: method={}, status={}, outcome={}, durationMs={}",
                        request.getMethod(),
                        response.getStatus(),
                        completed ? "completed" : "failed",
                        durationMillis
                );
            } finally {
                // Servlet thread가 재사용될 때 다음 요청으로 ID가 새지 않도록 복원 대신 제거한다.
                MDC.remove(RequestIdContext.MDC_KEY);
            }
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        Enumeration<String> headerValues = request.getHeaders(HEADER_NAME);
        if (headerValues != null && headerValues.hasMoreElements()) {
            String candidate = headerValues.nextElement();
            if (!headerValues.hasMoreElements() && SAFE_REQUEST_ID.matcher(candidate).matches()) {
                return candidate;
            }
        }

        return requestIdGenerator.get();
    }
}
