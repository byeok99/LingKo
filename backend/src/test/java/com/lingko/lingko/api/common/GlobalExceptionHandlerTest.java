package com.lingko.lingko.api.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.lingko.lingko.api.evaluation.EvaluationController;
import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.service.EvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Global 예외 Handler Test의 성공·실패 경로와 회귀 계약을 검증한다.
 *
 * 보장하려는 동작을 테스트 경계에 명시해 구현 변경이 계약을 깨뜨리면 자동 검증에서 드러나게 한다.
 */
@WebMvcTest(EvaluationController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationService evaluationService;

    @Test
    @DisplayName("정상 요청은 응답 Header와 INFO 완료 로그에 같은 requestId를 사용한다")
    void successfulRequestAndLogShareRequestId() throws Exception {
        String requestId = "successful-request-01";
        when(evaluationService.convertToStandardPronunciation(anyString())).thenReturn("마시께따");
        Logger logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            mockMvc.perform(post("/api/pronunciation/convert")
                            .header(RequestIdFilter.HEADER_NAME, requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\":\"맛있겠다\"}"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
                    .andExpect(jsonPath("$.standardPronunciation").value("마시께따"));

            org.assertj.core.api.Assertions.assertThat(appender.list)
                    .anySatisfy(event -> {
                        org.assertj.core.api.Assertions.assertThat(event.getLevel()).isEqualTo(Level.INFO);
                        org.assertj.core.api.Assertions.assertThat(event.getMDCPropertyMap())
                                .containsEntry(RequestIdContext.MDC_KEY, requestId);
                        org.assertj.core.api.Assertions.assertThat(event.getFormattedMessage())
                                .contains("method=POST", "status=200", "outcome=completed");
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("validation 실패는 응답 Header와 ErrorResponse에 같은 requestId를 반환한다")
    void validationFailureReturnsErrorResponse() throws Exception {
        String requestId = "validation-request-01";

        mockMvc.perform(post("/api/pronunciation/convert")
                        .header(RequestIdFilter.HEADER_NAME, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("text"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    @DisplayName("외부 연동 실패는 내부 URL을 노출하지 않는 고정 메시지를 반환한다")
    void videoGenerationFailureDoesNotExposeInternalDetails() throws Exception {
        when(evaluationService.convertToStandardPronunciation(anyString()))
                .thenThrow(new VideoGenerationException(
                        "외부 미디어 URL 연결 실패: https://replicate.delivery/private/result.mp4"
                ));

        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"맛있겠다\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EVALUATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Pronunciation evaluation failed. Please try again."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("replicate.delivery"))));
    }

    @Test
    @DisplayName("외부 연동 실패 로그에도 credential을 포함할 수 있는 URL 원문을 남기지 않는다")
    void videoGenerationFailureDoesNotExposeSourceUrlInLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            var failure = new VideoGenerationException(
                    "외부 미디어 URL 연결 실패: https://example.invalid/result.mp4?token=sensitive"
            );

            new GlobalExceptionHandler().handleVideoGeneration(failure);

            org.assertj.core.api.Assertions.assertThat(appender.list)
                    .hasSize(1)
                    .allSatisfy(event -> {
                        org.assertj.core.api.Assertions.assertThat(event.getFormattedMessage())
                                .doesNotContain("example.invalid", "token=sensitive");
                        org.assertj.core.api.Assertions.assertThat(event.getThrowableProxy()).isNull();
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("예상하지 못한 500 예외는 상세 응답 없이 ERROR stack trace를 서버 로그에 남긴다")
    void unexpectedFailureIsLoggedWithoutExposingDetails() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            RuntimeException failure = new RuntimeException("database unavailable");

            var response = new GlobalExceptionHandler().handleUnexpected(failure);

            org.assertj.core.api.Assertions.assertThat(response.getStatusCode().value()).isEqualTo(500);
            org.assertj.core.api.Assertions.assertThat(response.getBody().message())
                    .isEqualTo("Unexpected server error")
                    .doesNotContain("database unavailable");
            org.assertj.core.api.Assertions.assertThat(appender.list)
                    .anySatisfy(event -> {
                        org.assertj.core.api.Assertions.assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                        org.assertj.core.api.Assertions.assertThat(event.getFormattedMessage())
                                .isEqualTo("Unexpected server error");
                        org.assertj.core.api.Assertions.assertThat(event.getThrowableProxy()).isNotNull();
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("존재하지 않는 경로는 ERROR 로그 없이 404 NOT_FOUND를 반환한다")
    void unknownPathReturnsNotFoundWithoutErrorLog() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            mockMvc.perform(get("/wp-login.php"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));

            org.assertj.core.api.Assertions.assertThat(appender.list)
                    .noneSatisfy(event -> org.assertj.core.api.Assertions.assertThat(event.getLevel())
                            .isEqualTo(Level.ERROR));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    @DisplayName("지원하지 않는 HTTP method는 405와 Allow Header를 반환한다")
    void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/api/pronunciation/convert"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("JSON이 아닌 Content-Type은 415를 반환한다")
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("맛있겠다"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("malformed JSON은 Jackson 내부 메시지 대신 고정 메시지를 반환한다")
    void malformedJsonHidesParserDetails() throws Exception {
        mockMvc.perform(post("/api/pronunciation/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
    }

    @Test
    @DisplayName("LingKo 코드의 IllegalArgumentException 메시지는 유지하고 라이브러리 원문은 숨긴다")
    void illegalArgumentMessageIsExposedOnlyForApplicationCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException applicationFailure =
                new IllegalArgumentException("Only WAV audio up to 10 MiB can be uploaded");
        IllegalArgumentException libraryFailure = org.assertj.core.api.Assertions.catchThrowableOfType(
                IllegalArgumentException.class,
                () -> java.util.UUID.fromString("not-a-uuid")
        );

        org.assertj.core.api.Assertions.assertThat(handler.handleIllegalArgument(applicationFailure).getBody().message())
                .isEqualTo("Only WAV audio up to 10 MiB can be uploaded");
        org.assertj.core.api.Assertions.assertThat(handler.handleIllegalArgument(libraryFailure).getBody().message())
                .isEqualTo("Request is invalid");
    }

    @Test
    @DisplayName("예상하지 못한 500 응답과 ERROR 로그는 같은 requestId로 연결된다")
    void unexpectedFailureResponseAndLogShareRequestId() throws Exception {
        String requestId = "unexpected-request-01";
        when(evaluationService.convertToStandardPronunciation(anyString()))
                .thenThrow(new RuntimeException("database unavailable"));
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            mockMvc.perform(post("/api/pronunciation/convert")
                            .header(RequestIdFilter.HEADER_NAME, requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\":\"맛있겠다\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
                    .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                    .andExpect(jsonPath("$.message").value("Unexpected server error"))
                    .andExpect(jsonPath("$.requestId").value(requestId));

            org.assertj.core.api.Assertions.assertThat(appender.list)
                    .anySatisfy(event -> {
                        org.assertj.core.api.Assertions.assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                        org.assertj.core.api.Assertions.assertThat(event.getMDCPropertyMap())
                                .containsEntry(RequestIdContext.MDC_KEY, requestId);
                        org.assertj.core.api.Assertions.assertThat(event.getThrowableProxy()).isNotNull();
                    });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

}
