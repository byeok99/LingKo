package com.lingko.lingko.api.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HTTP 경계에서 사용하는 Standard Pronunciation 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StandardPronunciationRequest {
    @NotBlank(message = "문장을 입력해주세요.")
    @Size(min=1, max=30, message = "문장은 30자 이내로 입력해주세요.")
    private String text;
}
