package com.lingko.lingko.core.domain.evaluation.exception;

/**
 * 영상 생성 실패 예외
 *
 * 발생 케이스:
 * - Replicate API 호출 실패
 * - FFmpeg 병합 실패
 * - S3 업로드 실패
 * - 타임아웃
 * - 잘못된 입력 (URL 형식 오류 등)
 */
public class VideoGenerationException extends RuntimeException {

    public VideoGenerationException(String message) {
        super(message);
    }

    public VideoGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}