package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardPronunciationResponse {
    /**
     * 원본 문장
     * 예: "밥 먹었어요"
     */
    private String originalText;

    /**
     * 표준 발음
     * 예: "밥 머거써요"
     */
    private String standardPronunciation;
}
