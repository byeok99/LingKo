package com.lingko.lingko.api.common;

import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobConflictException;
import com.lingko.lingko.core.domain.evaluation.exception.EvaluationJobNotFoundException;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
import com.lingko.lingko.core.domain.user.service.AccountDeletionUnavailableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * validation·도메인·infrastructure 예외를 안전한 HTTP 응답으로 변환한다.
 *
 * 컨트롤러의 책임을 줄이고 내부 예외 정보 노출을 막기 위해 예외 매핑을 한곳에 집중했다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException exception) {
        // 응답 차이로 인증 정보가 추론되지 않도록 인증 세부사항을 의도적으로 고정 메시지로 대체한다.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AUTHENTICATION_FAILED", "Authentication failed"));
    }

    @ExceptionHandler(SentenceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSentenceNotFound(SentenceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("SENTENCE_NOT_FOUND", "Sentence not found"));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of("QUOTA_EXCEEDED", "Daily practice quota exceeded"));
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of("AUDIO_TOO_LARGE", "Audio file is too large"));
    }

    @ExceptionHandler(VideoGenerationException.class)
    public ResponseEntity<ErrorResponse> handleVideoGeneration(VideoGenerationException exception) {
        // 공급자 세부사항은 서버에만 남기고 클라이언트에는 재시도 가능한 중립 오류를 반환한다.
        log.warn("Pronunciation evaluation failed", exception);
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
        // 예측하지 못한 실패 경로의 임의 예외 메시지를 응답으로 직렬화하지 않는다.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "Unexpected server error"));
    }

    private ErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
        return new ErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
