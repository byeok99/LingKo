package com.lingko.lingko.api.common;

import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobConflictException;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobNotFoundException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobAccessDeniedException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobCapacityExceededException;
import com.lingko.lingko.core.domain.evaluation.exception.GuideJobRateLimitExceededException;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.auth.exception.ReviewAccessRateLimitExceededException;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.quota.exception.AdMobSsvVerificationException;
import com.lingko.lingko.core.domain.quota.exception.AdRewardSessionNotFoundException;
import com.lingko.lingko.core.domain.quota.exception.AdRewardUnavailableException;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.user.service.AccountDeletionUnavailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * validation·도메인·infrastructure 예외를 안전한 HTTP 응답으로 변환한다.
 *
 * 컨트롤러의 책임을 줄이고 내부 예외 정보 노출을 막기 위해 예외 매핑을 한곳에 집중했다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String APPLICATION_PACKAGE = "com.lingko.lingko.";

    /** 기존 앱과 직접 호출도 동의 누락을 인증 실패와 구분해 처리하게 한다. */
    @ExceptionHandler(com.lingko.lingko.core.domain.legal.service.AiConsentRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAiConsentRequired(Exception exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("AI_CONSENT_REQUIRED", "Allow AI assessment before sending your recording"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toDetail)
                .toList();

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_FAILED", "Validation failed", details));
    }

    /**
     * framework가 만든 요청 형식 오류는 고정 메시지로 응답한다.
     *
     * <p>Jackson·Bean Validation·Spring MVC 예외 메시지에는 내부 타입명, controller method 이름,
     * 파라미터 타입이 들어가므로 원문을 클라이언트에 그대로 직렬화하지 않는다.</p>
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", "Request is invalid"));
    }

    /**
     * 업무 검증 메시지는 유지하되 라이브러리가 던진 IllegalArgumentException 원문은 숨긴다.
     *
     * <p>LingKo 코드가 던진 예외는 "Only WAV audio up to 10 MiB can be uploaded"처럼 사용자에게 보여 줄
     * 의도로 작성됐다. 첫 stack frame이 LingKo package가 아니면(JDK enum 변환, URI parsing 등) 내부 구현
     * 정보가 섞일 수 있어 고정 메시지로 대체한다.</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        String message = isApplicationMessage(exception) ? exception.getMessage() : "Request is invalid";
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", message));
    }

    /** 존재하지 않는 경로는 스캐너 트래픽이 대부분이므로 ERROR 로그 없이 404로 응답한다. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Resource not found"));
    }

    /** 허용되지 않은 HTTP method는 Allow Header와 함께 405로 응답한다. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (exception.getSupportedHttpMethods() != null) {
            builder.allow(exception.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new));
        }
        return builder.body(ErrorResponse.of("METHOD_NOT_ALLOWED", "Method not allowed"));
    }

    /** JSON endpoint에 다른 Content-Type을 보낸 요청은 서버 오류가 아니라 415다. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "Unsupported media type"));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException exception) {
        // 응답 차이로 인증 정보가 추론되지 않도록 인증 세부사항을 의도적으로 고정 메시지로 대체한다.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AUTHENTICATION_FAILED", "Authentication failed"));
    }

    /** 심사용 인증 공격을 늦추면서 구체적인 검증 실패 정보 없이 재시도 시각만 전달한다. */
    @ExceptionHandler(ReviewAccessRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleReviewAccessRateLimit(
            ReviewAccessRateLimitExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(ErrorResponse.of("REVIEW_ACCESS_RATE_LIMITED", "Too many review access attempts"));
    }

    @ExceptionHandler(GuideJobAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleGuideJobAccessDenied(GuideJobAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("GUIDE_JOB_FORBIDDEN", "Guide generation is not available to this user"));
    }

    @ExceptionHandler(GuideJobRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleGuideJobRateLimit(GuideJobRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(ErrorResponse.of("GUIDE_JOB_RATE_LIMITED", "Too many guide generation requests"));
    }

    @ExceptionHandler(GuideJobCapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleGuideJobCapacity(GuideJobCapacityExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(ErrorResponse.of("GUIDE_JOB_CAPACITY_EXCEEDED", "Guide generation is busy"));
    }

    @ExceptionHandler(SentenceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSentenceNotFound(SentenceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("SENTENCE_NOT_FOUND", "Sentence not found"));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of("QUOTA_EXCEEDED", "Practice energy exhausted"));
    }

    @ExceptionHandler(AdMobSsvVerificationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAdMobCallback(
            AdMobSsvVerificationException exception
    ) {
        // 위조 판단 근거와 cryptographic 예외는 외부에 노출하지 않는다.
        log.warn("Rejected AdMob SSV callback: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_AD_REWARD_CALLBACK", "Invalid ad reward callback"));
    }

    @ExceptionHandler(AdRewardSessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAdRewardSessionNotFound(
            AdRewardSessionNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("AD_REWARD_SESSION_NOT_FOUND", "Ad reward session not found"));
    }

    @ExceptionHandler(AdRewardUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAdRewardUnavailable(
            AdRewardUnavailableException exception
    ) {
        log.warn("Ad reward is temporarily unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("AD_REWARD_UNAVAILABLE", "Ad reward is temporarily unavailable"));
    }

    @ExceptionHandler(EvaluationJobConflictException.class)
    public ResponseEntity<ErrorResponse> handleEvaluationJobConflict(
            EvaluationJobConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "IDEMPOTENCY_CONFLICT",
                        "Idempotency key was already used for a different request"
                ));
    }

    @ExceptionHandler(EvaluationJobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEvaluationJobNotFound(
            EvaluationJobNotFoundException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("EVALUATION_JOB_NOT_FOUND", "Evaluation job not found"));
    }

    @ExceptionHandler(VideoGenerationException.class)
    public ResponseEntity<ErrorResponse> handleVideoGeneration(VideoGenerationException exception) {
        // 외부 URL query에는 credential이 포함될 수 있어 예외 원문·stack trace 대신 분류만 남긴다.
        String causeType = exception.getCause() == null
                ? "none"
                : exception.getCause().getClass().getSimpleName();
        log.warn(
                "Pronunciation evaluation failed: errorType={}, causeType={}",
                exception.getClass().getSimpleName(),
                causeType
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("EVALUATION_FAILED", "Pronunciation evaluation failed. Please try again."));
    }

    @ExceptionHandler(AccountDeletionUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAccountDeletionUnavailable(
            AccountDeletionUnavailableException exception
    ) {
        log.warn("Account deletion deferred because remote cleanup failed", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(
                        "ACCOUNT_DELETION_UNAVAILABLE",
                        "Account deletion is temporarily unavailable. Please try again."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        // 상세 stack trace는 운영 로그에만 남기고 임의 예외 메시지는 응답으로 직렬화하지 않는다.
        log.error("Unexpected server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "Unexpected server error"));
    }

    private boolean isApplicationMessage(IllegalArgumentException exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        return exception.getMessage() != null
                && stackTrace.length > 0
                && stackTrace[0].getClassName().startsWith(APPLICATION_PACKAGE);
    }

    private ErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
        return new ErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
