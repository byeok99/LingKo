package com.lingko.lingko.api.sentence.dto;

import com.lingko.lingko.api.evaluation.dto.GuideCharacterResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSentenceResponse {
    private Long sentenceId;
    private String source;
    private String originalText;
    private String standardPronunciation;
    private String translation;
    private String categoryLabel;
    private String learningPoint;
    private int initialScore;
    private List<GuideCharacterResponse> characters;
}
