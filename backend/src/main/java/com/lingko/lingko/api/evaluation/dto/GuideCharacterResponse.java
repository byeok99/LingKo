package com.lingko.lingko.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideCharacterResponse {
    private int position;
    private String text;
    private String pronunciationText;
    private Integer score;
    private String scoreStatus;
    private List<String> phonemes;
    private String guideType;
    private String guideStatus;
    private String mouthGuideUrl;
    private String tongueGuideUrl;
    private String note;
}
