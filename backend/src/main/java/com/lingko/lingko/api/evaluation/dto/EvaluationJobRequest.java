package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이미 업로드된 사용자 소유 음성과 평가 대상을 비동기 작업으로 등록한다.
 *
 * {@code text} 상한은 자유 문장 준비 endpoint와 동일한 100자다. 평가 요청은 준비를 통과한
 * 문장만 올라오므로 두 경계의 상한이 다르면 준비에서 막은 길이가 평가로는 들어오는 검증 구멍이 된다.
 */
public record EvaluationJobRequest(
        @NotBlank @Size(max = 500) String objectKey,
        Long sentenceId,
        @Size(max = 100) String text
) {
}
