package com.lingko.lingko.api.evaluation.dto;

import com.lingko.lingko.core.domain.evaluation.dto.VideoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * HTTP 경계에서 사용하는 Guide Generation Job 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
@Getter
@NoArgsConstructor
public class GuideGenerationJobRequest {
    @NotBlank
    @Size(max = 10)
    private String syllable;

    @NotNull
    private VideoType type;

    @NotEmpty
    @Size(max = 10)
    private List<@NotEmpty @Size(min = 1, max = 2) List<@NotBlank String>> urlPairs;

    public String trimmedSyllable() {
        return syllable.trim();
    }
}
