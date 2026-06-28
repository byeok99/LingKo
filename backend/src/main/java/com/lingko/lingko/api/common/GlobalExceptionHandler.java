package com.lingko.lingko.api.common;

import com.lingko.lingko.core.domain.evaluation.exception.VideoGenerationException;
import com.lingko.lingko.core.domain.auth.exception.AuthException;
import com.lingko.lingko.core.domain.quota.exception.QuotaExceededException;
import com.lingko.lingko.core.domain.sentence.exception.SentenceNotFoundException;
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of("AUDIO_TOO_LARGE", "Audio file is too large"));
    }

    @ExceptionHandler(VideoGenerationException.class)
    public ResponseEntity<ErrorResponse> handleVideoGeneration(VideoGenerationException exception) {
        log.warn("Pronunciation evaluation failed", exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("EVALUATION_FAILED", "Pronunciation evaluation failed. Please try again."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "Unexpected server error"));
    }

    private ErrorResponse.FieldErrorDetail toDetail(FieldError fieldError) {
        return new ErrorResponse.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
