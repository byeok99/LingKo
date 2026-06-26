package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeHistoryCharacterResponse {
    private int position;
    private String text;
    private Integer score;
    private String feedback;
    private String mouthGuideUrl;
    private String tongueGuideUrl;
}
