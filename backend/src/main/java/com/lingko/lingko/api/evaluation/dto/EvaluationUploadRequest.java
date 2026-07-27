package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 앱이 S3에 직접 업로드할 WAV 파일의 서명 조건을 전달한다.
 */
public record EvaluationUploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank String contentType,
        @Min(44) @Max(10L * 1024 * 1024) long contentLength
) {
}
