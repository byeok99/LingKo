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
    /**
     * 변환할 문장이며 최대 100자다.
     *
     * <p>자유 문장 준비({@code /prepare})와 평가 작업 요청의 상한이 100자이므로 같은 값을 쓴다.
     * 경계마다 상한이 다르면 한쪽에서 통과한 문장이 다른 쪽에서 거부되는 계약 불일치가 생긴다.</p>
     */
    @NotBlank(message = "text is required")
    @Size(max = 100, message = "text must be 100 characters or fewer")
    private String text;
}
