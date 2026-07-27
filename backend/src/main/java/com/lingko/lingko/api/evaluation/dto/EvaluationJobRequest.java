package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이미 업로드된 사용자 소유 음성과 평가 대상을 비동기 작업으로 등록한다.
 */
public record EvaluationJobRequest(
        @NotBlank @Size(max = 500) String objectKey,
        Long sentenceId,
        @Size(max = 300) String text
) {
}
